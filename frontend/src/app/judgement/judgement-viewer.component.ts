import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {merge, Observable, Subscription} from 'rxjs';
import {
    CompetitionRunService,
    Judgement,
    JudgementRequest,
    JudgementService,
    RunInfo,
    RunState,
    Submission,
    TaskGroup
} from '../../../openapi';
import {ActivatedRoute} from '@angular/router';
import {webSocket, WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/webSocket';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {AppConfig} from '../app.config';
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';
import {filter, flatMap, map, share, shareReplay, switchMap} from 'rxjs/operators';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {JudgementMediaViewerComponent} from './judgement-media-viewer.component';

/**
 * This component subscribes to the websocket for submissions.
 * If the current task is an AVS task, a new submission triggers judgment.
 */
@Component({
    selector: 'app-judgement-viewer',
    templateUrl: './judgement-viewer.component.html',
    styleUrls: ['./judgement-viewer.component.scss']
})
export class JudgementViewerComponent implements OnInit, OnDestroy, AfterViewInit {

    @ViewChild(JudgementMediaViewerComponent) judgePlayer: JudgementMediaViewerComponent;

    webSocketSubject: WebSocketSubject<IWsMessage> = webSocket({
        url: `${AppConfig.settings.endpoint.tls ? 'wss://' : 'ws://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api/ws/run`,
    } as WebSocketSubjectConfig<IWsMessage>);
    private webSocket: Observable<IWsServerMessage>;
    private runInfo: Observable<RunInfo>;
    private runState: Observable<RunState>;
    private routeSubscription: Subscription;

    private currentRequest: Observable<JudgementRequest>;

    private judgementRequest: JudgementRequest;
    private runId: string;
    private name: string;

    constructor(private judgementService: JudgementService, private activeRoute: ActivatedRoute, private runService: CompetitionRunService) {
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

    ngOnInit(): void {
        /* Subscription and current run id */
        this.routeSubscription = this.activeRoute.params.subscribe(p => {
            this.runId = p.competitionId;
        });
        this.runInfo.subscribe(info => {
            this.runId = info.id.toString();
        });
        /* Websocket for submission */
        this.activeRoute.params.subscribe(a => {
            this.webSocketSubject.next({runId: a.runId, type: 'REGISTER'} as IWsClientMessage);
        });
        /* Get the current judgment request whenever an update occurs */ // TODO is this a reasonable approach or better polling?
        this.currentRequest = this.runState.pipe(
            /* only fire if avs task */
            filter(state => state.currentTask.taskGroup.type === TaskGroup.TypeEnum.AVS),
            switchMap(state => {
                return this.judgementService.getApiRunWithRunidJudgeNext(state.id.toString());
            })
        );
        /* TODO subject thingy */
        this.currentRequest.subscribe(req => {
            console.log('Received request');
            console.log(req);
            // TODO handle case there is no submission to judge
            this.judgementRequest = req;
            this.judgePlayer.judge(req);
        });
        /* Beautification */
        this.runState.subscribe(state => {
            this.name = state.currentTask.name;
        });
    }

    ngAfterViewInit(): void {
    }

    ngOnDestroy(): void {
        this.activeRoute.params.subscribe(a => {
            this.webSocketSubject.next({runId: a.runId, type: 'UNREGISTER'} as IWsClientMessage);
        });
        this.routeSubscription.unsubscribe();
    }

    public judge(status: Submission.StatusEnum) {
        const judgement = {
            token: this.judgementRequest.token,
            validator: this.judgementRequest.validator,
            verdict: status
        } as Judgement;
        this.judgementService.postApiRunWithRunidJudge(this.runId, judgement);
    }

}
