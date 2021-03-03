import {Component, Input, OnInit} from '@angular/core';
import {BehaviorSubject, interval, Observable, of, Subscription} from 'rxjs';
import {JudgementRequest, JudgementService} from '../../../openapi';
import {catchError, filter, map, switchMap, withLatestFrom} from 'rxjs/operators';
import {ActivatedRoute, Router} from '@angular/router';
import {AppConfig} from '../app.config';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-judgement-voting-viewer',
  templateUrl: './judgement-voting-viewer.component.html',
  styleUrls: ['./judgement-voting-viewer.component.scss']
})
export class JudgementVotingViewerComponent implements OnInit {

  @Input() pollingFrequency = 10_000;

  private runId: Observable<string>;
  private requestSub: Subscription;

  isJudgmentAvailable = false;
  judgementRequest: JudgementRequest = null;

  observableJudgementRequest: BehaviorSubject<JudgementRequest> = new BehaviorSubject<JudgementRequest>(null);
  voteClientPath: Observable<string>;

  constructor(
      private judgementService: JudgementService,
      private activeRoute: ActivatedRoute,
      private config: AppConfig,
      private snackBar: MatSnackBar,
      private router: Router,
  ) { }

  ngOnInit(): void {

    this.runId = this.activeRoute.params.pipe(map(p => p.runId));
    this.voteClientPath = this.runId.pipe(map(id => this.config.resolveUrl(`vote#${id}`)));

    /* Poll for score updates in a given interval. */
    this.requestSub = interval(this.pollingFrequency).pipe(
        withLatestFrom(this.runId),
        switchMap(([i, runId]) => {
          if (this.runId) {
            return this.judgementService.getApiRunWithRunidVoteNext(runId, 'response').pipe(
                map((req: HttpResponse<JudgementRequest>) => {
                  if (req.status === 202) {
                    this.isJudgmentAvailable = false;
                    this.judgementRequest = null;
                    console.log('currently nothing for audience to vote on');
                    return null;
                  } else {
                    const lastRequest = req.body;
                    if (this.judgementRequest !== null && lastRequest.token === this.judgementRequest.token) {
                      return of(null); // still the same, no action required
                    }
                    return lastRequest;
                  }
                }),
                catchError(err => {
                  const httpErr = err as HttpErrorResponse;
                  if (httpErr) {
                    if (httpErr.status === 404) {
                      const snack = this.snackBar.open(`Invalid runId: ${runId}`, null, {duration: 2000});
                      snack.afterDismissed().subscribe(() => {
                        this.router.navigate(['/run/list']);
                      });
                    }
                  }
                  console.log('[Judgement Voting View] Error in getApiRunWithRunidVoteNext: ');
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
      this.judgementRequest = req;
      this.observableJudgementRequest.next(req);
      this.isJudgmentAvailable = true;
    });
  }

}
