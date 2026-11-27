import { from, merge, Observable } from 'rxjs';
import { bufferTime, distinctUntilChanged, filter, map, mergeMap, share, shareReplay, switchMap, withLatestFrom } from 'rxjs/operators';
import { ApiEvaluationState } from '../../../openapi';
import { ServerMessageType } from '../model/ws/server-message-type.enum';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';

/**
 * Emits the evaluation ID when a viewer widget should refresh its REST-backed data.
 * Initial state and navigation to a different evaluation refresh immediately; a single
 * coalesced event stream is shared across widgets to avoid redundant refresh scheduling.
 */
export type ViewerRefreshEvent = { evaluationId: string; types: ReadonlySet<ServerMessageType.ServerMessageTypeEnum> };

export function viewerRefreshEvents(messages: Observable<IWsServerMessage>): Observable<ViewerRefreshEvent> {
  return messages.pipe(
    filter((message) => VIEWER_ALL_UPDATE_TYPES.includes(message.type)),
    bufferTime(200),
    filter((batch) => batch.length > 0),
    mergeMap((batch) => {
      const typesByEvaluation = new Map<string, Set<ServerMessageType.ServerMessageTypeEnum>>();
      batch.forEach((message) => {
        const types = typesByEvaluation.get(message.evaluationId) ?? new Set<ServerMessageType.ServerMessageTypeEnum>();
        types.add(message.type);
        typesByEvaluation.set(message.evaluationId, types);
      });
      return from([...typesByEvaluation].map(([evaluationId, types]) => ({ evaluationId, types })));
    }),
    share()
  );
}

const viewerDataByEvents = new WeakMap<Observable<ViewerRefreshEvent>, Map<string, Observable<unknown>>>();

/** Shares identical REST-backed resources between widgets in the same viewer page. */
export function viewerData<T>(
  state: Observable<ApiEvaluationState>,
  refreshEvents: Observable<ViewerRefreshEvent>,
  reconnected: Observable<void>,
  eventTypes: readonly ServerMessageType.ServerMessageTypeEnum[],
  resourceKey: string,
  fetch: (evaluationId: string) => Observable<T>
): Observable<T> {
  let resources = viewerDataByEvents.get(refreshEvents);
  if (!resources) {
    resources = new Map<string, Observable<unknown>>();
    viewerDataByEvents.set(refreshEvents, resources);
  }

  const existing = resources.get(resourceKey);
  if (existing) return existing as Observable<T>;

  const resource = viewerRefreshes(state, refreshEvents, reconnected, eventTypes).pipe(
    switchMap((evaluationId) => fetch(evaluationId)),
    shareReplay({ bufferSize: 1, refCount: true })
  );
  resources.set(resourceKey, resource);
  return resource;
}

export function viewerRefreshes(
  state: Observable<ApiEvaluationState>,
  refreshEvents: Observable<ViewerRefreshEvent>,
  reconnected: Observable<void>,
  eventTypes: readonly ServerMessageType.ServerMessageTypeEnum[]
): Observable<string> {
  const evaluationId = state.pipe(
    filter((snapshot) => !!snapshot?.evaluationId),
    map((snapshot) => snapshot.evaluationId),
    distinctUntilChanged(),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  const eventRefreshes = refreshEvents.pipe(
    withLatestFrom(evaluationId),
    filter(([event, currentId]) => event.evaluationId === currentId && [...event.types].some((type) => eventTypes.includes(type))),
    map(([, currentId]) => currentId)
  );

  const reconnectRefreshes = reconnected.pipe(withLatestFrom(evaluationId), map(([, currentId]) => currentId));
  const stateRefreshes = evaluationId.pipe(map((currentId) => currentId));

  return merge(stateRefreshes, eventRefreshes, reconnectRefreshes);
}

export const VIEWER_ALL_UPDATE_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
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
