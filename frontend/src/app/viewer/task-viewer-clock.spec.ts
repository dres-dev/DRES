import { fakeAsync, tick } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { ApiEvaluationState } from '../../../openapi';
import { taskClock } from './task-viewer.component';

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
    const subscription = taskClock(source).subscribe((value) => values.push(value));

    source.next(state());
    tick(2000);

    expect(values.map((value) => [value.timeElapsed, value.timeLeft])).toEqual([
      [10, 30],
      [11, 29],
      [12, 28],
    ]);
    subscription.unsubscribe();
  }));

  it('resynchronizes from a new server snapshot and clamps remaining time at zero', fakeAsync(() => {
    const source = new Subject<ApiEvaluationState>();
    const values: ApiEvaluationState[] = [];
    const subscription = taskClock(source).subscribe((value) => values.push(value));

    source.next(state());
    tick(2000);
    source.next(state({ timeElapsed: 50, timeLeft: 1 }));
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
    const subscription = taskClock(source).subscribe((value) => values.push(value));

    source.next(state({ timeLeft: null }));
    tick(2000);
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
