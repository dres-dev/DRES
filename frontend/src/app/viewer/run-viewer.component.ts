import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {interval, merge, Observable, of, Subscription} from 'rxjs';
import {
    catchError,
    delay,
    filter,
    flatMap,
    map,
    pairwise,
    retry,
    retryWhen,
    share,
    shareReplay,
    switchMap,
    tap,
    withLatestFrom
} from 'rxjs/operators';
import {webSocket, WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/webSocket';
import {AppConfig} from '../app.config';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {CompetitionRunService, RunInfo, RunState, TaskDescription} from '../../../openapi';
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
    selector: 'app-run-viewer',
    templateUrl: './run-viewer.component.html',
    styleUrls: ['./run-viewer.component.scss']
})
export class RunViewerComponent implements OnInit, OnDestroy  {
    /** The WebSocketSubject that represent the WebSocket connection to the DRES endpoint. */
    webSocketSubject: WebSocketSubject<IWsMessage>;

    /** Observable for incoming WebSocket messages. */
    webSocket: Observable<IWsServerMessage>;

    /** Observable for current run ID. */
    runId: Observable<number>;

    /** Observable for information about the current run. Usually queried once when the view is loaded. */
    runInfo: Observable<RunInfo>;

    /** Observable for information about the current run's state. Usually queried when a state change is signaled via WebSocket. */
    runState: Observable<RunState>;

    /** Observable that fires whenever a task starts. Emits the task description of the task that just started. */
    taskStarted: Observable<TaskDescription>;

    /** Observable that fires whenever a task changes. Emits the task description of the new task. */
    taskChanged: Observable<TaskDescription>;

    /** */
    currentQueryObject: Observable<any>

    /** Observable that fires whenever a task ends. Emits the task description of the task that just ended. */
    taskEnded: Observable<TaskDescription>;

    /** Internal WebSocket subscription for pinging the server. */
    private pingSubscription: Subscription;

    /**
     * Constructor; extracts the runId and keeps a local reference.
     *
     * @param activeRoute
     * @param config
     * @param runService
     * @param snackBar
     */
    constructor(private activeRoute: ActivatedRoute,
                private config: AppConfig,
                private runService: CompetitionRunService,
                private snackBar: MatSnackBar) {

        /** Initialize basic WebSocketSubject. */
        const wsurl = this.config.webSocketUrl;
        this.webSocketSubject = webSocket({
            url: wsurl,
            openObserver: {
                next(openEvent) {
                    console.log(`[RunViewerComponent] WebSocket connection to ${wsurl} established!`);
                }
            },
            closeObserver: {
                next(closeEvent) {
                    console.log(`[RunViewerComponent] WebSocket connection to ${wsurl} closed!`);
                }
            }
        } as WebSocketSubjectConfig<IWsMessage>);

        /** Observable for the current run Id. */
        this.runId = this.activeRoute.params.pipe(
            map(a => a.runId)
        );

        /* Basic observable for general run info; this information is static and does not change over the course of a run. */
        this.runInfo = this.runId.pipe(
            switchMap(runId => this.runService.getApiRunInfoWithRunid(runId).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[RunViewerComponent] There was an error while loading information in the current run: ${err?.message}`);
                    this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /* Basic observable for web socket messages received from the DRES server. */
        this.webSocket = this.runId.pipe(
            flatMap(runId => this.webSocketSubject.multiplex(
                () => {
                    return {runId, type: 'REGISTER'} as IWsClientMessage;
                },
                () => {
                    return {runId, type: 'UNREGISTER'} as IWsClientMessage;
                },
                message => true
            ).pipe(
                retryWhen((err) => err.pipe(
                    tap(e => console.error('[RunViewerComponent] An error occurred with the WebSocket communication channel. Trying to reconnect in 1 second.', e)),
                    delay(1000)
                )),
                map(m => m as IWsServerMessage),
                filter(q => q != null),
                tap(m => console.log(`[RunViewerComponent] WebSocket message received: ${m.type}`))
            )),
            share()
        );

        /* Basic observable for run state info; this information is dynamic and is subject to change over the course of a run. */
        this.runState = merge(this.runId, this.webSocket.pipe(filter(m => m.type !== 'PING'), map(m => m.runId))).pipe(
            switchMap((runId) => this.runService.getApiRunStateWithRunid(runId).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[RunViewerComponent] There was an error while loading information in the current run state: ${err?.message}`);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /* Basic observable that fires when a task starts.  */
        this.taskStarted = this.runState.pipe(
            pairwise(),
            filter(([s1, s2]) => s1.status === 'PREPARING_TASK' && s2.status === 'RUNNING_TASK'),
            map(([s1, s2]) => s2.currentTask),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /* Basic observable that fires when a task ends.  */
        this.taskEnded = this.runState.pipe(
            pairwise(),
            filter(([s1, s2]) => s1.status === 'RUNNING_TASK' && s2.status === 'ACTIVE'),
            map(([s1, s2]) => s2.currentTask),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /* Observable that tracks the currently active task. */
        this.taskChanged = merge(of(null as RunState), this.runState).pipe(
            pairwise(),
            filter(([s1, s2]) => s1 === null || (s1.currentTask.name !== s2.currentTask.name)),
            map(([s1, s2]) => s2.currentTask),
            shareReplay({bufferSize: 1, refCount: true})
        );
    }

    /**
     * Registers this RunViewerComponent on view initialization and creates the WebSocket subscription.
     */
    ngOnInit(): void {
        /* Register WebSocket ping. */
        this.pingSubscription = interval(5000).pipe(
            withLatestFrom(this.activeRoute.params),
            tap(([i, a]) => this.webSocketSubject.next({runId: a.runId, type: 'PING'} as IWsClientMessage))
        ).subscribe();
    }

    /**
     * Unregisters this RunViewerComponent on view destruction and cleans the WebSocket subscription.
     */
    ngOnDestroy(): void {
        /* Unregister Ping service. */
        this.pingSubscription.unsubscribe();
        this.pingSubscription = null;
    }
}
