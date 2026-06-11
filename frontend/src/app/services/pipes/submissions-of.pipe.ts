import { Pipe, PipeTransform } from '@angular/core';
import { ApiSubmission, EvaluationAdministratorService } from '../../../../openapi';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Pipe({
  name: 'submissionsOf',
  standalone: false,
})
export class SubmissionsOfPipe implements PipeTransform {
  constructor(private adminService: EvaluationAdministratorService) {}

  /**
   * Returns the submissions of the given task template for the specified evaluation.
   * @param templateId The task template id whose tasks' submissions are loaded
   * @param evaluationId The evaluation id whose tasks should be considered
   */
  transform(templateId: string, evaluationId: string): Observable<ApiSubmission[]> {
    return this.adminService
      .getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(evaluationId, templateId)
      .pipe(map((submissionInfos) => submissionInfos.flatMap((info) => info.submissions)));
  }
}
