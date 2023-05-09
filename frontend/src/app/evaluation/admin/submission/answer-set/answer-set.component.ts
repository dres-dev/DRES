import { Component, Input } from "@angular/core";
import { ApiAnswerSet, ApiVerdictStatus, EvaluationAdministratorService } from "../../../../../../openapi";
import { MatSnackBar } from "@angular/material/snack-bar";

@Component({
  selector: 'app-answer-set',
  templateUrl: './answer-set.component.html',
  styleUrls: ['./answer-set.component.scss']
})
export class AnswerSetComponent {

  constructor(
    private evaluationService: EvaluationAdministratorService,
    private snakcBar: MatSnackBar,
  ) {
  }

  @Input()
  public answerSets: ApiAnswerSet[];

  @Input()
  public evaluationId: string;

  public displayedColumns: ['id', 'answers', 'verdict', 'actions'];

  update(submission: ApiAnswerSet, value: ApiVerdictStatus) {
    console.log("Update answerset for ", submission, value)
    submission.status = value;
    this.evaluationService.patchApiV2EvaluationAdminByEvaluationIdOverrideByAnswerSetId(this.evaluationId, submission.id, { verdict: value }).subscribe((res) => {
      this.snakcBar.open(`AnswerSet ${submission.id} successfully updated to ${value}.`, null, {duration: 5000});
    });
  }
}
