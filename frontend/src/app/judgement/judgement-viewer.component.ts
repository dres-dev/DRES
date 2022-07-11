import {AfterViewInit, Component, HostListener, Input, OnDestroy, ViewChild} from '@angular/core';
import {BehaviorSubject, interval, Observable, of, Subscription, timer} from 'rxjs';
import {Judgement, JudgementRequest, JudgementService, SubmissionInfo} from '../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {catchError, filter, map, switchMap, withLatestFrom} from 'rxjs/operators';
import {JudgementMediaViewerComponent} from './judgement-media-viewer.component';
import {MatSnackBar} from '@angular/material/snack-bar';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {animate, keyframes, state, style, transition, trigger} from '@angular/animations';
import {MatDialog} from '@angular/material/dialog';
import {JudgementDialogComponent} from './judgement-dialog/judgement-dialog.component';
import {JudgementDialogContent} from './judgement-dialog/judgement-dialog-content.model';

/**
 * This component subscribes to the websocket for submissions.
 * If the current task is an AVS task, a new submission triggers judgment.
 */
@Component({
    selector: 'app-judgement-viewer',
    templateUrl: './judgement-viewer.component.html',
    styleUrls: ['./judgement-viewer.component.scss'],
    animations: [
        trigger('newDesc', [
            state('known', style({backgroundColor: 'transparent'})),
            state('fresh', style({backgroundColor: 'transparent'})),

            transition('known => fresh', [
                animate(
                    '2s',
                    keyframes([
                        style({backgroundColor: 'transparent', offset: 0}),
                        style({backgroundColor: '#7b1fa2', offset: 0.2}), // TODO how to get access to primary color of theme
                        style({backgroundColor: 'transparent', offset: 1}),
                    ])
                ),
            ]),
        ]),
    ],
})
export class JudgementViewerComponent implements AfterViewInit, OnDestroy {
    status: 'fresh' | 'known' = 'known';

    @Input() debug = false;
    @Input() pollingFrequency = 1000;
    @Input() timeout = 60;
    @ViewChild(JudgementMediaViewerComponent) judgePlayer: JudgementMediaViewerComponent;
    observableJudgementRequest: BehaviorSubject<JudgementRequest> = new BehaviorSubject<JudgementRequest>(null);
    judgementRequest: JudgementRequest;
    prevDescHash: number;
    noJudgementMessage = '';
    isJudgmentAvailable = false;
    isNewJudgementDesc = false;

    openSubmissions = new BehaviorSubject(0);
    pendingSubmissions = new BehaviorSubject(0);

    private runId: Observable<string>;
    private requestSub: Subscription;
    private statusSub: Subscription;
    private deadMansSwitchSub: Subscription;
    private deadMansSwitchTime = 0;

    constructor(
        private judgementService: JudgementService,
        private activeRoute: ActivatedRoute,
        private snackBar: MatSnackBar,
        private router: Router,
        private dialog: MatDialog
    ) {
    }

    ngAfterViewInit(): void {
        const dialogRef = this.dialog.open(JudgementDialogComponent, {
            width: '400px',
            data: {
                title: 'Judgement Intro',
                body:
                    '<h3>Hello Judge</h3>\n' +
                    '    <p>\n' +
                    '        Once you clicked any of the button below, the judging view will open.\n' +
                    '        Your task will be to judge, whether the shown video segment fulfills the given description or not.\n' +
                    '        In case of doubt, you also can opt for <i>don\'t know</i>.\n' +
                    '    </p>\n' +
                    '    <p>\n' +
                    '        Thank you for being a fair Judge!\n' +
                    '    </p>',
            } as JudgementDialogContent,
        });
        dialogRef.afterClosed().subscribe((_) => {
            this.init();
            this.initialiseDeadMansSwitch();
        });
    }

    @HostListener('document:keypress', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        switch (event.key.toLowerCase()) {
            case 'a':
            case 'c':
                this.judge('CORRECT');
                break;
            case 'u':
                this.judge('UNDECIDABLE');
                break;
            case 'r':
            case 'w':
                this.judge('WRONG');
                break;
        }
    }

