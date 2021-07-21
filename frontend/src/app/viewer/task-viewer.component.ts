import {AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunService, ContentElement, RunState, TaskInfo, TaskTarget} from '../../../openapi';
import {BehaviorSubject, combineLatest, interval, merge, Observable, of, Subscription, timer} from 'rxjs';
import {
    catchError,
    concatMap,
    delayWhen,
    filter,
    finalize,
    flatMap,
    map,
    repeat,
    sampleTime,
    share,
    shareReplay,
    switchMap,
    take,
    tap,
    withLatestFrom
} from 'rxjs/operators';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {WebSocketSubject} from 'rxjs/webSocket';
import {AppConfig} from '../app.config';
import {AudioPlayerUtilities} from '../utilities/audio-player.utilities';
import {fromArray} from 'rxjs/internal/observable/fromArray';

/**
 * Internal enumeration used for TaskViewerComponent.
 */
enum ViewerState {
    VIEWER_UNKNOWN = 0,
    VIEWER_WAITING_FOR_TASK,
    VIEWER_TASK_ENDED,
    VIEWER_SYNC,
    VIEWER_COUNTDOWN,
    VIEWER_PLAYBACK
}

@Component({
    selector: 'app-task-viewer',
    templateUrl: './task-viewer.component.html',
    styleUrls: ['./task-viewer.component.scss']
})
export class TaskViewerComponent implements AfterViewInit, OnDestroy {
    @Input() runId: Observable<string>;
    @Input() state: Observable<RunState>;
    @Input() taskStarted: Observable<TaskInfo>;
    @Input() taskChanged: Observable<TaskInfo>;
    @Input() taskEnded: Observable<TaskInfo>;
    @Input() webSocketSubject: WebSocketSubject<IWsMessage>;

    /** Time that is still left (only when a task is running). */
    timeLeft: Observable<number>;

    /** Time that has elapsed (only when a task is running). */
    timeElapsed: Observable<number>;

    /** Observable that fires when all viewers have confirmed, that they are ready. */
    taskReady: Observable<boolean>;

    /** A {@link BehaviorSubject} of task countdown objects. */
    taskCountdown: BehaviorSubject<Observable<number>> = new BehaviorSubject(null);

    /** The current {@link ViewerState} of this {@link TaskViewerComponent}. */
    viewerState: BehaviorSubject<ViewerState> = new BehaviorSubject(ViewerState.VIEWER_UNKNOWN);

    /** Reference to the current {@link TaskTarget} {@link ContentElement}. */
    currentTaskHint: Observable<ContentElement>;

    /** Reference to the current {@link TaskTarget} {@link ContentElement}. */
    currentTaskTarget: Observable<ContentElement>;

    /** */
    viewerStateSubscription: Subscription;

