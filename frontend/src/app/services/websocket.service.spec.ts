import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { WebSocketService, WebSocketStatus } from './websocket.service';
import { AppConfig } from '../app.config';
import { ClientMessageType } from '../model/ws/client-message-type.enum';
import { ServerMessageType } from '../model/ws/server-message-type.enum';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';

// ── Minimal WebSocket mock ────────────────────────────────────────────────────

class MockWebSocket {
  /* The service compares `readyState` against the platform constants, and this class replaces the
     global constructor, so the constants have to be mirrored here or every comparison is `undefined`. */
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSING = 2;
  static readonly CLOSED = 3;

  static instance: MockWebSocket | null = null;
  static instances: MockWebSocket[] = [];

  readyState: number = WebSocket.CONNECTING;
  sentMessages: string[] = [];
  closed = false;

  onopen:    ((e: Event)        => void) | null = null;
  onmessage: ((e: MessageEvent) => void) | null = null;
  onclose:   ((e: CloseEvent)   => void) | null = null;
  onerror:   ((e: Event)        => void) | null = null;

  constructor(public url: string) {
    MockWebSocket.instance = this;
    MockWebSocket.instances.push(this);
  }

  send(data: string) {
    if (this.closed) throw new Error('send() on a closed socket');
    this.sentMessages.push(data);
  }

  /* Mirrors the browser: `close()` always surfaces as an `onclose` event. */
  close() {
    this.readyState = WebSocket.CLOSED;
    this.closed = true;
    this.onclose?.(new CloseEvent('close'));
  }

  /** Test helper — simulate a successful connection. */
  simulateOpen() {
    this.readyState = WebSocket.OPEN;
    this.onopen?.(new Event('open'));
  }

