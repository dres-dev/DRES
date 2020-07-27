import {AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {
    CompetitionRunService, RestTaskDescription,
    RunState
} from '../../../openapi';
import {combineLatest, interval, Observable, of, timer, zip} from 'rxjs';
import {catchError, filter, flatMap, map, share, shareReplay, switchMap, take, tap, withLatestFrom} from 'rxjs/operators';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {WebSocketSubject} from 'rxjs/webSocket';
import {AppConfig} from '../app.config';
import {AudioPlayerUtilities} from '../utilities/audio-player.utilities';

@Component({
    selector: 'app-task-viewer',
    templateUrl: './task-viewer.component.html',
    styleUrls: ['./task-viewer.component.scss']
})
export class TaskViewerComponent implements AfterViewInit, OnDestroy {
    @Input() runId: Observable<string>;
    @Input() state: Observable<RunState>;
    @Input() taskStarted: Observable<RestTaskDescription>;
    @Input() taskChanged: Observable<RestTaskDescription>;
    @Input() taskEnded: Observable<RestTaskDescription>;
    @Input() webSocket: Observable<IWsMessage>;
    @Input() webSocketSubject: WebSocketSubject<IWsMessage>;

    /** Time that is still left (only when a task is running). */
    timeLeft: Observable<number>;

    /** Time that has elapsed (only when a task is running). */
    timeElapsed: Observable<number>;

    /** Observable that returns and caches the current query object. */
    currentQueryObject: Observable<any> // <VideoQueryDescription | TextQueryDescription | ImageQueryDescription>;

    /** Value of the task count down. */
    taskCountdown: Observable<number>;

    /** Subscription */
    preparingTask: Observable<boolean>;

    /** Reference to the audio element used during countdown. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    constructor(protected runService: CompetitionRunService, public config: AppConfig) {}

    /**
     * Create a subscription for task changes.
     */
    ngAfterViewInit(): void {
        /* Subscription for the current query object. */
        this.currentQueryObject = this.taskChanged.pipe(
            flatMap(task => this.runId),
            switchMap(id => this.runService.getApiRunWithRunidQuery(id).pipe(
                catchError(e => {
                    console.error('[TaskViewerComponent] Could not load current query object due to an error.', e);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay({bufferSize: 1, refCount: true})
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

        /* Timer observables */
        this.timeLeft = polledState.pipe(
            map(s => s.timeLeft),
            tap(t => {
                if (t === 30 || t === 60) {
                    AudioPlayerUtilities.playOnce('assets/audio/glass.ogg', this.audio.nativeElement);
                }
            })
        );
        this.timeElapsed = polledState.pipe(map(s => s.currentTask?.duration - s.timeLeft));

        /* Observable reacting to TASK_PREPARE message. */
        this.preparingTask = combineLatest([
            zip(this.webSocket.pipe(filter(m => m.type === 'TASK_PREPARE')), this.currentQueryObject),
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
