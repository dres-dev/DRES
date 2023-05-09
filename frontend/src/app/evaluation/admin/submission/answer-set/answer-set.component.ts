import { Component, Input } from "@angular/core";
import { ApiAnswerSet } from "../../../../../../openapi";

@Component({
  selector: 'app-answer-set',
  templateUrl: './answer-set.component.html',
  styleUrls: ['./answer-set.component.scss']
})
export class AnswerSetComponent {

  @Input()
  public answerSets: ApiAnswerSet[];

  public displayedColumns: ['id', 'answers', 'verdict', 'actions'];

  update(submission, value: any) {
    console.log("Update answerset for ", submission, value)
    // TODO actually update
  }
}