  /** Test helper — simulate an incoming server message. */
  simulateMessage(msg: IWsServerMessage) {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(msg) }));
  }

  /** Test helper — simulate a raw inbound frame, e.g. malformed JSON. */
  simulateRawMessage(data: string) {
    this.onmessage?.(new MessageEvent('message', { data }));
  }

  /** Test helper — simulate the peer going away. */
  simulateClose() {
    this.readyState = WebSocket.CLOSED;
    this.onclose?.(new CloseEvent('close'));
  }

  sentTypes(): string[] {
    return this.sentMessages.map((s) => JSON.parse(s).type);
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const WS_URL = 'ws://localhost:8080/api/ws/run';
const PING_INTERVAL = 15_000;
const LIVENESS_TIMEOUT = 30_000;

function makeConfig(): Partial<AppConfig> {
  return { webSocketUrl: WS_URL } as Partial<AppConfig>;
}

function serverMsg(type: ServerMessageType.ServerMessageTypeEnum, evaluationId = 'eval-1'): IWsServerMessage {
  return { evaluationId, type, timestamp: Date.now() };
}

/** The service's own keep-alive reply, which the backend sends for every PING. */
function pingReply(evaluationId = 'eval-1'): IWsServerMessage {
  return serverMsg(ServerMessageType.ServerMessageTypeEnum.PING, evaluationId);
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    MockWebSocket.instance = null;
    MockWebSocket.instances = [];
    (window as any).WebSocket = MockWebSocket;

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: AppConfig, useValue: makeConfig() },
      ],
    });

    service = TestBed.inject(WebSocketService);
  });

  afterEach(() => {
    service.disconnect();
  });

  // ── connect() ──────────────────────────────────────────────────────────────

  describe('connect', () => {
    it('opens a WebSocket to the configured URL', () => {
      service.connect('eval-1');
      expect(MockWebSocket.instance).not.toBeNull();
      expect(MockWebSocket.instance!.url).toBe(WS_URL);
    });

    it('sends REGISTER message on open', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      const sent = JSON.parse(MockWebSocket.instance!.sentMessages[0]);
      expect(sent.type).toBe(ClientMessageType.ClientMessageTypeEnum.REGISTER);
      expect(sent.evaluationId).toBe('eval-1');
    });

    it('pings immediately on open so liveness is anchored without waiting a full interval', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      expect(MockWebSocket.instance!.sentTypes()).toEqual([
        ClientMessageType.ClientMessageTypeEnum.REGISTER,
        ClientMessageType.ClientMessageTypeEnum.PING,
      ]);
    });

    it('is a no-op when already connected to the same evaluation', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const first = MockWebSocket.instance;

      service.connect('eval-1');

      expect(MockWebSocket.instance).toBe(first);
    });

    it('reconnects when called with a different evaluationId', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const first = MockWebSocket.instance;

      service.connect('eval-2');

      expect(MockWebSocket.instance).not.toBe(first);
    });
  });

  // ── messages$ ──────────────────────────────────────────────────────────────

  describe('messages$', () => {
    it('emits parsed server messages', () => {
      const received: IWsServerMessage[] = [];
      service.messages$.subscribe((m) => received.push(m));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateMessage(serverMsg(ServerMessageType.ServerMessageTypeEnum.TASK_START));

      expect(received.length).toBe(1); // opening the socket emits no message; only the frame below does
      expect(received[received.length - 1].type).toBe(ServerMessageType.ServerMessageTypeEnum.TASK_START);
    });

    it('emits multiple consecutive messages', () => {
      const received: IWsServerMessage[] = [];
      service.messages$.subscribe((m) => received.push(m));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateMessage(serverMsg(ServerMessageType.ServerMessageTypeEnum.TASK_START));
      MockWebSocket.instance!.simulateMessage(serverMsg(ServerMessageType.ServerMessageTypeEnum.TASK_END));

      expect(received.map((m) => m.type)).toEqual([
        ServerMessageType.ServerMessageTypeEnum.TASK_START,
        ServerMessageType.ServerMessageTypeEnum.TASK_END,
      ]);
    });

    it('silently ignores malformed JSON', () => {
      const received: IWsServerMessage[] = [];
      service.messages$.subscribe((m) => received.push(m));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateRawMessage('not-json');

      expect(received.length).toBe(0);
    });
  });

  // ── disconnect() ───────────────────────────────────────────────────────────

  describe('disconnect', () => {
    it('sends UNREGISTER before closing', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      service.disconnect();

      expect(MockWebSocket.instance!.sentTypes()).toContain(ClientMessageType.ClientMessageTypeEnum.UNREGISTER);
    });

    it('sets socket to null after disconnect', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      service.disconnect();

      expect((service as any).socket).toBeNull();
    });

    it('does not attempt reconnect after explicit disconnect', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      service.disconnect();

      const socketAfterDisconnect = MockWebSocket.instance;
      tick(6000);

      expect(MockWebSocket.instance).toBe(socketAfterDisconnect);
    }));

    it('reports idle after an explicit disconnect', () => {
      const states: string[] = [];
      service.status$.subscribe((s) => states.push(s.state));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      service.disconnect();

      expect(states[states.length - 1]).toBe('idle');
      expect(service.status.degraded).toBeFalse();
    });
  });

  // ── auto-reconnect ─────────────────────────────────────────────────────────

  describe('auto-reconnect', () => {
    it('reconnects after the connection drops unexpectedly', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const first = MockWebSocket.instance;

      MockWebSocket.instance!.simulateClose();
      tick(5000);

      expect(MockWebSocket.instance).not.toBe(first);
      expect(MockWebSocket.instance!.url).toBe(WS_URL);
    }));
  });

  // ── reconnected$ ───────────────────────────────────────────────────────────

  describe('reconnected$', () => {
    it('does not emit on the initial connection', () => {
      const reconnects: void[] = [];
      service.reconnected$.subscribe(() => reconnects.push(undefined));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      expect(reconnects.length).toBe(0);
    });

    it('emits when the socket re-opens after an unexpected drop', fakeAsync(() => {
      const reconnects: void[] = [];
      service.reconnected$.subscribe(() => reconnects.push(undefined));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();
      tick(5000);
      MockWebSocket.instance!.simulateOpen();

      expect(reconnects.length).toBe(1);
    }));

    it('does not emit again after an explicit disconnect and fresh connect', () => {
      const reconnects: void[] = [];
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      service.disconnect();

      service.reconnected$.subscribe(() => reconnects.push(undefined));
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      expect(reconnects.length).toBe(0);
    });
  });

  // ── keep-alive ─────────────────────────────────────────────────────────────

  describe('keep-alive', () => {
    it('sends a PING message on the keep-alive interval', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const before = MockWebSocket.instance!.sentTypes().filter((t) => ClientMessageType.ClientMessageTypeEnum.PING === t).length;

      tick(PING_INTERVAL);

      const pings = MockWebSocket.instance!.sentTypes().filter((t) => ClientMessageType.ClientMessageTypeEnum.PING === t);
      expect(pings.length).toBe(before + 1);
    }));
  });

  // ── status$: liveness ──────────────────────────────────────────────────────

  describe('status$', () => {
    it('starts idle and does not report a degraded poll rate', () => {
      expect(service.status.state).toBe('idle');
      expect(service.status.degraded).toBeFalse();
    });

    it('reports live once messages arrive', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      expect(service.status.state).toBe('live');
      expect(service.status.degraded).toBeFalse();
    });

    it('replays the current status to a late subscriber', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      const seen: WebSocketStatus[] = [];
      service.status$.subscribe((s) => seen.push(s));

      expect(seen.length).toBe(1);
      expect(seen[0].state).toBe('live');
    });

    /**
     * The reported symptom: the backend was shut down, the browser kept the TCP connection looking
     * open, and the UI went on claiming it was live. A silent window must be reported instead of
     * being treated as a healthy connection.
     */
    it('reports waiting when the socket goes completely silent', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      expect(service.status.state).toBe('live');

      /* Nothing arrives for a whole liveness window. */
      tick(LIVENESS_TIMEOUT);

      expect(service.status.state).toBe('waiting');
      expect(service.status.degraded).toBeTrue();
    }));

    it('probes with a ping when a window is silent, so the next window can confirm the state', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const pingsBefore = MockWebSocket.instance!.sentTypes().filter(
        (t) => t === ClientMessageType.ClientMessageTypeEnum.PING
      ).length;

      tick(LIVENESS_TIMEOUT);

      const pingsAfter = MockWebSocket.instance!.sentTypes().filter(
        (t) => t === ClientMessageType.ClientMessageTypeEnum.PING
      ).length;
      expect(pingsAfter).toBeGreaterThan(pingsBefore);
    }));

    it('a silent healthy run stays merely quiet when the ping is answered', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      tick(LIVENESS_TIMEOUT);
      expect(service.status.state).toBe('waiting');

      /* The run is idle, but the server answers the probe: the socket is fine. */
      MockWebSocket.instance!.simulateMessage(pingReply());
      tick(PING_INTERVAL);
      MockWebSocket.instance!.simulateMessage(pingReply());

      expect(service.status.state).toBe('live');
      expect(service.status.degraded).toBeFalse();
    }));

    /**
     * A socket that stays silent for a second window cannot be trusted even though the browser
     * still calls it open. Waiting for TCP to notice takes minutes, so it is replaced directly.
     */
    it('forces a reconnect after two silent windows instead of waiting for TCP', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const dead = MockWebSocket.instance;

      tick(LIVENESS_TIMEOUT);
      expect(service.status.state).toBe('waiting');

      tick(LIVENESS_TIMEOUT);
      expect(service.status.state).toBe('reconnecting');
      expect(dead!.closed).toBeTrue();

      /* And a fresh socket follows. */
      tick(5000);
      expect(MockWebSocket.instance).not.toBe(dead);
    }));

    it('reports reconnecting as soon as the socket closes', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      MockWebSocket.instance!.simulateClose();

      expect(service.status.state).toBe('reconnecting');
      expect(service.status.degraded).toBeTrue();
    }));

    /**
     * A socket that was silent and then spoke again means messages may have been missed while the UI
     * believed it was live. `resync$` announces that exactly once.
     */
    it('emits resync when a silent-but-open socket speaks again', fakeAsync(() => {
      let resyncs = 0;
      service.resync$.subscribe(() => resyncs++);

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      tick(LIVENESS_TIMEOUT);
      expect(service.status.state).toBe('waiting');

      MockWebSocket.instance!.simulateMessage(pingReply());
      tick(0);

      expect(resyncs).toBe(1);
      expect(service.status.state).toBe('live');
    }));

    it('emits resync only once per silent window, not for every message', fakeAsync(() => {
      let resyncs = 0;
      service.resync$.subscribe(() => resyncs++);

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      tick(LIVENESS_TIMEOUT);
      MockWebSocket.instance!.simulateMessage(pingReply());
      MockWebSocket.instance!.simulateMessage(pingReply());
      MockWebSocket.instance!.simulateMessage(pingReply());
      tick(0);

      expect(resyncs).toBe(1);
    }));

    it('does not emit resync for a socket that merely reconnects', fakeAsync(() => {
      let resyncs = 0;
      service.resync$.subscribe(() => resyncs++);

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();
      tick(5000);
      MockWebSocket.instance!.simulateOpen();
      tick(0);

      /* A reconnect reports through reconnected$ instead; the two must not both fire. */
      expect(resyncs).toBe(0);
    }));
  });

  // ── status$: polling cadence ───────────────────────────────────────────────

  describe('poll interval', () => {
    it('uses the resting cadence while live', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      expect(service.status.pollIntervalMs).toBe(30_000);
    });

    it('shortens the cadence as soon as the socket is lost', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      MockWebSocket.instance!.simulateClose();

      expect(service.status.pollIntervalMs).toBeLessThan(30_000);
    });

    /**
     * The cadence is driven by observed change rather than by elapsed outage time, so a long outage in
     * which something keeps happening stays on the short interval instead of sliding to the slow one.
     */
    it('does not slide towards the slow end just because an outage lasts', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();
      expect(service.status.pollIntervalMs).toBe(5_000);

      /* Every poll finds something new, and every reconnect attempt fails for ten minutes. */
      for (let i = 0; i < 40; i++) {
        tick(15_000);
        service.reportPollResult(true);
        MockWebSocket.instances[MockWebSocket.instances.length - 1].simulateClose();
      }

      expect(service.status.pollIntervalMs).toBe(5_000);
    }));

    it('restores the resting cadence once the socket recovers', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();
      expect(service.status.pollIntervalMs).toBeLessThan(30_000);

      tick(5000);
      MockWebSocket.instance!.simulateOpen();

      expect(service.status.pollIntervalMs).toBe(30_000);
      expect(service.status.degraded).toBeFalse();
    }));

    /** A silent socket is degraded, and the poll must reflect that too. */
    it('uses the degraded cadence while the socket is only quiet', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      tick(LIVENESS_TIMEOUT);

      expect(service.status.state).toBe('waiting');
      expect(service.status.pollIntervalMs).toBe(5_000);
    }));
  });

  // ── reportPollResult(): AIMD ──────────────────────────────────────────────

  describe('adaptive cadence', () => {
    /** Put the service into the "lost the socket" state, which is when the cadence starts mattering. */
    function loseSocket() {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();
      expect(service.status.pollIntervalMs).toBe(5_000);
    }

    /**
     * Advances fake time without letting the socket fall silent: one long tick would trip the
     * liveness window and drop the service into `waiting`, which changes the cadence floor.
     */
    function elapseWhileLive(ms: number) {
      const step = 10_000;
      for (let left = ms; left > 0; left -= step) {
        tick(Math.min(step, left));
        MockWebSocket.instance!.simulateMessage(pingReply());
      }
    }

    /** The guard lets at most one adjustment per current interval through. */
    function pollAfterWaiting(found: boolean) {
      tick(service.status.pollIntervalMs);
      service.reportPollResult(found);
    }

    it('keeps the fast cadence as long as polls keep finding something', fakeAsync(() => {
      loseSocket();
      for (let i = 0; i < 6; i++) {
        pollAfterWaiting(true);
        expect(service.status.pollIntervalMs).toBe(5_000);
      }
    }));

    it('grows the cadence by a fixed step while nothing changes', fakeAsync(() => {
      loseSocket();
      const observed: number[] = [service.status.pollIntervalMs];
      for (let i = 0; i < 4; i++) {
        pollAfterWaiting(false);
        observed.push(service.status.pollIntervalMs);
      }

      expect(observed).toEqual([5_000, 10_000, 15_000, 20_000, 25_000]);
    }));

    it('never grows past the maximum', fakeAsync(() => {
      loseSocket();
      for (let i = 0; i < 40; i++) {
        pollAfterWaiting(false);
      }

      expect(service.status.pollIntervalMs).toBe(60_000);
    }));

    it('halves the cadence as soon as something changed', fakeAsync(() => {
      loseSocket();
      pollAfterWaiting(false);
      pollAfterWaiting(false);
      expect(service.status.pollIntervalMs).toBe(15_000);

      pollAfterWaiting(true);

      expect(service.status.pollIntervalMs).toBe(7_500);
    }));

    it('stops decreasing at the fast end', fakeAsync(() => {
      loseSocket();
      for (let i = 0; i < 5; i++) {
        pollAfterWaiting(true);
      }

      expect(service.status.pollIntervalMs).toBe(5_000);
    }));

    /** While the socket is healthy the cadence may grow, but never below the resting interval. */
    it('does not let a live socket poll faster than the resting cadence', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      elapseWhileLive(30_000);
      service.reportPollResult(true);

      /* Halving alone would reach 15 000, but that is needless load while the socket is live. */
      expect(service.status.pollIntervalMs).toBe(30_000);
    }));

    it('adapts the live cadence too, but keeps the floor after a change', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      for (let i = 0; i < 6; i++) {
        elapseWhileLive(service.status.pollIntervalMs);
        service.reportPollResult(false);
      }
      expect(service.status.pollIntervalMs).toBe(60_000);

      elapseWhileLive(60_000);
      service.reportPollResult(true);
      expect(service.status.pollIntervalMs).toBe(30_000);
    }));

    it('ignores extra reports within the same interval so several pollers do not double-step', fakeAsync(() => {
      loseSocket();
      /* Two views polled the same response: only the first may move the cadence. */
      tick(5_000);
      service.reportPollResult(false);
      service.reportPollResult(false);
      service.reportPollResult(false);

      expect(service.status.pollIntervalMs).toBe(10_000);
    }));

    it('resets to the fast cadence when the socket is lost again after recovering', fakeAsync(() => {
      loseSocket();
      pollAfterWaiting(false);
      pollAfterWaiting(false);
      expect(service.status.pollIntervalMs).toBe(15_000);

      /* Recovery, then a fresh drop: the outage clock starts over. */
      tick(10_000);
      MockWebSocket.instance!.simulateOpen();
      expect(service.status.pollIntervalMs).toBe(30_000);
      MockWebSocket.instance!.simulateClose();

      expect(service.status.pollIntervalMs).toBe(5_000);
    }));

    it('publishes every cadence change so pollers switch over', fakeAsync(() => {
      loseSocket();

      /* Subscribed after the drop: the replay is the fast cadence running pollers already use. */
      const seen: number[] = [];
      service.status$.subscribe((s) => seen.push(s.pollIntervalMs));

      pollAfterWaiting(false);

      expect(seen).toEqual([5_000, 10_000]);
    }));
  });

  // ── fallbackPolling() ─────────────────────────────────────────────────────

  describe('fallbackPolling', () => {
    it('emits immediately so the first load does not wait for a tick', fakeAsync(() => {
      let emissions = 0;
      const sub = service.fallbackPolling().subscribe(() => emissions++);

      tick(0);
      expect(emissions).toBe(1);
      sub.unsubscribe();
    }));

    it('emits on the current cadence', fakeAsync(() => {
      let emissions = 0;
      const sub = service.fallbackPolling().subscribe(() => emissions++);
      tick(0);

      tick(30_000);

      expect(emissions).toBe(2);
      sub.unsubscribe();
    }));

    it('speeds up when the socket drops', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      let emissions = 0;
      const sub = service.fallbackPolling().subscribe(() => emissions++);
      tick(0);
      expect(emissions).toBe(1);

      MockWebSocket.instance!.simulateClose();
      /* The degraded cadence is 5 s, and the switch to it restarts the timer immediately. */
      tick(5_000);

      expect(emissions).toBeGreaterThanOrEqual(2);
      sub.unsubscribe();
    }));

    it('keeps polling on the grown cadence while nothing changes', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();

      let emissions = 0;
      const sub = service.fallbackPolling().subscribe(() => emissions++);
      tick(0);
      expect(emissions).toBe(1);

      /* Two quiet polls push the cadence from 5 s to 15 s. */
      tick(5_000);
      expect(emissions).toBe(2);
      service.reportPollResult(false);
      tick(10_000);
      expect(emissions).toBe(3);
      service.reportPollResult(false);

      /* Polls now come every 15 s instead of every 5 s. */
      tick(14_000);
      expect(emissions).toBe(3);
      tick(1_000);
      expect(emissions).toBe(4);

      sub.unsubscribe();
    }));

    it('polls again one shortened interval after a change, not immediately', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateClose();

      let emissions = 0;
      const sub = service.fallbackPolling().subscribe(() => emissions++);
      tick(0);
      expect(emissions).toBe(1);

      /* Grow the cadence, then report a change right after a poll. */
      tick(5_000);
      expect(emissions).toBe(2);
      service.reportPollResult(false);
      expect(service.status.pollIntervalMs).toBe(10_000);

      tick(10_000);
      expect(emissions).toBe(3);
      service.reportPollResult(true);
      expect(service.status.pollIntervalMs).toBe(5_000);

      /* No immediate re-poll: the next one is one (now shorter) interval away. */
      tick(0);
      expect(emissions).toBe(3);
      tick(5_000);
      expect(emissions).toBe(4);

      sub.unsubscribe();
    }));

    it('does not duplicate polls when several views poll at once', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      let first = 0;
      let second = 0;
      const subs = [
        service.fallbackPolling().subscribe(() => first++),
        service.fallbackPolling().subscribe(() => second++),
      ];
      tick(0);

      expect([first, second]).toEqual([1, 1]);
      subs.forEach((s) => s.unsubscribe());
    }));

    it('emits when a caller-supplied trigger fires', fakeAsync(() => {
      const trigger = new (require('rxjs').Subject)();
      let emissions = 0;
      const sub = service.fallbackPolling(trigger).subscribe(() => emissions++);
      tick(0);
      const before = emissions;

      trigger.next(undefined);
      tick(0);

      expect(emissions).toBe(before + 1);
      sub.unsubscribe();
    }));

    it('emits after a reconnect so a missed window is repaired immediately', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      let emissions = 0;
      const sub = service.fallbackPolling().subscribe(() => emissions++);
      tick(0);
      const before = emissions;

      MockWebSocket.instance!.simulateClose();
      tick(5000);
      MockWebSocket.instance!.simulateOpen();
      tick(0);

      expect(emissions).toBeGreaterThan(before);
      sub.unsubscribe();
    }));
  });
});
