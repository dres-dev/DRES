import { Pipe, PipeTransform } from "@angular/core";
import { ApiSubmission, EvaluationAdministratorService } from "../../../../openapi";
import { flatMap, Observable } from "rxjs";
import { filter, tap } from "rxjs/operators";

@Pipe({
  name: "submissionsOf"
})
export class SubmissionsOfPipe implements PipeTransform {

  constructor(private adminService: EvaluationAdministratorService) {
  }

  /**
   * Returns the submissions of the given task template for the specified evaluation.
   * @param templateId The task template id whose tasks' submissions are loaded
   * @param evaluationId The evaluation id whose tasks should be considered
   */
  transform(templateId: string, evaluationId: string): Observable<ApiSubmission[]> {
    return this.adminService
      .getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(evaluationId, templateId)
      .pipe(
        tap(e => {
          console.log("submissionsOf 1", e);
        }),
        filter((submissionInfos, idx) => {
          return submissionInfos[idx]?.evaluationId === evaluationId || false;
        }),
        tap(e => {
          console.log("submissionsOf 2", e);
        }),
        flatMap(submissionInfos => {
          return submissionInfos.filter(it => it.evaluationId === evaluationId).map(it => it.submissions);
        }),
        tap(e => {
          console.log("submissionsOf 3", e);
        })
      );
  }

}
