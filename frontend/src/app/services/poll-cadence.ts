import { WebSocketService } from './websocket.service';

/**
 * Remembers the last state a view model fetched, so a fallback poll can tell whether it learned
 * anything new and let the polling cadence adapt to it.
 *
 * Without this, a poller can only report "I ran", which forces the cadence to be a fixed guess: too
 * slow for a busy run, needlessly fast for a run where nothing happens for minutes.
 *
 * Usage:
 * ```ts
 * private readonly statePolls = new PollCadence(this.wsService, 'state');
 * // in the HTTP pipeline of every poll:
 * map((state) => this.statePolls.observe(state)),
 * ```
 */
export class PollCadence {
  private previous: string | undefined;

  /**
   * @param webSocketService the service that owns the adaptive cadence
   * @param label identifies this poller in tests and debugger output
   */
  constructor(
    private readonly webSocketService: WebSocketService,
    private readonly label: string
  ) {}

  /**
   * Fingerprints [snapshot], reports whether it differs from the previous one, and returns the
   * fingerprint so the caller can keep it. The first observation always counts as a change: it is
   * the load that fills an empty view.
   */
  observe(snapshot: unknown): string {
    const fingerprint = PollCadence.fingerprint(snapshot);
    const changed = this.previous !== undefined && fingerprint !== this.previous;
    const first = this.previous === undefined;
    this.previous = fingerprint;
    this.webSocketService.reportPollResult(first || changed);
    return fingerprint;
  }

  /** Forgets the previous snapshot, e.g. when the observed run changes. */
  reset(): void {
    this.previous = undefined;
  }

  get name(): string {
    return this.label;
  }

  /**
   * Stable, key-sorted serialisation, so two structurally equal states produce the same string and
   * a reordered response does not look like a change.
   */
  static fingerprint(value: unknown): string {
    return JSON.stringify(PollCadence.stabilise(value));
  }

  private static stabilise(value: unknown): unknown {
    if (Array.isArray(value)) {
      return value.map((entry) => PollCadence.stabilise(entry));
    }
    if (value !== null && typeof value === 'object') {
      const record = value as Record<string, unknown>;
      const sorted: Record<string, unknown> = {};
      for (const key of Object.keys(record).sort()) {
        sorted[key] = PollCadence.stabilise(record[key]);
      }
      return sorted;
    }
    return value;
  }
}
