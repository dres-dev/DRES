import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { BehaviorSubject, combineLatest, interval, merge, Observable, of, Subscription, timer } from 'rxjs';
import {
  catchError,
  delayWhen,
  filter,
  flatMap,
  map,
  repeat,
  sampleTime,
  share,
  shareReplay,
  switchMap,
  take,
  tap,
  withLatestFrom,
} from 'rxjs/operators';
import { IWsMessage } from '../model/ws/ws-message.interface';
import { IWsClientMessage } from '../model/ws/ws-client-message.interface';
import { WebSocketSubject } from 'rxjs/webSocket';
import { AppConfig } from '../app.config';
import { AudioPlayerUtilities } from '../utilities/audio-player.utilities';
import { fromArray } from 'rxjs/internal/observable/fromArray';
import {
  ApiContentElement, ApiContentType,
  ApiEvaluationState, ApiHint,
  ApiHintContent,
  ApiTarget,
  ApiTargetContent,
  ApiTaskStatus,
  EvaluationService
} from '../../../openapi';

/**
 * Internal enumeration used for TaskViewerComponent.
 */
enum ViewerState {
  VIEWER_UNKNOWN = 0,
  VIEWER_WAITING_FOR_TASK,
  VIEWER_SYNC,
  VIEWER_COUNTDOWN,
  VIEWER_PLAYBACK,
  VIEWER_TASK_ENDED
}

@Component({
  selector: 'app-task-viewer',
  templateUrl: './task-viewer.component.html',
  styleUrls: ['./task-viewer.component.scss'],
})
export class TaskViewerComponent implements AfterViewInit, OnDestroy {
  @Input() runId: Observable<string>;
  @Input() state: Observable<ApiEvaluationState>;
  @Input() taskStarted: Observable<ApiEvaluationState>;
  @Input() taskChanged: Observable<ApiEvaluationState>;
  @Input() taskEnded: Observable<ApiEvaluationState>;
  @Input() webSocketSubject: WebSocketSubject<IWsMessage>;

  /** Time that is still left (only when a task is running). */
  timeLeft: Observable<number>;

  /** Time that has elapsed (only when a task is running). */
  timeElapsed: Observable<number>;

  /** A {@link BehaviorSubject} of task countdown objects. */
  taskCountdown: BehaviorSubject<number> = new BehaviorSubject(null);

  /** The current {@link ViewerState} of this {@link TaskViewerComponent}. */
  viewerState: BehaviorSubject<ViewerState> = new BehaviorSubject(ViewerState.VIEWER_UNKNOWN);

  /** Reference to the current {@link TaskHint} {@link ContentElement}s. */
  currentTaskHint: Observable<ApiHintContent>;

  /** Reference to the current {@link TaskTarget} {@link ContentElement}. */
  currentTaskTarget: Observable<ApiTargetContent>;

  /** The subscription associated with the current viewer state. */
  viewerStateSubscription: Subscription;

  /** Reference to the audio element used during countdown. */
  @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

  constructor(protected runService: EvaluationService, public config: AppConfig) {}

