import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { BehaviorSubject, merge, Observable, of, Subscription } from 'rxjs';
import { catchError, filter, map, switchMap, take, withLatestFrom } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../app.config';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { JudgementMediaViewerComponent } from './judgement-media-viewer.component';
import {ApiJudgementRequest, JudgementService} from '../../../openapi';
import { WebSocketService } from '../services/websocket.service';
import { PollCadence } from '../services/poll-cadence';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

@Component({
  selector: 'app-judgement-voting-viewer',
  templateUrl: './judgement-voting-viewer.component.html',
  styleUrls: ['./judgement-voting-viewer.component.scss'],
  standalone: false,
})
export class JudgementVotingViewerComponent implements OnInit, OnDestroy {
  @Input() pollingFrequency = 1000;

  /** Watches the vote queue, so an idle run is polled rarely and a busy one often. */
  private readonly votePolls = new PollCadence(this.wsService, 'vote-request');

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

    /* Fetch next vote request on websocket event, or on the adaptive fallback cadence when the
       socket cannot be trusted. */
    this.requestSub = merge(this.wsService.fallbackPolling(), wsRefresh$)
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
                  this.votePolls.observe(null);
                  return null;
                } else {
                  const lastRequest = req.body;
                  if (this.judgementRequest !== null && lastRequest.token === this.judgementRequest.token) {
                    this.votePolls.observe(lastRequest.token);
                    return null; // still the same, no action required
                  }
                  this.votePolls.observe(lastRequest.token ?? 'pending');
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
