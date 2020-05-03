import {Component, OnDestroy, OnInit} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {Judgement, JudgementService, Submission} from '../../../openapi';
import {JudgementRequest} from '../../../openapi';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-judgement-viewer',
  templateUrl: './judgement-viewer.component.html',
  styleUrls: ['./judgement-viewer.component.scss']
})
export class JudgementViewerComponent implements OnInit, OnDestroy {

  nextJudgementRequest: Observable<JudgementRequest>;
  private currentRequest: JudgementRequest;

  routeSubscription: Subscription;

  private runId;

  constructor(private judgementService: JudgementService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.params.subscribe(p => {
      this.runId = p.competitionId;
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription.unsubscribe();
  }

  public judge(status: Submission.StatusEnum) {
    const judgement = {
      token : this.currentRequest.token,
      validator: this.currentRequest.validator,
      verdict : status
    } as Judgement;
    this.judgementService.postApiRunWithRunidJudge(this.runId, judgement);
  }

}
