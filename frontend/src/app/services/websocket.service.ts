import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable, Subject, Subscription, interval, merge, timer } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { AppConfig } from '../app.config';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { IWsClientMessage } from '../model/ws/ws-client-message.interface';
import { ClientMessageType } from '../model/ws/client-message-type.enum';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

/**
 * Health of the run WebSocket.
 *
 * - `idle`: nothing is connected (no run view, or the view was left).
 * - `live`: messages are arriving; pushed updates can be trusted.
 * - `waiting`: the socket is still open but nothing has arrived for a whole liveness window. The
 *   connection may be fine (an idle run) or the far end may be gone in a way the browser cannot see,
 *   so the UI must say that updates are no longer live.
 * - `reconnecting`: the socket is closed or unusable and the service is retrying with backoff.
 * - `rejected`: the server explicitly refused this session (unknown run, no permission). No amount
 *   of retrying helps, so reconnect attempts stop; only a new registration (or a reload) can fix it.
 */
export type WebSocketConnectionState = 'idle' | 'live' | 'waiting' | 'reconnecting' | 'rejected';

/** Connection state as displayed to the user, together with the polling cadence it implies. */
export interface WebSocketStatus {
  state: WebSocketConnectionState;
  /** True when pushed updates cannot be trusted and the UI is falling back to REST polling. */
  degraded: boolean;
  /**
   * Interval, in milliseconds, at which fallback polling should currently run.
   *
   * This is not fixed: it adapts to what polls actually return (see {@link WebSocketService.reportPollResult}).
   */
  pollIntervalMs: number;
  /** Why the server refused this session, or null. Only set while the state is `rejected`. */
  reason: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class WebSocketService implements OnDestroy {
  /** Base delay for the first reconnect attempt. */
  private static readonly RECONNECT_BASE_DELAY_MS = 1_000;
  /** Upper bound for the reconnect delay, reached after repeated failures. */
  private static readonly RECONNECT_MAX_DELAY_MS = 30_000;

  /**
   * Keep-alive interval. The server uses these pings to keep a viewer marked online, and the reply
   * doubles as a liveness probe.
   */
  private static readonly PING_INTERVAL_MS = 15_000;

  /**
   * How long the socket may be completely silent before the UI stops trusting it. Must stay
   * comfortably above {@link PING_INTERVAL_MS}, otherwise a healthy but idle run (where the only
   * traffic *is* the ping reply) would be reported as degraded every cycle.
   */
  private static readonly LIVENESS_TIMEOUT_MS = 30_000;

  /**
   * Polling cadence while pushed updates cannot be trusted. An outage starts here: fast enough that
   * losing the socket costs at most a few seconds of staleness.
   */
  private static readonly POLL_FAST_MS = 5_000;

  /**
   * Polling cadence while the socket is healthy: events do the real work, this is only a safety net.
   * Also the floor an outage has to earn its way back down to after recovering.
   */
  private static readonly POLL_RESTING_MS = 30_000;

  /** Upper bound of the adaptive cadence, reached while nothing has been changing for a while. */
  private static readonly POLL_MAX_MS = 60_000;

  /** AIMD: additive increase per poll that found nothing new. */
  private static readonly POLL_INCREASE_MS = 5_000;

  /** AIMD: multiplicative decrease as soon as a poll found something new. */
  private static readonly POLL_DECREASE = 2;

  private socket: WebSocket | null = null;
  private messageSubject = new Subject<IWsServerMessage>();
  private reconnectedSubject = new Subject<void>();
  private resyncSubject = new Subject<void>();
  private statusSubject: BehaviorSubject<WebSocketStatus>;
  private lastStatus: WebSocketStatus = {
    state: 'idle',
    degraded: false,
    pollIntervalMs: WebSocketService.POLL_RESTING_MS,
    reason: null,
  };
  /** Why the server refused this session; cleared as soon as a new connection is attempted. */
  private rejectionReason: string | null = null;
  private pingSubscription: Subscription | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private livenessTimeout: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempts = 0;
  private currentEvaluationId: string | null = null;
  private statusState: WebSocketConnectionState = 'idle';
  /** Current adaptive polling cadence; moved by {@link reportPollResult} between state changes. */
  private pollIntervalMs = WebSocketService.POLL_RESTING_MS;
  /** When the cadence last grew or shrank, so one poll cannot be counted twice by several views. */
  private lastPollAdjustmentAt = 0;

  /** When the last fallback poll was observed; only polls of the current cadence count. */
  private lastPollAt = 0;
  /** Liveness windows in a row that produced no traffic at all. */
  private silentWindows = 0;

  /** Observable stream of messages pushed by the server. */
  readonly messages$: Observable<IWsServerMessage> = this.messageSubject.asObservable();

  /**
   * Current connection health, emitted on every change and replayed to new subscribers.
   *
   * The UI uses this both to warn the user and to shorten its fallback polling; it is the single
   * place that decides whether pushed updates can be trusted.
   */
  readonly status$: Observable<WebSocketStatus>;

  /**
   * Fires whenever the socket re-opens after an unexpected drop (NOT on the initial connect).
   * Consumers should treat this as a signal that they may have missed messages while
   * disconnected and should refetch their state from the REST API to resync.
   */
  readonly reconnected$: Observable<void> = this.reconnectedSubject.asObservable();

  /**
   * Fires when liveness is confirmed for a socket that had gone silent, but only if the socket never
   * closed (a closed socket reports through {@link reconnected$} instead, so the two never overlap).
   * Consumers should refetch once, because messages sent during the silent window were missed.
   */
  readonly resync$: Observable<void> = this.resyncSubject.asObservable();

  constructor(private config: AppConfig) {
    this.statusSubject = new BehaviorSubject<WebSocketStatus>(this.lastStatus);
    this.status$ = this.statusSubject.pipe(
      distinctUntilChanged(
        (a, b) => a.state === b.state && a.degraded === b.degraded && a.pollIntervalMs === b.pollIntervalMs
      )
    );
  }

  /** Current connection state, for code that cannot subscribe. */
  get status(): WebSocketStatus {
    return this.lastStatus;
  }

  /**
   * Connects to the WebSocket endpoint and registers for the given evaluation.
   * Safe to call multiple times — no-ops if already connected to the same evaluation.
   */
  connect(evaluationId: string): void {
    if (this.socket?.readyState === WebSocket.OPEN && this.currentEvaluationId === evaluationId) {
      return;
    }
    this.closeSocket();
    this.currentEvaluationId = evaluationId;
    this.openConnection();
  }

  /** Disconnects from the WebSocket endpoint and stops reconnect attempts. */
  disconnect(): void {
    this.currentEvaluationId = null;
    this.closeSocket();
    this.setStatus('idle');
  }

  /**
   * Emits once immediately and then on the current fallback cadence, plus on [triggers].
   *
   * When the socket drops the cadence shortens so the fallback is actually usable. Afterwards it is
   * adaptive: every consumer has to report whether its poll found anything with
   * {@link reportPollResult}, and the cadence grows while nothing changes and drops back as soon as
   * something does. A resync emits once as well, so a window in which events were missed is repaired
   * immediately.
   */
  fallbackPolling(triggers: Observable<unknown> = EMPTY): Observable<unknown> {
    const cadence = this.status$.pipe(
      map((status) => status.pollIntervalMs),
      distinctUntilChanged(),
      /* The first cadence fires at once, because the view has nothing on screen yet. Every later
         cadence change replaces the timer but waits a full new interval, so a poll that just found
         something is never immediately followed by another request. */
      switchMap((intervalMs, index) => (index === 0 ? timer(0, intervalMs) : timer(intervalMs, intervalMs)))
    );

    return merge(cadence, triggers, this.reconnected$, this.resync$);
  }

  /**
   * Records what a fallback poll returned and adapts the cadence accordingly (AIMD).
   *
   * Call this once per poll with `true` when the response differed from the previous one and `false`
   * when it did not. The cadence grows by a fixed step while nothing changes and shrinks
   * multiplicatively as soon as something does, which keeps a quiet run cheap without ever making a
   * busy one wait long. Polls that arrive after the cadence was already adjusted are ignored, so a
   * page with several pollers does not double-step the interval.
   */
  reportPollResult(changed: boolean): void {
    const now = Date.now();
    /* Several views report the same poll, and a slow poll can arrive after the interval already
       moved; neither may step the cadence twice. */
    if (now - this.lastPollAt < this.pollIntervalMs || now - this.lastPollAdjustmentAt < this.pollIntervalMs) {
      return;
    }
    this.lastPollAt = now;
    this.lastPollAdjustmentAt = now;

    const next = changed
      ? Math.max(
          this.statusState === 'live' ? WebSocketService.POLL_RESTING_MS : WebSocketService.POLL_FAST_MS,
          Math.round(this.pollIntervalMs / WebSocketService.POLL_DECREASE)
        )
      : Math.min(WebSocketService.POLL_MAX_MS, this.pollIntervalMs + WebSocketService.POLL_INCREASE_MS);

    if (next !== this.pollIntervalMs) {
      this.publishPollInterval(next);
    }
  }

  private openConnection(): void {
    const url = this.config.webSocketUrl;
    this.rejectionReason = null;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      const isReconnect = this.reconnectAttempts > 0;
      this.reconnectAttempts = 0;
      this.send({
        evaluationId: this.currentEvaluationId,
        type: ClientMessageType.ClientMessageTypeEnum.REGISTER,
      });
      this.startPing();
      this.markAlive();
      /* Ping right away: it anchors the liveness deadline and confirms that the server accepted the
         registration instead of silently ignoring an unauthorised session. */
      this.sendPing();
      if (isReconnect) {
        this.reconnectedSubject.next();
      }
    };

    this.socket.onmessage = (event: MessageEvent) => {
      this.markAlive();
      let message: IWsServerMessage;
      try {
        message = JSON.parse(event.data as string);
      } catch {
        console.warn('[WebSocketService] Could not parse message:', event.data);
        return;
      }
      /* An explicit refusal is not an event to distribute: it ends this session's usefulness. */
      if (message.type === ServerMessageType.ServerMessageTypeEnum.ERROR) {
        this.onRejected(message.error ?? 'The server refused this connection.');
        return;
      }
      this.messageSubject.next(message);
    };

    this.socket.onclose = () => {
      this.stopPing();
      this.stopLiveness();
      if (this.statusState === 'rejected') {
        /* The server said no; retrying would only produce the same answer. */
        return;
      }
      if (this.currentEvaluationId) {
        this.setStatus('reconnecting');
        const delay = this.nextReconnectDelay();
        this.reconnectTimeout = setTimeout(() => {
          this.reconnectTimeout = null;
          this.openConnection();
        }, delay);
      } else {
        this.setStatus('idle');
      }
    };

    this.socket.onerror = () => {
      /* `onclose` always follows an error and is the only place that changes state, so that the
         reconnect attempt and the status update cannot disagree. */
    };
  }

