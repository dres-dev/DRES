import { Component, OnInit } from '@angular/core';
import {Observable} from 'rxjs';
import {Submission} from '../../../openapi';
//import {JudgementRequest} from '../../../openapi';

@Component({
  selector: 'app-judgement-viewer',
  templateUrl: './judgement-viewer.component.html',
  styleUrls: ['./judgement-viewer.component.scss']
})
export class JudgementViewerComponent implements OnInit {

  //judgementRequest: Observable<JudgementRequest>;

  constructor() { }

  ngOnInit(): void {
  }

  public judge(status: Submission.StatusEnum) {

  }

}
