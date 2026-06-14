import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { BehaviorSubject, merge, Observable, of, Subscription, timer } from 'rxjs';
import { catchError, filter, map, switchMap, take, withLatestFrom } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../app.config';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { JudgementMediaViewerComponent } from './judgement-media-viewer.component';
<<<<<<< HEAD
import { ApiJudgementRequest, JudgementService } from '../../../openapi';
=======
import {ApiJudgementRequest, JudgementService} from '../../../openapi';
>>>>>>> d4e51a229d945ba8e81455762fa7bf0759518b7a
import { WebSocketService } from '../services/websocket.service';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

@Component({
  selector: 'app-judgement-voting-viewer',
  templateUrl: './judgement-voting-viewer.component.html',
  styleUrls: ['./judgement-voting-viewer.component.scss'],
  standalone: false,
})
export class JudgementVotingViewerComponent implements OnInit, OnDestroy {
  @Input() pollingFrequency = 1000;

  private runId: Observable<string>;
  private requestSub: Subscription;

  isJudgmentAvailable = false;
  judgementRequest: ApiJudgementRequest = null;

  observableJudgementRequest: BehaviorSubject<ApiJudgementRequest> = new BehaviorSubject<ApiJudgementRequest>(null);
  voteClientPath: Observable<string>;

  @ViewChild(JudgementMediaViewerComponent) judgePlayer: JudgementMediaViewerComponent;

  constructor(
    private judgementService: JudgementService,
    private activeRoute: ActivatedRoute,
    private config: AppConfig,
    private snackBar: MatSnackBar,
    private router: Router,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.runId = this.activeRoute.params.pipe(map((p) => p.runId));
    this.voteClientPath = this.runId.pipe(map((id) => this.config.resolveUrl(`vote#${id}`)));

    this.runId.pipe(take(1)).subscribe((id) => this.wsService.connect(id));

    const wsRefresh$ = this.wsService.messages$.pipe(
      filter((msg) => [
        ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
        ServerMessageType.ServerMessageTypeEnum.TASK_START,
        ServerMessageType.ServerMessageTypeEnum.TASK_END,
      ].includes(msg.type))
    );

    /* Fetch next vote request on websocket event or 30s fallback poll. */
    this.requestSub = merge(timer(0, 30_000), wsRefresh$)
      .pipe(
        withLatestFrom(this.runId),
        switchMap(([_, runId]) => {
          if (this.runId) {
            return this.judgementService.getApiV2EvaluationByEvaluationIdVoteNext(runId, 'response').pipe(
              map((req: HttpResponse<ApiJudgementRequest>) => {
                if (req.status === 202) {
                  this.isJudgmentAvailable = false;
                  this.judgementRequest = null;
                  this.judgePlayer?.stop();
                  console.log('currently nothing for audience to vote on');
                  return null;
                } else {
                  const lastRequest = req.body;
                  if (this.judgementRequest !== null && lastRequest.token === this.judgementRequest.token) {
                    return null; // still the same, no action required
                  }
                  return lastRequest;
                }
              }),
              catchError((err) => {
                const httpErr = err as HttpErrorResponse;
                if (httpErr) {
                  if (httpErr.status === 404) {
                    const snack = this.snackBar.open(`Invalid runId: ${runId}`, null, { duration: 2000 });
                    snack.afterDismissed().subscribe(() => {
                      this.router.navigate(['/evaluation/list']);
                    });
                  }
                }
                console.log('[Judgement Voting View] Error in getApiV1RunWithRunidVoteNext: ');
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
        this.judgementRequest = req;
        this.observableJudgementRequest.next(req);
        this.isJudgmentAvailable = true;
      });
  }

  allAnswers() {
    return this.observableJudgementRequest?.value?.answerSet?.answers || [];
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
    this.requestSub.unsubscribe();
    this.requestSub = null;
    if (this.judgePlayer) {
      this.judgePlayer.stop();
    }
  }
}
