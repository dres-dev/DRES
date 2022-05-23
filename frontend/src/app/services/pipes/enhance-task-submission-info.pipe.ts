import { Pipe, PipeTransform } from '@angular/core';
import { PastTaskInfo, TaskInfo, TaskRunSubmissionInfo } from '../../../../openapi';

@Pipe({
  name: 'enhanceTaskSubmissionInfo',
})
export class EnhanceTaskSubmissionInfoPipe implements PipeTransform {
  transform(value: TaskInfo, submissionInfos: TaskRunSubmissionInfo[]): TaskRunSubmissionInfo | null {
    if (!value || !submissionInfos) {
      return null;
    }
    const filtered = submissionInfos.filter((si) => si.taskRunId === value.id);
    return filtered.length > 0 ? filtered[0] : null;
  }
}