    /** Reference to the audio element used during countdown. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    constructor(protected runService: CompetitionRunService, public config: AppConfig) {}

    /**
     * Create a subscription for task changes.
     */
    ngAfterViewInit(): void {

        /*  Observable for the current query hint. */
        const currentTaskHint = this.taskChanged.pipe(
            flatMap(task => this.runId),
            switchMap(id => this.runService.getApiRunWithRunidHint(id).pipe(
                catchError(e => {
                    console.error('[TaskViewerComponent] Could not load current query object due to an error.', e);
                    return of(null);
                })
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /*  Observable for the current query target. */
        const currentTaskTarget = this.taskEnded.pipe(
            flatMap(s => this.runId),
            switchMap(id => this.runService.getApiRunWithRunidTarget(id).pipe(
                catchError(e => {
                    console.error('[TaskViewerComponent] Could not load current query object due to an error.', e);
                    return of(null);
                })
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /*
         * This is the main switch that updates the viewer's state and the only actual subscription.
         *
         * IMPORTANT: Unsubscribe onDestroy.
         */
        this.viewerStateSubscription = combineLatest([currentTaskHint, this.state]).subscribe(([h, s]) => {
            switch (s.status) {
                case 'CREATED':
                case 'ACTIVE':
                    this.viewerState.next(ViewerState.VIEWER_WAITING_FOR_TASK);
                    break;
                case 'PREPARING_TASK':
                    this.viewerState.next(ViewerState.VIEWER_SYNC);
                    if (h != null) { this.webSocketSubject.next({runId: s.id, type: 'ACK'} as IWsClientMessage); } /* Send ACK. */
                    break;
                case 'RUNNING_TASK':
                    const countdown = (s.timeLeft - (s.currentTask.duration - 5));
                    if (countdown > 0) {
                        this.viewerState.next(ViewerState.VIEWER_COUNTDOWN);
                        this.taskCountdown.next(timer(0, 1000).pipe(
                            take(countdown),
                            withLatestFrom(this.runId),
                            map(([count, id]) => {
                                if (count < (countdown - 1)) {
                                    AudioPlayerUtilities.playOnce('assets/audio/beep_1.ogg', this.audio.nativeElement);
                                } else {
                                    AudioPlayerUtilities.playOnce('assets/audio/beep_2.ogg', this.audio.nativeElement);
                                }
                                return countdown - count - 1;
                            }),
                            finalize(() => {
                                this.viewerState.next(ViewerState.VIEWER_PLAYBACK);
                                this.taskCountdown.next(null);
                            })
                        ));
                    } else {
                        this.viewerState.next(ViewerState.VIEWER_PLAYBACK);
                    }
                    break;
                case 'TASK_ENDED':
                case 'TERMINATED':
                    return this.viewerState.next(ViewerState.VIEWER_TASK_ENDED);
            }
        });

        /** Map task target to representation used by viewer. */
        this.currentTaskTarget = currentTaskTarget.pipe(
            flatMap((h: TaskTarget) => {
            if (!h) { return of(null); }
            return fromArray(h.sequence).pipe(
                delayWhen<ContentElement>((c: ContentElement) => interval(1000 * c.offset)),
                repeat(-1),
            );
        }));

        /* Map task hint to representation used by viewer. */
        this.currentTaskHint = currentTaskHint.pipe(
            concatMap((h, i) => {
                return this.timeElapsed.pipe(
                    take(1),
                    flatMap(time => {
                        const sequence = [];
                        const largest = new Map<ContentElement.ContentTypeEnum, ContentElement>();

                        if (!h) { return null; }

                        /* Find last element per category (which is always retained). */
                        h.sequence.forEach(e => {
                            if (e.offset - time < 0) {
                                if (!largest.has(e.contentType)) {
                                    largest.set(e.contentType, e);
                                    sequence.push(e);
                                } else if (largest.get(e.contentType).offset < e.offset) {
                                    sequence.splice(sequence.indexOf(largest.get(e.contentType)));
                                    largest.set(e.contentType, e);
                                    sequence.push(e);
                                }
                            } else {
                                sequence.push(e);
                            }
                        });

                        /* Filter out all element in the sequence that are not eligible for display*/
                        return fromArray(sequence).pipe(
                            delayWhen<any>(c => interval(Math.max(0, 1000 * (c.offset - time)))),
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
        );

        /*
         * This Observable is used to poll the RunState; it merges the normal state observable with a timer and
         * makes sure that:
         *
         * - If normal state changes (e.g. due to an external event), that state object is re-used
         * - If timer fire, the state is queried.
         * - Both timer + normal state only trigger an update every 500ms.
         *
         * Implicitly, this Observable is only used when a task is running due to how it is used in the template!
         */
        const polledState = merge(interval(1000).pipe(flatMap(() => this.runId)), this.state).pipe(
            sampleTime(1000), /* This is again sampled to only ever emit once every second. */
            switchMap(s => {
                if (typeof s === 'string') {
                    return this.runService.getApiRunStateWithRunid(s); /* Timer! Load run state! */
                } else {
                    return of(s as RunState); /* This is a freshly loaded run state. */
                }
            }),
            catchError((err, o) => {
                console.log(`[TaskViewerComponent] Error occurred while polling state: ${err?.message}`);
                return of(null);
            }),
            filter(p => p != null),
            share()
        );

        /* Observable for the time that is still left. */
        this.timeLeft = polledState.pipe(
            map(s => s.timeLeft),
            tap(t => {
                if (t === 30 || t === 60) {
                    AudioPlayerUtilities.playOnce('assets/audio/glass.ogg', this.audio.nativeElement); /* Reminder that time is running out. */
                }
            })
        );

        /* Observable for the time that has elapsed. */
        this.timeElapsed = polledState.pipe(map(s => s.currentTask?.duration - s.timeLeft));
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
        const hours   = Math.floor(sec / 3600);
        const minutes = Math.floor(sec / 60) % 60;
        const seconds = sec % 60;

        return [hours, minutes, seconds]
            .map(v => v < 10 ? '0' + v : v)
            .filter((v, i) => v !== '00' || i > 0)
            .join(':');
    }
}