    init(): void {
        /* Subscription and current run id */
        this.runId = this.activeRoute.params.pipe(map((p) => p.runId));
        /* Poll for score status in a given interval */
        this.statusSub = interval(this.pollingFrequency)
            .pipe(
                withLatestFrom(this.runId),
                switchMap(([i, runId]) => {
                    return this.judgementService.getApiV1RunWithRunidJudgeStatus(runId).pipe(
                        catchError((err) => {
                            console.log('Error in JudgeStatus');
                            console.log(err);
                            return of(null);
                        }),
                        filter((x) => x !== null)
                    );
                }),
                filter((x) => x != null)
            )
            .subscribe((value) => {
                let pending = 0;
                let open = 0;
                value.forEach((j) => {
                    pending += j.pending;
                    open += j.open;
                });
                this.updateProgress(pending, open);
            });

        /* Poll for score updates in a given interval. */
        this.requestSub = interval(this.pollingFrequency)
            .pipe(
                withLatestFrom(this.runId),
                switchMap(([i, runId]) => {
                    /* Stop polling while judgment is ongooing */
                    if (this.runId && !this.isJudgmentAvailable) {
                        return this.judgementService.getApiV1RunWithRunidJudgeNext(runId, 'response').pipe(
                            map((req: HttpResponse<JudgementRequest>) => {
                                if (req.status === 202) {
                                    this.noJudgementMessage = 'There is currently no submission awaiting judgement.';
                                    /* Don't penalise if there's nothing to do*/
                                    this.deadMansSwitchTime = 0;
                                    this.isJudgmentAvailable = false;
                                    return null;
                                } else {
                                    return req.body;
                                }
                            }),
                            catchError((err) => {
                                const httperr = err as HttpErrorResponse;
                                if (httperr) {
                                    if (httperr.status === 404) {
                                        const snack = this.snackBar.open(`Invalid runId: ${runId}`, null, {duration: 2000});
                                        snack.afterDismissed().subscribe(() => {
                                            this.router.navigate(['/run/list']);
                                        });
                                    } else if (httperr.status === 408) {
                                        this.snackBar.open(`You were inactive for too long and the verdict was not accepted by teh server`, null, {duration: 2000});
                                        return of(null);
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
                filter((x) => x != null)
            )
            .subscribe((req) => {
                console.log('[Judgem.View] Received request');
                console.log(req);
                if (this.prevDescHash) {
                    this.isNewJudgementDesc = this.prevDescHash !== this.hashCode(req.taskDescription);
                    console.log('new: ' + this.isNewJudgementDesc);
                    if (this.isNewJudgementDesc) {
                        this.status = 'fresh';
                    } else {
                        this.status = 'known';
                    }
                }
                this.judgementRequest = req;
                this.observableJudgementRequest.next(req);
                this.isJudgmentAvailable = true;
                this.prevDescHash = this.hashCode(this.judgementRequest.taskDescription);
            });
    }

    /**
     *
     */
    ngOnDestroy(): void {
        this.stopAll();
    }

    public updateProgress(pending: number, open: number) {
        this.openSubmissions.next(Math.round(open));
        this.pendingSubmissions.next(Math.round(pending));
    }

    public judge(status: SubmissionInfo.StatusEnum) {
        this.deadMansSwitchTime = 0;
        const judgement = {
            token: this.judgementRequest.token,
            validator: this.judgementRequest.validator,
            verdict: status,
        } as Judgement;
        this.runId
            .pipe(switchMap((runId) => this.judgementService.postApiV1RunWithRunidJudge(runId, judgement)),
                catchError((err) => {
                    const httperr = err as HttpErrorResponse;
                    if (httperr) {
                        if (httperr.status === 408) {
                            this.snackBar.open(`You were inactive for too long and the verdict was not accepted by the server`, null, {duration: 2000});
                            return of(null);
                        }
                    }
                    console.log('[Judgem.View] Error in judge: ');
                    console.log(err);
                    return of(null);
                }))
            .subscribe((res) => {
                if (res) {
                    this.snackBar.open(res.description, null, {duration: 5000});
                }
            });
        this.judgePlayer.stop();
        this.judgementRequest = null;
        this.isJudgmentAvailable = false;
    }

    private stopAll() {
        this.requestSub.unsubscribe();
        this.requestSub = null;
        this.statusSub.unsubscribe();
        this.statusSub = null;
        this.deadMansSwitchSub.unsubscribe();
        this.deadMansSwitchSub = null;
        if (this.judgePlayer) {
            this.judgePlayer.stop();
        }
    }

    private initialiseDeadMansSwitch() {
        /* Dead Man's Switch: Timeout upon no action */
        this.deadMansSwitchSub = timer(1000, 1000).subscribe((val) => {
            /* emits the second value every second, with a single second delay in the beginning */
            this.deadMansSwitchTime++;
            /* If there's a timeout, display the reactivate dialog */
            if (this.deadMansSwitchTime >= this.timeout) {
                /* No more polling */
                this.stopAll();
                /* Reset time, to be safe */
                this.deadMansSwitchTime = 0;
                /* Show the dialog */
                const ref = this.dialog.open(JudgementDialogComponent, {
                    width: '400px',
                    data: {
                        title: 'Judgement Inactive',
                        body:
                            '<h3>Judgement Deactivated</h3>\n' +
                            '<p>Jugement was deactivated due to inactivity. You can continue judging by closing this dialog.</p>',
                    } as JudgementDialogContent,
                });
                ref.afterClosed().subscribe((_) => {
                    /* Apparently, the judge is back, so restart everything */
                    this.init();
                    this.initialiseDeadMansSwitch();
                    this.observableJudgementRequest.next(this.judgementRequest);
                });
            }
        });
    }

    /**
     * Kindly provided by https://stackoverflow.com/a/7616484
     */
    private hashCode(str: string) {
        let hash = 0;
        let i;
        let chr;
        for (i = 0; i < str.length; i++) {
            chr = str.charCodeAt(i);
            // eslint-disable-next-line no-bitwise
            hash = (hash << 5) - hash + chr;
            // eslint-disable-next-line no-bitwise
            hash |= 0; // Convert to 32bit integer
        }
        return hash as number;
    }
}
