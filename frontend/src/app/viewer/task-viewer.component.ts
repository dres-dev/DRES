import {AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunService, ContentElement, RunState, TaskHint, TaskInfo, TaskTarget} from '../../../openapi';
import {BehaviorSubject, combineLatest, interval, Observable, of, Subscription, timer, zip} from 'rxjs';
import {
    catchError,
    concatMap,
    delayWhen,
    filter,
    flatMap,
    map,
    repeat,
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
    @Input() webSocket: Observable<IWsMessage>;
    @Input() webSocketSubject: WebSocketSubject<IWsMessage>;

    /** Time that is still left (only when a task is running). */
    timeLeft: Observable<number>;

    /** Time that has elapsed (only when a task is running). */
    timeElapsed: Observable<number>;

    /** Observable that returns  the current {@link ContentElement} based on {@link QueryHint} and time that has ellapsed. */
    currentQueryContentElement: Observable<ContentElement>;

    /** Observable that fires everytime a TASK_PREPARE message is received. */
    preparingTask: Observable<boolean>;

    /** Value of the task count down. */
    taskCountdown: Observable<number>;

    /** Reference to the audio element used during countdown. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    /** Reference to the current {@link TaskHint}. */
    currentTaskHint = new BehaviorSubject<TaskHint>(null);

    /** Reference to the  {@link TaskTarget}. */
    currentTaskTarget: Observable<ContentElement>;

    /** Subscription for the current {@link TaskHint}. */
    currentTaskHintSubscription: Subscription;

    constructor(protected runService: CompetitionRunService, public config: AppConfig) {}

    /**
     * Create a subscription for task changes.
     */
    ngAfterViewInit(): void {

        /*
        * Subscription for the current query object; required because loading of TaskHint should take place irrespective of whether
        * that TaskHint is currently being displayed.
        *
        * IMPORTANT: Unsubscribe in onDestroy!
        */
        this.currentTaskHintSubscription = this.taskChanged.pipe(
            flatMap(task => this.runId),
            tap(s => this.currentTaskHint.next(null)),
            switchMap(id => this.runService.getApiRunWithRunidHint(id).pipe(
                catchError(e => {
                    console.error('[TaskViewerComponent] Could not load current query object due to an error.', e);
                    return of(null);
                }),
                filter(h => h != null)
            ))
        ).subscribe(h => {
            this.currentTaskHint.next(h);
        });

        /* Observable for the current task target (loaded on demand). */
        this.currentTaskTarget = this.taskEnded.pipe(
            flatMap(s => this.runId),
            switchMap(id => this.runService.getApiRunWithRunidTarget(id).pipe(
                catchError(e => {
                    console.error('[TaskViewerComponent] Could not load current query object due to an error.', e);
                    return of(null);
                }),
                shareReplay({bufferSize: 1, refCount: true})
            )),
            flatMap((h: TaskTarget) => {
                if (!h) {
                    return [];
                }
                return fromArray(h.sequence).pipe(
                    delayWhen<any>((c: ContentElement) => interval(1000 * c.offset)),
                    repeat(-1),
                );
            })
        );

        /* Observable for current query component. */
        this.currentQueryContentElement = this.currentTaskHint.pipe(
            concatMap((hint, i) => {
                return this.timeElapsed.pipe(
                    take(1),
                    flatMap(time => {
                        const sequence = [];
                        const largest = new Map<ContentElement.ContentTypeEnum, ContentElement>();

                        if (!hint) {
                            return sequence;
                        }

                        /* Find last element per category (which is always retained). */
                        hint.sequence.forEach(e => {
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
                            delayWhen<any>(c => interval(1000 * Math.max(0, (c.offset - time)))),
                            map((t, index) => {
                                if (index > 0) {
                                    AudioPlayerUtilities.playOnce('assets/audio/ding.ogg', this.audio.nativeElement);
                                }
                                return t;
                            })
                        );
                    })
                );
            })
        );

        /* Observable for the time left and time elapsed (for running tasks only). */
        const polledState = this.state.pipe(
            filter(s => s.status === 'RUNNING_TASK'),
            switchMap(s => interval(1000).pipe(
                switchMap(t => this.runService.getApiRunStateWithRunid(s.id)),
                catchError((err, o) => {
                    console.log(`[TaskViewerComponent] Error occurred while polling state: ${err?.message}`);
                    return of(null);
                }),
                filter(p => p != null)
            )),
            share()
        );

        /* Observable for the time that is still left. */
        this.timeLeft = polledState.pipe(
            map(s => s.timeLeft),
            tap(t => {
                if (t === 30 || t === 60) {
                    AudioPlayerUtilities.playOnce('assets/audio/glass.ogg', this.audio.nativeElement);
                }
            })
        );

        /* Observable for the time that has ellapsed. */
        this.timeElapsed = polledState.pipe(map(s => s.currentTask?.duration - s.timeLeft));

        /* Observable reacting to TASK_PREPARE message. */
        this.preparingTask = combineLatest([
            zip(this.webSocket.pipe(filter(m => m.type === 'TASK_PREPARE')), this.currentTaskHint.pipe(filter(h => h != null))),
            this.state
        ]).pipe(
            map(([m, s]) => {
                return s.status === 'PREPARING_TASK';
            })
        );

        /* Observable for task countdown */
        this.taskCountdown = timer(0, 1000).pipe(
            take(6),
            withLatestFrom(this.runId),
            map(([count, id]) => {
                if (count < 5) {
                    AudioPlayerUtilities.playOnce('assets/audio/beep_1.ogg', this.audio.nativeElement);
                } else {
                    AudioPlayerUtilities.playOnce('assets/audio/beep_2.ogg', this.audio.nativeElement);
                    this.webSocketSubject.next({runId: id, type: 'ACK'} as IWsClientMessage);
                }
                return 5 - count;
            })
        );
    }

    /**
     * Cleanup all subscriptions.
     */
    ngOnDestroy(): void {
        this.currentTaskHintSubscription.unsubscribe(); /* IMPORTANT! */
        this.currentTaskHintSubscription = null;
    }

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
