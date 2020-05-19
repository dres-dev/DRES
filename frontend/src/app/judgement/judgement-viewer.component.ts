import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {interval, Observable, of, Subscription} from 'rxjs';
import {ErrorStatus, Judgement, JudgementRequest, JudgementService, SubmissionInfo} from '../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {catchError, filter, shareReplay, switchMap} from 'rxjs/operators';
import {JudgementMediaViewerComponent} from './judgement-media-viewer.component';
import {MatSnackBar} from '@angular/material/snack-bar';
import {HttpErrorResponse} from '@angular/common/http';

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

    @Input() pollingFrequency = 1000;
    @ViewChild(JudgementMediaViewerComponent) judgePlayer: JudgementMediaViewerComponent;
    judgementRequest: JudgementRequest;
    noJudgementMessage = '';
    isJudgmentAvailable = false;
    private routeSubscription: Subscription;
    private currentRequest: Observable<JudgementRequest>;
    private runId: string;

    private intervalRef: Observable<number>;
    private intervalSub: Subscription;

    constructor(
        private judgementService: JudgementService,
        private activeRoute: ActivatedRoute,
        private snackBar: MatSnackBar,
        private router: Router
    ) {
    }

    ngOnInit(): void {
        /* Subscription and current run id */
        this.routeSubscription = this.activeRoute.params.subscribe(p => {
            console.log('[Judgem.View] route param: ' + p.runId);
            this.runId = p.runId;
        });
        /* Get the current judgment request whenever an update occurs */
        this.intervalRef = interval(this.pollingFrequency);
        /*this.intervalSub = this.intervalRef.subscribe(_ => {

        });*/

        this.currentRequest = this.intervalRef.pipe(
            switchMap(_ => {
                /* Stop polling while judgment is ongooing */
                if (this.runId && !this.isJudgmentAvailable) {
                    return this.judgementService.getApiRunWithRunidJudgeNext(this.runId).pipe(
                        switchMap(req => {
                            if (req.hasOwnProperty('status') && req.hasOwnProperty('description')) {
                                console.log('[Judgem.View] No judgement yet');
                                const noReq = (req as unknown) as ErrorStatus; // unkown first to make TSLint happy
                                this.noJudgementMessage = noReq.description;
                                this.isJudgmentAvailable = false;
                                return of(null);
                            }
                            return of(req as JudgementRequest);
                        }),
                        catchError(err => {
                            const httperr = err as HttpErrorResponse;
                            if (httperr) {
                                if (httperr.status === 404) {
                                    const snack = this.snackBar.open(`Invalid runId: ${this.runId}`, null, {duration: 2000});
                                    snack.afterDismissed().subscribe(() => {
                                        this.router.navigate(['/run/list']);
                                    });
                                }
                            }
                            console.log('[Judgem.View] Error in getJudgeNext: ');
                            console.log(err);
                            return of(null);
                        })
                    );
                } else {
                    return of(null);
                }
            }),
            /* Filter null values */
            filter(x => x != null),
            shareReplay(1)
        );

        /* Beautification */
    }

    ngAfterViewInit(): void {
        /* TODO subject thingy */
        this.currentRequest.subscribe(req => {
            console.log('[Judgem.View] Received request');
            console.log(req);
            // TODO handle case there is no submission to judge
            this.judgementRequest = req;
            this.isJudgmentAvailable = true;
            this.judgePlayer.judge(req);
        });
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
        this.judgementService.postApiRunWithRunidJudge(this.runId, judgement)
            .subscribe(res => {
                this.snackBar.open(res.description, null, {duration: 5000});
            });
        this.judgePlayer.stop();
        this.judgementRequest = null;
        this.isJudgmentAvailable = false;
    }

}
