import { PollCadence } from './poll-cadence';
import { WebSocketService } from './websocket.service';

describe('PollCadence', () => {
  let wsService: jasmine.SpyObj<WebSocketService>;
  let cadence: PollCadence;

  beforeEach(() => {
    wsService = jasmine.createSpyObj<WebSocketService>('WebSocketService', ['reportPollResult']);
    cadence = new PollCadence(wsService, 'state');
  });

  /** The single argument the service was told about on the nth call. */
  function reported(index: number): boolean {
    return wsService.reportPollResult.calls.argsFor(index)[0];
  }

  it('counts the first observation as a change so an empty view fills at the fast cadence', () => {
    cadence.observe({ status: 'RUNNING' });

    expect(wsService.reportPollResult).toHaveBeenCalledOnceWith(true);
  });

  it('reports no change for an identical snapshot', () => {
    cadence.observe({ status: 'RUNNING' });
    cadence.observe({ status: 'RUNNING' });

    expect(reported(1)).toBeFalse();
  });

  it('reports a change when a value moved', () => {
    cadence.observe({ status: 'RUNNING' });
    cadence.observe({ status: 'ENDED' });

    expect(reported(1)).toBeTrue();
  });

  it('ignores key order, so a reordered response is not mistaken for a change', () => {
    cadence.observe({ status: 'RUNNING', timeLeft: 10 });
    cadence.observe({ timeLeft: 10, status: 'RUNNING' });

    expect(reported(1)).toBeFalse();
  });

  it('compares nested structures, including arrays', () => {
    cadence.observe({ teams: [{ id: 'a', score: 1 }] });
    cadence.observe({ teams: [{ id: 'a', score: 1 }] });

    expect(reported(1)).toBeFalse();
  });

  it('notices an array entry that changed', () => {
    cadence.observe({ teams: [{ id: 'a', score: 1 }] });
    cadence.observe({ teams: [{ id: 'a', score: 2 }] });

    expect(reported(1)).toBeTrue();
  });

  it('treats a null snapshot as its own value', () => {
    cadence.observe(null);
    cadence.observe(null);
    cadence.observe({ status: 'ENDED' });

    expect(reported(1)).toBeFalse();
    expect(reported(2)).toBeTrue();
  });

  it('counts the observation after a reset as a change again', () => {
    cadence.observe({ status: 'RUNNING' });
    cadence.reset();
    cadence.observe({ status: 'RUNNING' });

    expect(reported(1)).toBeTrue();
  });

  it('returns the fingerprint it reported so callers can pass it along', () => {
    expect(cadence.observe({ a: 1, b: 2 })).toBe(PollCadence.fingerprint({ b: 2, a: 1 }));
  });

  it('keeps its label for debugging', () => {
    expect(cadence.name).toBe('state');
  });
});
