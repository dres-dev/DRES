import { fakeAsync, tick } from '@angular/core/testing';
import { BehaviorSubject, Subject } from 'rxjs';
import { ApiEvaluationState } from '../../../openapi';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';
import { viewerRefreshes, viewerRefreshEvents, VIEWER_SCORE_UPDATE_TYPES } from './viewer-refresh';

describe('viewer refresh streams', () => {
  it('coalesces event types across evaluations in one shared batch', fakeAsync(() => {
    const messages = new Subject<IWsServerMessage>();
    const refreshEvents = viewerRefreshEvents(messages);
    const received: Array<{ evaluationId: string; types: ReadonlySet<ServerMessageType.ServerMessageTypeEnum> }> = [];
    const first = refreshEvents.subscribe((event) => received.push(event));
    const second = refreshEvents.subscribe((event) => received.push(event));

    messages.next({ evaluationId: 'eval-1', type: ServerMessageType.ServerMessageTypeEnum.TASK_START, timestamp: 1 });
    messages.next({ evaluationId: 'eval-1', type: ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED, timestamp: 2 });
    messages.next({ evaluationId: 'eval-2', type: ServerMessageType.ServerMessageTypeEnum.COMPETITION_END, timestamp: 3 });
    messages.next({ evaluationId: 'eval-1', type: ServerMessageType.ServerMessageTypeEnum.PING, timestamp: 4 });
    tick(200);

    expect(received.length).toBe(4);
    expect(received[0].evaluationId).toBe('eval-1');
    expect([...received[0].types]).toEqual([
      ServerMessageType.ServerMessageTypeEnum.TASK_START,
      ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
    ]);
    expect(received[1]).toBe(received[0]);
    expect(received[2].evaluationId).toBe('eval-2');
    expect([...received[2].types]).toEqual([ServerMessageType.ServerMessageTypeEnum.COMPETITION_END]);
    expect(received[3]).toBe(received[2]);

    first.unsubscribe();
    second.unsubscribe();
  }));

  it('refreshes matching widgets once for a coalesced relevant batch', fakeAsync(() => {
    const state = new BehaviorSubject<ApiEvaluationState>({ evaluationId: 'eval-1' } as ApiEvaluationState);
    const messages = new Subject<IWsServerMessage>();
    const refreshEvents = viewerRefreshEvents(messages);
    const refreshIds: string[] = [];
    const subscription = viewerRefreshes(state, refreshEvents, new Subject<void>(), VIEWER_SCORE_UPDATE_TYPES).subscribe((id) => refreshIds.push(id));

    expect(refreshIds).toEqual(['eval-1']);
    messages.next({ evaluationId: 'eval-1', type: ServerMessageType.ServerMessageTypeEnum.TASK_START, timestamp: 1 });
    messages.next({ evaluationId: 'eval-1', type: ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE, timestamp: 2 });
    tick(200);

    expect(refreshIds).toEqual(['eval-1', 'eval-1']);
    subscription.unsubscribe();
  }));

  it('does not refresh for unrelated evaluation or event types', fakeAsync(() => {
    const state = new BehaviorSubject<ApiEvaluationState>({ evaluationId: 'eval-1' } as ApiEvaluationState);
    const messages = new Subject<IWsServerMessage>();
    const refreshEvents = viewerRefreshEvents(messages);
    const refreshIds: string[] = [];
    const subscription = viewerRefreshes(state, refreshEvents, new Subject<void>(), VIEWER_SCORE_UPDATE_TYPES).subscribe((id) => refreshIds.push(id));

    messages.next({ evaluationId: 'eval-2', type: ServerMessageType.ServerMessageTypeEnum.TASK_START, timestamp: 1 });
    messages.next({ evaluationId: 'eval-1', type: ServerMessageType.ServerMessageTypeEnum.PING, timestamp: 2 });
    tick(200);

    expect(refreshIds).toEqual(['eval-1']);
    subscription.unsubscribe();
  }));
});
