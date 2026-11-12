import { merge, Observable } from 'rxjs';
import { auditTime, distinctUntilChanged, filter, map, shareReplay, withLatestFrom } from 'rxjs/operators';
import { ApiEvaluationState } from '../../../openapi';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

/**
 * Emits the evaluation ID when a viewer widget should refresh its REST-backed data.
 * Initial state and navigation to a different evaluation refresh immediately; matching
 * WebSocket events are coalesced to avoid a burst of redundant requests.
 */
export function viewerRefreshes(
  state: Observable<ApiEvaluationState>,
  messages: Observable<IWsServerMessage>,
  reconnected: Observable<void>,
  eventTypes: ServerMessageType.ServerMessageTypeEnum[]
): Observable<string> {
  const evaluationId = state.pipe(
    filter((snapshot) => !!snapshot?.evaluationId),
    map((snapshot) => snapshot.evaluationId),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  const eventRefreshes = messages.pipe(
    filter((message) => eventTypes.includes(message.type)),
    withLatestFrom(evaluationId),
    filter(([message, currentId]) => message.evaluationId === currentId),
    map(([, currentId]) => currentId),
    auditTime(200)
  );

  const reconnectRefreshes = reconnected.pipe(withLatestFrom(evaluationId), map(([, currentId]) => currentId));
  const stateRefreshes = evaluationId.pipe(map((currentId) => currentId));

  return merge(stateRefreshes, eventRefreshes, reconnectRefreshes);
}

export const VIEWER_DATA_UPDATE_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
  ServerMessageType.ServerMessageTypeEnum.TASK_PREPARE,
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_START,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
];

export const VIEWER_SCORE_UPDATE_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_START,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
];

export const VIEWER_SUBMISSION_UPDATE_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_START,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
];
