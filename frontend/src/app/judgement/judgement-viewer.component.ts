import { Component, OnInit } from '@angular/core';
import {Observable} from 'rxjs';
import {Judgement, JudgementService, Submission} from '../../../openapi';
import {JudgementRequest} from '../../../openapi';

@Component({
  selector: 'app-judgement-viewer',
  templateUrl: './judgement-viewer.component.html',
  styleUrls: ['./judgement-viewer.component.scss']
})
export class JudgementViewerComponent implements OnInit {

  nextJudgementRequest: Observable<JudgementRequest>;
  private currentRequest: JudgementRequest;
  private judgementService: JudgementService;

  private runId =  'TODO';

  constructor(judgementService: JudgementService) {
    this.judgementService = judgementService;
  }

  ngOnInit(): void {
  }

  public judge(status: Submission.StatusEnum) {
    const judgement = {
      token : this.currentRequest.token,
      verdict : status
    } as Judgement;
    this.judgementService.postApiRunWithRunidJudge(this.runId, judgement);
  }

}
