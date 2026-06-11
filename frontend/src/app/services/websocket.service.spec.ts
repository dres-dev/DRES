import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';
import { AppConfig } from '../app.config';
import { ClientMessageType } from '../model/ws/client-message-type.enum';
import { ServerMessageType } from '../model/ws/server-message-type.enum';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';

// ── Minimal WebSocket mock ────────────────────────────────────────────────────

class MockWebSocket {
  static instance: MockWebSocket | null = null;

  readyState: number = WebSocket.CONNECTING;
  sentMessages: string[] = [];

  onopen:    ((e: Event)        => void) | null = null;
  onmessage: ((e: MessageEvent) => void) | null = null;
  onclose:   ((e: CloseEvent)   => void) | null = null;
  onerror:   ((e: Event)        => void) | null = null;

  constructor(public url: string) {
    MockWebSocket.instance = this;
  }

  send(data: string) { this.sentMessages.push(data); }
  close()            { this.readyState = WebSocket.CLOSED; this.onclose?.(new CloseEvent('close')); }

  /** Test helper — simulate a successful connection. */
  simulateOpen() {
    this.readyState = WebSocket.OPEN;
    this.onopen?.(new Event('open'));
  }

  /** Test helper — simulate an incoming server message. */
  simulateMessage(msg: IWsServerMessage) {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(msg) }));
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const WS_URL = 'ws://localhost:8080/api/ws/run';

function makeConfig(): Partial<AppConfig> {
  return { webSocketUrl: WS_URL } as Partial<AppConfig>;
}

function serverMsg(type: ServerMessageType.ServerMessageTypeEnum, evaluationId = 'eval-1'): IWsServerMessage {
  return { evaluationId, type, timestamp: Date.now() };
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    MockWebSocket.instance = null;
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

      expect(received.length).toBe(1);
      expect(received[0].type).toBe(ServerMessageType.ServerMessageTypeEnum.TASK_START);
    });

    it('emits multiple consecutive messages', () => {
      const received: IWsServerMessage[] = [];
      service.messages$.subscribe((m) => received.push(m));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.simulateMessage(serverMsg(ServerMessageType.ServerMessageTypeEnum.TASK_START));
      MockWebSocket.instance!.simulateMessage(serverMsg(ServerMessageType.ServerMessageTypeEnum.TASK_END));

      expect(received.length).toBe(2);
    });

    it('silently ignores malformed JSON', () => {
      const received: IWsServerMessage[] = [];
      service.messages$.subscribe((m) => received.push(m));

      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      MockWebSocket.instance!.onmessage?.(new MessageEvent('message', { data: 'not-json' }));

      expect(received.length).toBe(0);
    });
  });

  // ── disconnect() ───────────────────────────────────────────────────────────

  describe('disconnect', () => {
    it('sends UNREGISTER before closing', () => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      service.disconnect();

      const msgs = MockWebSocket.instance!.sentMessages.map((s) => JSON.parse(s));
      const unregister = msgs.find((m) => m.type === ClientMessageType.ClientMessageTypeEnum.UNREGISTER);
      expect(unregister).toBeDefined();
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
  });

  // ── auto-reconnect ─────────────────────────────────────────────────────────

  describe('auto-reconnect', () => {
    it('reconnects after 5 seconds when connection drops unexpectedly', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();
      const first = MockWebSocket.instance;

      // Simulate unexpected server-side close
      MockWebSocket.instance!.readyState = WebSocket.CLOSED;
      MockWebSocket.instance!.onclose?.(new CloseEvent('close'));

      tick(5000);

      expect(MockWebSocket.instance).not.toBe(first);
      expect(MockWebSocket.instance!.url).toBe(WS_URL);
    }));
  });

  // ── ping ───────────────────────────────────────────────────────────────────

  describe('ping', () => {
    it('sends a PING message every 30 seconds', fakeAsync(() => {
      service.connect('eval-1');
      MockWebSocket.instance!.simulateOpen();

      tick(30_000);

      const msgs = MockWebSocket.instance!.sentMessages.map((s) => JSON.parse(s));
      const ping = msgs.find((m) => m.type === ClientMessageType.ClientMessageTypeEnum.PING);
      expect(ping).toBeDefined();
      expect(ping.evaluationId).toBe('eval-1');
    }));
  });
});
