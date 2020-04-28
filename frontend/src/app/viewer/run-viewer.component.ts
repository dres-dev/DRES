import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {merge, Observable, Subscription} from 'rxjs';
import {flatMap, map, share, shareReplay, switchMap} from 'rxjs/operators';
import {webSocket, WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/webSocket';
import {AppConfig} from '../app.config';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {CompetitionRunService, RunInfo, RunState} from '../../../openapi';
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';

@Component({
    selector: 'app-run-viewer',
    templateUrl: './run-viewer.component.html',
    styleUrls: ['./run-viewer.component.scss']
})
export class RunViewerComponent implements OnInit, OnDestroy  {

    webSocketSubject: WebSocketSubject<IWsMessage> = webSocket({
        url: `${AppConfig.settings.endpoint.tls ? 'wss://' : 'ws://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api/ws/run`,
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
     */
    constructor(protected activeRoute: ActivatedRoute,
                protected runService: CompetitionRunService) {

        /* Basic observable for general run info; this information is static and does not change over the course of a run. */
        this.runInfo = this.activeRoute.params.pipe(
            switchMap(a => this.runService.getApiRunInfoWithRunid(a.runId)),
            shareReplay(1)
        );

        /* Basic observable for web socket messages received from the DRES server. */
        this.webSocket = this.activeRoute.params.pipe(
            flatMap(a => this.webSocketSubject.pipe(map(m => m as IWsServerMessage))),
            share()
        );

        /* Basic observable for run state info; this information is dynamic and does is subject to change over the course of a run. */
        this.runState = merge(this.activeRoute.params, this.webSocket).pipe(
            switchMap((a) => this.runService.getApiRunStateWithRunid(a.runId)),
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
