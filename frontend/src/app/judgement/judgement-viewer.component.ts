import {Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {interval, Observable, of, Subscription} from 'rxjs';
import {Judgement, JudgementRequest, JudgementService, SubmissionInfo} from '../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {catchError, filter, map, switchMap, withLatestFrom} from 'rxjs/operators';
import {JudgementMediaViewerComponent} from './judgement-media-viewer.component';
import {MatSnackBar} from '@angular/material/snack-bar';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';

/**
 * This component subscribes to the websocket for submissions.
 * If the current task is an AVS task, a new submission triggers judgment.
 */
@Component({
    selector: 'app-judgement-viewer',
    templateUrl: './judgement-viewer.component.html',
    styleUrls: ['./judgement-viewer.component.scss']
})
export class JudgementViewerComponent implements OnInit, OnDestroy {

    @Input() pollingFrequency = 1000;
    @ViewChild(JudgementMediaViewerComponent) judgePlayer: JudgementMediaViewerComponent;
    judgementRequest: JudgementRequest;
    noJudgementMessage = '';
    isJudgmentAvailable = false;

    private runId: Observable<string>;
    private requestSub: Subscription;

    constructor(
        private judgementService: JudgementService,
        private activeRoute: ActivatedRoute,
        private snackBar: MatSnackBar,
        private router: Router
    ) {
    }

    ngOnInit(): void {
        /* Subscription and current run id */
        this.runId = this.activeRoute.params.pipe(map(p => p.runId));

        /* Poll for score updates in a given interval. */
        this.requestSub = interval(this.pollingFrequency).pipe(
            withLatestFrom(this.runId),
            switchMap(([i, runId]) => {
                /* Stop polling while judgment is ongooing */
                if (this.runId && !this.isJudgmentAvailable) {
                    return this.judgementService.getApiRunWithRunidJudgeNext(runId, 'response').pipe(
                        map((req: HttpResponse<JudgementRequest>) => {
                            if (req.status === 202) {
                                this.noJudgementMessage = 'There is currently no submission awaiting judgement.';
                                this.isJudgmentAvailable = false;
                                return null;
                            } else {
                                return req.body;
                            }
                        }),
                        catchError(err => {
                            const httperr = err as HttpErrorResponse;
                            if (httperr) {
                                if (httperr.status === 404) {
                                    const snack = this.snackBar.open(`Invalid runId: ${runId}`, null, {duration: 2000});
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
            filter(x => x != null)
        ).subscribe(req => {
            console.log('[Judgem.View] Received request');
            console.log(req);
            // TODO handle case there is no submission to judge
            this.judgementRequest = req;
            this.isJudgmentAvailable = true;
            this.judgePlayer.judge(req);
        });
    }

    /**
     *
     */
    ngOnDestroy(): void {
        this.requestSub.unsubscribe();
        this.requestSub = null;
    }

    /**
     *
     * @param status
     */
    public judge(status: SubmissionInfo.StatusEnum) {
        const judgement = {
            token: this.judgementRequest.token,
            validator: this.judgementRequest.validator,
            verdict: status
        } as Judgement;
        this.runId.pipe(
            switchMap(runId => this.judgementService.postApiRunWithRunidJudge(runId, judgement))
        ).subscribe(res => {
            this.snackBar.open(res.description, null, {duration: 5000});
        });
        this.judgePlayer.stop();
        this.judgementRequest = null;
        this.isJudgmentAvailable = false;
    }

}