  /**
   * Create a subscription for task changes.
   */
  ngAfterViewInit(): void {
    /*  Observable for the current query hint. */
    const currentTaskHint = this.taskChanged.pipe(
      withLatestFrom(this.runId),
      switchMap(([task, runId]) =>
        this.runService.getApiV2RunevaluationIdHinttaskId(runId, task.id).pipe(
          catchError((e) => {
            console.error('[TaskViewerComponent] Could not load current query hint due to an error.', e);
            return of(null);
          })
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /*  Observable for the current query target. */
    const currentTaskTarget = this.state.pipe(
      filter(s => s.taskRunStatus == ApiTaskStatus.ENDED),
      switchMap((s) =>
        this.runService.getApiV2RunevaluationIdHinttaskId(s.id, s.currentTask?.templateId).pipe(
          catchError((e) => {
            console.error('[TaskViewerComponent] Could not load current task target due to an error.', e);
            return of(null);
          })
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /*
     * This Observable is used to poll the RunState; it merges the normal state observable with a timer and makes sure that:
     * - If normal state changes (e.g. due to an external event), that state object is re-used
     * - If timer fire, the state is queried.
     * - Both timer + normal state only trigger an update every 500ms.
     *
     * Implicitly, this Observable is only used when a task is running due to how it is used in the template!
     */
    const polledState = merge(interval(1000).pipe(flatMap(() => this.runId)), this.state).pipe(
      sampleTime(1000) /* This is again sampled to only ever emit once every second. */,
      switchMap((s) => {
        if (typeof s === 'string') {
          return this.runService.getApiV2EvaluationevaluationIdState(s); /* Timer! Load run state! */
        } else {
          return of(s as ApiEvaluationState); /* This is a freshly loaded run state. */
        }
      }),
      catchError((err, o) => {
        console.log(`[TaskViewerComponent] Error occurred while polling state: ${err?.message}`);
        return of(null);
      }),
      filter((p) => p != null),
      share()
    );

    /*
     * This is the main switch that updates the viewer's state and the only actual subscription.
     *
     * IMPORTANT: Unsubscribe onDestroy.
     */
    this.viewerStateSubscription = combineLatest([currentTaskHint, polledState]).subscribe(([h, s]) => {
      switch (s.taskRunStatus) {
        case 'NO_TASK':
        case 'CREATED':
          this.viewerState.next(ViewerState.VIEWER_WAITING_FOR_TASK);
          break;
        case 'PREPARING':
          this.viewerState.next(ViewerState.VIEWER_SYNC);
          if (h != null) {
            this.webSocketSubject.next({ runId: s.id, type: 'ACK' } as IWsClientMessage);
          } /* Send ACK. */
          break;
        case 'RUNNING':
          if (s.timeElapsed < 0) {
            const countdown = Math.abs(s.timeElapsed) - 1;
            this.viewerState.next(ViewerState.VIEWER_COUNTDOWN);
            this.taskCountdown.next(countdown);
            if (countdown > 0) {
              AudioPlayerUtilities.playOnce('/immutable/assets/audio/beep_1.ogg', this.audio.nativeElement);
            } else {
              AudioPlayerUtilities.playOnce('/immutable/assets/audio/beep_2.ogg', this.audio.nativeElement);
            }
          } else {
            this.viewerState.next(ViewerState.VIEWER_PLAYBACK);
          }
          break;
        case 'ENDED':
            return this.viewerState.next(ViewerState.VIEWER_TASK_ENDED);
      }
    });

    /** Map task target to representation used by viewer. */
    this.currentTaskTarget = currentTaskTarget.pipe(
      flatMap((h: ApiHintContent) => {
        if (!h) {
          return fromArray([]);
        }
        return fromArray(h.sequence).pipe(
          delayWhen<ApiContentElement>((c: ApiContentElement) => interval(1000 * c.offset)),
          repeat(-1)
        );
      })
    );

    /* Map task hint to representation used by viewer. */
    this.currentTaskHint = currentTaskHint.pipe(
      flatMap((hint) => {
        console.log(`Current Task Hint fired`);
        return this.timeElapsed.pipe(
          take(1),
          flatMap((timeElapsed) => {
            const actualTimeElapsed = Math.max(timeElapsed, 0);
            const sequence = [];
            if (hint) {
              const largest = new Map<ApiContentType, ApiContentElement>();
              hint.sequence.forEach((c) => {
                if (c.offset >= actualTimeElapsed) {
                  sequence.push(c);
                } else if (!largest.has(c.contentType)) {
                  largest.set(c.contentType, c);
                  sequence.push(c);
                } else if (largest.get(c.contentType).offset < c.offset) {
                  sequence.splice(sequence.indexOf(largest.get(c.contentType)));
                  largest.set(c.contentType, c);
                  sequence.push(c);
                }
              });
            }

            return fromArray(sequence).pipe(
              delayWhen<any>((c) => interval(Math.max(0, 1000 * (c.offset - actualTimeElapsed)))),
              map((t, index) => {
                if (index > 0) {
                  AudioPlayerUtilities.playOnce('assets/audio/ding.ogg', this.audio.nativeElement);
                }
                return t;
              })
            );
          })
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Observable for the time that is still left. */
    this.timeLeft = polledState.pipe(
      map((s) => s.timeLeft) /* Compensating for added countdown. */,
      tap((t) => {
        if (t === 30 || t === 60) {
          AudioPlayerUtilities.playOnce(
            '/immutable/assets/audio/glass.ogg',
            this.audio.nativeElement
          ); /* Reminder that time is running out. */
        }
      })
    );

    /* Observable for the time that has elapsed. */
    this.timeElapsed = polledState.pipe(map((s) => s.timeElapsed));
  }

  /**
   * Cleanup all subscriptions.
   */
  ngOnDestroy(): void {
    this.viewerStateSubscription.unsubscribe(); /* IMPORTANT! */
    this.viewerStateSubscription = null;
  }

  /**
   * Formats a given number of seconds into a time format hh:mm:ss.
   *
   * @param sec The number of seconds to convert.
   */
  public toFormattedTime(sec: number): string {
    const hours = Math.floor(sec / 3600);
    const minutes = Math.floor(sec / 60) % 60;
    const seconds = sec % 60;

    return [hours, minutes, seconds]
      .map((v) => (v < 10 ? '0' + v : v))
      .filter((v, i) => v !== '00' || i > 0)
      .join(':');
  }
}