  private send(message: IWsClientMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    }
  }

  private closeSocket(): void {
    this.reconnectAttempts = 0;
    this.stopPing();
    this.stopLiveness();
    if (this.reconnectTimeout !== null) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    if (this.socket) {
      if (this.socket.readyState === WebSocket.OPEN) {
        this.send({
          evaluationId: this.currentEvaluationId,
          type: ClientMessageType.ClientMessageTypeEnum.UNREGISTER,
        });
        this.socket.close();
      }
      this.socket = null;
    }
  }

  /**
   * The server refused this session, so stop pretending the connection is usable.
   *
   * Refusals used to be invisible: the socket stayed open, no event ever arrived, and the UI kept
   * claiming it was connected. Polling still has to work (the REST API may answer for a session the
   * socket layer refused), so the fallback cadence is set to its fastest and the state is marked
   * `rejected` so the indicator can say why instead of looking merely quiet.
   */
  private onRejected(reason: string): void {
    this.rejectionReason = reason;
    this.stopPing();
    this.stopLiveness();
    if (this.reconnectTimeout !== null) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    const dead = this.socket;
    this.socket = null;
    if (dead) {
      dead.onopen = null;
      dead.onmessage = null;
      dead.onclose = null;
      dead.onerror = null;
      try {
        dead.close();
      } catch {
        /* Nothing left to close. */
      }
    }
    this.setStatus('rejected');
  }

  /**
   * Records that the connection produced something, and re-arms the silence timer.
   *
   * Every inbound message proves the socket is alive, so this is also the only place that can move
   * the status out of {@link WebSocketConnectionState.waiting}.
   */
  private markAlive(): void {
    const wasSilent = this.silentWindows > 0;
    this.silentWindows = 0;
    if (this.statusState === 'waiting') {
      this.setStatus('live', { resync: wasSilent });
    } else if (this.statusState !== 'live') {
      this.setStatus('live');
    }
    this.armLivenessTimer();
  }

  private armLivenessTimer(): void {
    if (this.livenessTimeout !== null) {
      clearTimeout(this.livenessTimeout);
    }
    this.livenessTimeout = setTimeout(() => this.onSilenceElapsed(), WebSocketService.LIVENESS_TIMEOUT_MS);
  }

  /**
   * The socket has been completely silent for a full window: the open connection may be a corpse (a
   * backend that was shut down leaves browsers in exactly this state until the OS eventually times
   * the TCP connection out). Say so instead of pretending updates are still arriving, and probe with
   * a ping so the next window can actually confirm or deny it.
   */
  private onSilenceElapsed(): void {
    this.livenessTimeout = null;
    if (this.socket?.readyState !== WebSocket.OPEN) {
      /* A close is on its way or already handled; `onclose` owns the state. */
      return;
    }
    this.silentWindows++;
    if (this.silentWindows === 1) {
      this.setStatus('waiting');
      this.sendPing();
      this.armLivenessTimer();
      return;
    }
    /* A second window with nothing at all: the connection is unusable even though the browser still
       reports it as open. Replace it instead of waiting for a TCP timeout that can take minutes. */
    this.setStatus('reconnecting');
    this.forceReconnect();
  }

  /**
   * Discards a socket the browser believes is open but that has gone silent, and reconnects.
   *
   * `close()` on such a socket can take minutes to surface as `onclose`, which is exactly the stall
   * this guards against; detaching the handlers first means the deliberate reconnect below is the
   * only one that happens.
   */
  private forceReconnect(): void {
    this.stopPing();
    const dead = this.socket;
    this.socket = null;
    if (dead) {
      dead.onopen = null;
      dead.onmessage = null;
      dead.onclose = null;
      dead.onerror = null;
      try {
        dead.close();
      } catch {
        /* Nothing left to close. */
      }
    }
    if (this.currentEvaluationId && this.reconnectTimeout === null) {
      this.reconnectAttempts++;
      const delay = this.nextReconnectDelay();
      this.reconnectTimeout = setTimeout(() => {
        this.reconnectTimeout = null;
        this.openConnection();
      }, delay);
    }
  }

  private sendPing(): void {
    if (this.currentEvaluationId) {
      this.send({
        evaluationId: this.currentEvaluationId,
        type: ClientMessageType.ClientMessageTypeEnum.PING,
      });
    }
  }

  private setStatus(state: WebSocketConnectionState, options: { resync?: boolean } = {}): void {
    const previous = this.statusState;
    if (previous === state) {
      return;
    }
    this.statusState = state;

    const degraded = state === 'waiting' || state === 'reconnecting' || state === 'rejected';

    /* Losing the socket (or losing trust in it) restarts the adaptive cadence from a useful speed;
       it then grows again from observed change rather than from elapsed outage time. Recovery keeps
       whatever cadence was earned, but at least the resting one. */
    if (degraded) {
      this.pollIntervalMs = Math.min(this.pollIntervalMs, WebSocketService.POLL_FAST_MS);
    } else if (state === 'live') {
      this.pollIntervalMs = Math.max(this.pollIntervalMs, WebSocketService.POLL_RESTING_MS);
    } else {
      this.pollIntervalMs = WebSocketService.POLL_RESTING_MS;
    }
    this.lastPollAt = 0;
    this.lastPollAdjustmentAt = 0;

    this.publishStatus();

    /* A silent-but-open socket never fires `onclose`, so its recovery has to be announced here. */
    if (options.resync === true && previous === 'waiting') {
      this.resyncSubject.next();
    }
  }

  /** Publishes the current cadence without changing the connection state. */
  private publishPollInterval(pollIntervalMs: number): void {
    this.pollIntervalMs = pollIntervalMs;
    this.publishStatus();
  }

  private publishStatus(): void {
    this.lastStatus = {
      state: this.statusState,
      degraded: this.statusState === 'waiting' || this.statusState === 'reconnecting' || this.statusState === 'rejected',
      pollIntervalMs: this.pollIntervalMs,
      reason: this.statusState === 'rejected' ? this.rejectionReason : null,
    };
    this.statusSubject.next(this.lastStatus);
  }

  /**
   * Computes the delay before the next reconnect attempt using exponential backoff with jitter,
   * capped at {@link RECONNECT_MAX_DELAY_MS}.
   */
  private nextReconnectDelay(): number {
    const exponentialDelay = WebSocketService.RECONNECT_BASE_DELAY_MS * 2 ** this.reconnectAttempts;
    const delay = Math.min(exponentialDelay, WebSocketService.RECONNECT_MAX_DELAY_MS);
    this.reconnectAttempts++;
    return delay / 2 + Math.random() * (delay / 2);
  }

  private startPing(): void {
    this.stopPing();
    this.pingSubscription = interval(WebSocketService.PING_INTERVAL_MS).subscribe(() => this.sendPing());
  }

  private stopPing(): void {
    this.pingSubscription?.unsubscribe();
    this.pingSubscription = null;
  }

  /** Forgets the liveness deadline; used whenever the socket stops being worth watching. */
  private stopLiveness(): void {
    if (this.livenessTimeout !== null) {
      clearTimeout(this.livenessTimeout);
      this.livenessTimeout = null;
    }
    this.silentWindows = 0;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.messageSubject.complete();
    this.reconnectedSubject.complete();
    this.resyncSubject.complete();
    this.statusSubject.complete();
  }
}
