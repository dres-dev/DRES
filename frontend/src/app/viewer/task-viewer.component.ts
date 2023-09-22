import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import {BehaviorSubject, combineLatest, from, interval, mergeMap, Observable, of, Subscription} from 'rxjs';
import {
  catchError,
  delayWhen,
  map,
  repeat,
  shareReplay,
  switchMap,
  take,
  tap,
  withLatestFrom,
} from 'rxjs/operators';
import { AppConfig } from '../app.config';
import {
  ApiContentElement, ApiContentType, ApiEvaluationInfo,
  ApiEvaluationState,
  ApiHintContent,
  ApiTargetContent,
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
  @Input() evaluationId: Observable<string>;

  /** Observable for information about the current run. Usually queried once when the view is loaded. */
  @Input() info: Observable<ApiEvaluationInfo>;

  /** Observable for information about the current run's {@link ApiEvaluationState}. */
  @Input() state: Observable<ApiEvaluationState>;

  /** Observable that fires whenever a task starts. Emits the {@link ApiEvaluationState} that triggered the fire. */
  @Input() taskStarted: Observable<ApiEvaluationState>;

  /** Observable that fires whenever a task ends. Emits the {@link ApiEvaluationState} that triggered the fire. */
  @Input() taskEnded: Observable<ApiEvaluationState>;

  /** Observable that fires whenever the active task template changes. Emits the {@link ApiEvaluationState} that triggered the fire. */
  @Input() taskChanged: Observable<ApiEvaluationState>;

  /** Seconds duration remaining for early warning. Default is 60 */
  @Input() earlyWarningThreshold: number = 60;

  /** Seconds duration remaining for late warning. Default is 30 */
  @Input() lateWarningThreshold: number = 30;

  /** Observable that fires whenever the active task template changes. Emits the {@link ApiEvaluationState} that triggered the fire. */
  currentTaskName: Observable<string>;

  /** Time that is still left (only when a task is running). */
  timeLeft: Observable<number>;

  /** Time that has elapsed (only when a task is running). */
  timeElapsed: Observable<number>;

  /** A {@link BehaviorSubject} of task countdown objects. */
  taskCountdown: BehaviorSubject<number> = new BehaviorSubject(null);

  /** The current {@link ViewerState} of this {@link TaskViewerComponent}. */
  viewerState: BehaviorSubject<ViewerState> = new BehaviorSubject(ViewerState.VIEWER_UNKNOWN);

  /** Reference to the current {@link ApiHintContent}. */
  currentTaskHint: Observable<ApiContentElement>;

  /** Reference to the current {@link ApiTargetContent}. */
  currentTaskTarget: Observable<ApiContentElement>;

  /** The subscription associated with the current viewer state. */
  viewerStateSubscription: Subscription;

  /** Reference to the audio elements used during countdown. */
  @ViewChild('audio_beep_1') beep1: ElementRef<HTMLAudioElement>;
  @ViewChild('audio_beep_2') beep2: ElementRef<HTMLAudioElement>;
  @ViewChild('audio_ding') ding: ElementRef<HTMLAudioElement>;
  @ViewChild('audio_glass') glass: ElementRef<HTMLAudioElement>;

  constructor(protected runService: EvaluationService, public config: AppConfig) {}

  private playOnce(audio: HTMLAudioElement) {
    if (this.config.config.effects.mute) {
      return
    }
    audio
        .play()
        .catch((reason) => console.warn('Failed to play audio effects due to an error:', reason))
        .then(() => {});
  }

  /**
   * Create a subscription for task changes.
   */
  ngAfterViewInit(): void {
    /*  Observable for the current query hint. */
    const currentTaskHint = this.taskChanged.pipe(
      mergeMap((task) => {
          console.log("current task hint triggered", task)
          return this.runService.getApiV2EvaluationByEvaluationIdByTaskIdHint(task.evaluationId, task.taskId).pipe(
            catchError((e) => {
              console.error("[TaskViewerComponent] Could not load current query hint due to an error.", e);
              return of(null);
            })
          );
        }
      ),
      tap((hint) => this.evaluationId.pipe(switchMap(evaluationId => this.runService.getApiV2EvaluationByEvaluationIdByTaskIdReady(evaluationId, hint.taskId))).subscribe()),
      shareReplay({ bufferSize: 1, refCount: true })
    );


    /*  Observable for the current query target. */
    const currentTaskTarget = this.taskEnded.pipe(
      withLatestFrom(this.evaluationId),
      switchMap(([task, evaluationId]) =>
        this.runService.getApiV2EvaluationByEvaluationIdByTaskIdTarget(evaluationId, task.taskId).pipe(
          catchError((e) => {
            console.error('[TaskViewerComponent] Could not load current task target due to an error.', e);
            return of(null);
          })
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );


    /*
     * This is the main switch that updates the viewer's state and the only actual subscription.
     *
     * IMPORTANT: Unsubscribe onDestroy.
     */
    this.viewerStateSubscription = this.state.subscribe((s) => {
      // this.timeElapsed = of(s.timeElapsed)
      switch (s.taskStatus) {
        case 'NO_TASK':
        case 'CREATED':
          this.viewerState.next(ViewerState.VIEWER_WAITING_FOR_TASK);
          break;
        case 'PREPARING':
          this.viewerState.next(ViewerState.VIEWER_SYNC);
          break;
        case 'RUNNING':
          if (s.timeElapsed < 0) {
            const countdown = Math.abs(s.timeElapsed) - 1;
            this.viewerState.next(ViewerState.VIEWER_COUNTDOWN);
            this.taskCountdown.next(countdown);
            if (countdown > 0) {
              this.playOnce(this.beep1.nativeElement);
            } else {
              this.playOnce(this.beep2.nativeElement);
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
      mergeMap((h: ApiHintContent) => {
        if (!h) {
          return from([]);
        }
        return from(h.sequence).pipe(
          delayWhen<ApiContentElement>((c: ApiContentElement) => interval(1000 * c.offset)),
          repeat(1)
        );
      })
    );

    /* Map task hint to representation used by viewer. */
    this.currentTaskHint = currentTaskHint.pipe(
      mergeMap((hint) => {
        return this.timeElapsed.pipe(
            take(1),
            mergeMap((timeElapsed) => {
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

            return from(sequence).pipe(
              delayWhen<any>((c) => interval(Math.max(0, 1000 * (c.offset - actualTimeElapsed)))),
              map((t, index) => {
                if (index > 0) {
                  this.playOnce(this.ding.nativeElement);
                }
                return t;
              })
            );
          })
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Observable for the name of the current task. */
    this.currentTaskName = combineLatest([this.info, this.taskChanged]).pipe(
        map(([info, state]) => info.taskTemplates.find(t => t.templateId == state.taskTemplateId)?.name)
    )

    /* Observable for the time that is still left. */
    this.timeLeft = this.state.pipe(
      map((s) => s.timeLeft) /* Compensating for added countdown. */,
      tap((t) => {
        if (t === this.lateWarningThreshold || t === this.earlyWarningThreshold) {
          this.playOnce(this.glass.nativeElement); /* Reminder that time is running out. */
        }
      })
    );

    /* Observable for the time that has elapsed. */
    this.timeElapsed = this.state.pipe(map((s) => s.timeElapsed));
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
