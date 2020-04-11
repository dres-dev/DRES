import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {merge, Observable, Subscription} from 'rxjs';
import {filter, first, flatMap, map, tap} from 'rxjs/operators';
import {webSocket, WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/webSocket';
import {AppConfig} from '../app.config';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {CompetitionRunService, RunInfo, RunState} from '../../../openapi';
import {ServerMessageType} from '../model/ws/server-message-type.enum';
import ServerMessageTypes = ServerMessageType.ServerMessageTypes;
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';

@Component({
    selector: 'app-run-viewer',
    templateUrl: './run-viewer.component.html',
    styleUrls: ['./run-viewer.component.scss']
})
export class RunViewerComponent implements OnInit, OnDestroy  {

    webSocket: WebSocketSubject<IWsMessage> = webSocket({
        url: `${AppConfig.settings.endpoint.tls ? 'wss://' : 'ws://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api/ws/run`,
    } as WebSocketSubjectConfig<IWsMessage>);

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

        this.runInfo = this.activeRoute.params.pipe(
            flatMap(p => this.runService.getApiRunInfoWithRunid(p.runId))
        );

        this.runState = this.activeRoute.params.pipe(
            flatMap( p => {
                return merge(
                    this.runService.getApiRunStateWithRunid(p.runId),
                    this.webSocket.pipe(
                        map(m => m as IWsServerMessage),
                        filter(m => ServerMessageTypes.indexOf(m.type) > -1),
                        flatMap(() => this.runService.getApiRunStateWithRunid(p.runId))
                    )
                );
            })
        );
    }

    /**
     * Registers this RunViewerComponent on view initialization and creates the WebSocket subscription.
     */
    ngOnInit(): void {
        this.activeRoute.params.subscribe(p => {
            this.webSocket.next({runId: p.runId, type: 'REGISTER'} as IWsClientMessage);
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
        this.activeRoute.params.subscribe(p => {
            this.webSocket.next({runId: p.runId, type: 'UNREGISTER'} as IWsClientMessage);
        });

        /* Unregister WebSocket logger. */
        this.logSubscription.unsubscribe();
        this.logSubscription = null;
    }
}
