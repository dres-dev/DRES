import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {interval, Observable, Subscription} from 'rxjs';
import {Judgement, JudgementRequest, JudgementService, SubmissionInfo} from '../../../openapi';
import {ActivatedRoute} from '@angular/router';
import {switchMap} from 'rxjs/operators';
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

    private routeSubscription: Subscription;

    private currentRequest: Observable<JudgementRequest>;

    judgementRequest: JudgementRequest;
    private runId: string;

    constructor(
        private judgementService: JudgementService,
        private activeRoute: ActivatedRoute
    ) {
    }

    ngOnInit(): void {
        /* Subscription and current run id */
        this.routeSubscription = this.activeRoute.params.subscribe(p => {
            this.runId = p.competitionId;
        });
        /* Get the current judgment request whenever an update occurs */ // TODO is this a reasonable approach or better polling?
        this.currentRequest = interval(3000).pipe(
            /* only fire if avs task */
            switchMap(state => {
                return this.judgementService.getApiRunWithRunidJudgeNext(this.runId.toString());
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
    }

    ngAfterViewInit(): void {
    }

    ngOnDestroy(): void {
        this.routeSubscription.unsubscribe();
    }

    public judge(status: SubmissionInfo.StatusEnum) {
        const judgement = {
            token: this.judgementRequest.token,
            validator: this.judgementRequest.validator,
            verdict: status
        } as Judgement;
        this.judgementService.postApiRunWithRunidJudge(this.runId, judgement);
    }

}
