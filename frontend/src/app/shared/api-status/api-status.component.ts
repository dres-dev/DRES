import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Observable, Subscription, interval } from 'rxjs';
import { catchError, filter, map, switchMap } from 'rxjs/operators';
import { CurrentTime, StatusService } from '../../../../openapi';
import { WebSocketConnectionState, WebSocketService } from '../../services/websocket.service';

/** Everything the status pill shows, derived from the REST ping and the WebSocket state. */
export interface ApiStatusView {
  /** Round-trip time of the last successful REST request, or -1 before the first one. */
  rtt: number;
  /** Whether the last REST request failed; cleared as soon as one succeeds again. */
  restDown: boolean;
  /** Health of the run WebSocket, `idle` when no run view is open. */
  socket: WebSocketConnectionState;
  /** True when the socket cannot be trusted and the UI is polling instead. */
  degraded: boolean;
  /** Current fallback polling interval, so the tooltip can be specific. */
  pollIntervalS: number;
  /** Why the server refused this session; only set while the socket is `rejected`. */
  reason: string | null;
}

/**
 * Shows whether the client is actually in touch with the backend.
 *
 * This is deliberately one indicator rather than two: the interesting state is the combination. A
 * socket that looks open but has gone silent, and a REST request that fails, are both "the UI is
 * showing stale data", and the user should learn that from one place instead of having to compare
 * two icons.
 *
 * REST liveness and socket liveness are independent: a failed REST request does not mean events
 * stopped arriving, and a silent socket does not mean the API is down.
 */
@Component({
  selector: 'app-api-status',
  templateUrl: './api-status.component.html',
  styleUrls: ['./api-status.component.scss'],
  standalone: false,
})
export class ApiStatusComponent implements OnInit, OnDestroy {
  @Input() public pingFrequency = 10_000; // ms

  /** Rolling view state, so the template needs a single async pipe. */
  view: BehaviorSubject<ApiStatusView>;

  private rtt = -1;
  private restDown = false;
  private socket: WebSocketConnectionState = 'idle';
  private degraded = false;
  private pollIntervalMs = 30_000;
  private reason: string | null = null;
  private readonly subscriptions = new Subscription();

  constructor(
    private statusService: StatusService,
    private webSocketService: WebSocketService
  ) {
    this.view = new BehaviorSubject<ApiStatusView>(this.snapshot());
  }

  /** Server-to-client clock difference in milliseconds, once it has been measured. */
  get clockDiff$(): Observable<number> {
    return this.view.pipe(
      map((v) => v.rtt),
      filter(() => this.clockOffset !== null),
      map(() => this.clockOffset as number)
    );
  }

  private clockOffset: number | null = null;

  ngOnInit(): void {
    this.subscriptions.add(this.webSocketService.status$.subscribe((status) => {
      this.socket = status.state;
      this.degraded = status.degraded;
      this.pollIntervalMs = status.pollIntervalMs;
      this.reason = status.reason;
      this.publish();
    }));

    this.subscriptions.add(
      interval(this.pingFrequency)
        .pipe(
          switchMap(() => {
            const sentAt = Date.now();
            /* The error is handled per request, not around the whole stream: catching outside the
               switchMap completed the interval on the first failure, which froze the displayed RTT at
               whatever value it had when the backend went away. */
            return this.statusService.getApiV2StatusTime().pipe(
              map((time: CurrentTime) => ({ time, sentAt })),
              catchError(() => {
                this.restDown = true;
                this.publish();
                return [];
              })
            );
          })
        )
        .subscribe(({ time, sentAt }) => {
          this.restDown = false;
          this.rtt = Date.now() - sentAt;
          this.clockOffset = Math.abs(time.timeStamp - (sentAt + this.rtt / 2));
          this.publish();
        })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.view.complete();
  }

  /** Whether the pill should be drawn in its "something is wrong" style. */
  get isDegraded(): boolean {
    return this.degraded || this.restDown;
  }

  /** Short label for the socket half of the pill. */
  socketLabel(status: ApiStatusView): string {
    switch (status.socket) {
      case 'live':
        return 'Live';
      case 'waiting':
        return 'Quiet';
      case 'reconnecting':
        return 'Reconnecting';
      case 'rejected':
        return 'Refused';
      default:
        return '';
    }
  }

  /**
   * Explains the current state in words, including the one thing the user needs to know but cannot
   * infer from an icon: that updates are now arriving on a timer rather than as they happen.
   */
  tooltip(status: ApiStatusView): string {
    const lines: string[] = [];
    if (status.restDown) {
      lines.push('The server is not answering requests.');
    } else if (status.rtt >= 0) {
      lines.push(`Round trip to the server: ${status.rtt} ms.`);
    } else {
      lines.push('Contacting the server…');
    }
    if (this.clockOffset !== null) {
      lines.push(`Clock difference to the server: ${this.clockOffset} ms.`);
    }

    switch (status.socket) {
      case 'live':
        lines.push('Live updates are flowing.');
        break;
      case 'waiting':
        lines.push('The live connection has gone quiet, so updates may be incomplete.');
        break;
      case 'reconnecting':
        lines.push('The live connection was lost and is being re-established.');
        break;
      case 'rejected':
        lines.push(status.reason ?? 'The server refused the live connection for this session.');
        break;
      default:
        break;
    }
    if (status.degraded) {
      lines.push(
        `Falling back to polling every ${status.pollIntervalS} s, so updates are slower than usual. ` +
          'The interval shortens again as soon as something changes.'
      );
    }
    return lines.join('\n');
  }

  private publish(): void {
    this.view.next(this.snapshot());
  }

  private snapshot(): ApiStatusView {
    return {
      rtt: this.rtt,
      restDown: this.restDown,
      socket: this.socket,
      degraded: this.degraded,
      pollIntervalS: Math.round(this.pollIntervalMs / 1000),
      reason: this.reason,
    };
  }
}
