import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject, merge, Observable, of, Subscription} from 'rxjs';
import {catchError, filter, flatMap, map, retry, share, shareReplay, switchMap} from 'rxjs/operators';
import {webSocket, WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/webSocket';
import {AppConfig} from '../app.config';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {CompetitionRunService, RunInfo, RunState} from '../../../openapi';
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
    selector: 'app-run-viewer',
    templateUrl: './run-viewer.component.html',
    styleUrls: ['./run-viewer.component.scss']
})
export class RunViewerComponent implements OnInit, OnDestroy  {

    webSocketProvider = new BehaviorSubject(webSocket({
        url: `${AppConfig.settings.endpoint.tls ? 'wss://' : 'ws://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api/ws/run`
    } as WebSocketSubjectConfig<IWsMessage>));

    webSocketSubject: WebSocketSubject<IWsMessage> = webSocket({
        url: `${AppConfig.settings.endpoint.tls ? 'wss://' : 'ws://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api/ws/run`
    } as WebSocketSubjectConfig<IWsMessage>);

    webSocket: Observable<IWsServerMessage>;
    runInfo: Observable<RunInfo>;
    runState: Observable<RunState>;

    /** Internal WebSocket subscription for logging purposes. */
    private logSubscription: Subscription;

    /**
     * Constructor; extracts the runId and keeps a local reference.
     *
     * @param activeRoute
     * @param runService
     * @param snackBar
     */
    constructor(private activeRoute: ActivatedRoute,
                private runService: CompetitionRunService,
                private snackBar: MatSnackBar) {

        /* Basic observable for general run info; this information is static and does not change over the course of a run. */
        this.runInfo = this.activeRoute.params.pipe(
            switchMap(a => this.runService.getApiRunInfoWithRunid(a.runId).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[RunViewerComponent] There was an error while loading information in the current run: ${err?.message}`);
                    this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay(1)
        );

        /* Basic observable for web socket messages received from the DRES server. */
        this.webSocket = this.activeRoute.params.pipe(
            flatMap(a => this.webSocketSubject.pipe(
                map(m => m as IWsServerMessage),
                catchError((err, o) => {
                    console.log(`[RunViewerComponent] An error occurred with the WebSocket communication channel: ${err?.message}.`);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            share()
        );

        /* Basic observable for run state info; this information is dynamic and does is subject to change over the course of a run. */
        this.runState = merge(this.activeRoute.params, this.webSocket).pipe(
            switchMap((a) => this.runService.getApiRunStateWithRunid(a.runId).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[RunViewerComponent] There was an error while loading information in the current run state: ${err?.message}`);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay(1)
        );
    }

    /**
     * Registers this RunViewerComponent on view initialization and creates the WebSocket subscription.
     */
    ngOnInit(): void {
        this.activeRoute.params.subscribe(a => {
            this.webSocketSubject.next({runId: a.runId, type: 'REGISTER'} as IWsClientMessage);
        });

        /* Register WebSocket logger. */
        this.logSubscription = this.webSocket.subscribe(m => {
            console.log(`WebSocket message received: ${m.type}`);
        });
    }

    /**
     * Unregisters this RunViewerComponent on view destruction and cleans the WebSocket subscription.
     */
    ngOnDestroy(): void {
        this.activeRoute.params.subscribe(a => {
            this.webSocketSubject.next({runId: a.runId, type: 'UNREGISTER'} as IWsClientMessage);
        });

        /* Unregister WebSocket logger. */
        this.logSubscription.unsubscribe();
        this.logSubscription = null;
    }
}
