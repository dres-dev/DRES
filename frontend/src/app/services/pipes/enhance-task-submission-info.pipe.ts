import { Pipe, PipeTransform } from '@angular/core';
import {ApiSubmission, ApiSubmissionInfo, ApiTaskTemplateInfo} from '../../../../openapi';

@Pipe({
  name: 'enhanceTaskSubmissionInfo',
})
export class EnhanceTaskSubmissionInfoPipe implements PipeTransform {
  // FIXME compiler happiness: not sure whether this is appropriate
  transform(value: ApiTaskTemplateInfo, submissionInfos: ApiSubmissionInfo[]): ApiSubmissionInfo | null {
    if (!value || !submissionInfos) {
      return null;
    }
    const filtered = submissionInfos.filter((si) => si.taskId === value.templateId);
    return filtered.length > 0 ? filtered[0] : null;
  }
}
