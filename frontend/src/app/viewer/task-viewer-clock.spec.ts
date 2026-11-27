import { fakeAsync, tick } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { ApiEvaluationState } from '../../../openapi';
import { formatTimeLeft, taskClock } from './task-viewer.component';

function state(overrides: Partial<ApiEvaluationState> = {}): ApiEvaluationState {
  return {
    evaluationId: 'eval-1',
    evaluationStatus: 'RUNNING',
    taskStatus: 'RUNNING',
    timeElapsed: 10,
    timeLeft: 30,
    ...overrides,
  } as ApiEvaluationState;
}

describe('taskClock', () => {
  it('advances elapsed time and decreases remaining time between server snapshots', fakeAsync(() => {
    const source = new Subject<ApiEvaluationState>();
    const values: ApiEvaluationState[] = [];
    let now = 0;
    const subscription = taskClock(source, () => now).subscribe((value) => values.push(value));

    source.next(state());
    tick(0);
    now = 1000;
    tick(1000);
    now = 2000;
    tick(1000);

    expect(values.map((value) => [value.timeElapsed, value.timeLeft])).toEqual([
      [10, 30],
      [11, 29],
      [12, 28],
    ]);
    subscription.unsubscribe();
  }));

  it('recomputes elapsed time from monotonic time after a delayed timer callback', fakeAsync(() => {
    const source = new Subject<ApiEvaluationState>();
    const values: ApiEvaluationState[] = [];
    let now = 0;
    const subscription = taskClock(source, () => now).subscribe((value) => values.push(value));

    source.next(state());
    now = 3500;
    tick(1000);

    expect(values.slice(-1).map((value) => [value.timeElapsed, value.timeLeft])).toEqual([[13, 27]]);
    subscription.unsubscribe();
  }));

  it('resynchronizes from a new server snapshot and clamps remaining time at zero', fakeAsync(() => {
    const source = new Subject<ApiEvaluationState>();
    const values: ApiEvaluationState[] = [];
    let now = 0;
    const subscription = taskClock(source, () => now).subscribe((value) => values.push(value));

    source.next(state());
    tick(0);
    now = 2000;
    tick(2000);
    now = 2500;
    source.next(state({ timeElapsed: 50, timeLeft: 1 }));
    tick(0);
    now = 3500;
    tick(1000);

    expect(values.slice(-2).map((value) => [value.timeElapsed, value.timeLeft])).toEqual([
      [50, 1],
      [51, 0],
    ]);
    subscription.unsubscribe();
  }));

  it('does not advance non-running task states or invent a deadline for perpetual tasks', fakeAsync(() => {
    const source = new Subject<ApiEvaluationState>();
    const values: ApiEvaluationState[] = [];
    let now = 0;
    const subscription = taskClock(source, () => now).subscribe((value) => values.push(value));

    source.next(state({ timeLeft: null }));
    tick(0);
    now = 1000;
    tick(1000);
    now = 2000;
    tick(1000);
    source.next(state({ taskStatus: 'ENDED', timeElapsed: 42, timeLeft: -1 }));
    tick(2000);

    expect(values.map((value) => [value.taskStatus, value.timeElapsed, value.timeLeft])).toEqual([
      ['RUNNING', 10, null],
      ['RUNNING', 11, null],
      ['RUNNING', 12, null],
      ['ENDED', 42, -1],
    ]);
    subscription.unsubscribe();
  }));
});

describe('formatTimeLeft', () => {
  it('renders ∞ only for a perpetual task and never for an elapsed deadline', () => {
    expect(formatTimeLeft(null)).toBe('∞');
    expect(formatTimeLeft(undefined)).toBe('∞');
    expect(formatTimeLeft(0)).toBe('00:00');
    expect(formatTimeLeft(59)).toBe('00:59');
    expect(formatTimeLeft(60)).toBe('01:00');
    expect(formatTimeLeft(3661)).toBe('01:01:01');
  });

  it('clamps negative remaining time to zero instead of reporting an infinite countdown', () => {
    expect(formatTimeLeft(-1)).toBe('00:00');
  });
});
