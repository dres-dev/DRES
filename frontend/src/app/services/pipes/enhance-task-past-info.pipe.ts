import { Pipe, PipeTransform } from '@angular/core';
import {ApiTaskTemplateInfo} from '../../../../openapi';

@Pipe({
  name: 'enhanceTaskPastInfo',
})
export class EnhanceTaskPastInfoPipe implements PipeTransform {
  // FIXME compiler happiness: not sure whether this is appropriate
  transform(value: ApiTaskTemplateInfo, pastTasks: ApiTaskTemplateInfo[]): ApiTaskTemplateInfo | null {
    if (!value || !pastTasks) {
      return null;
    }
    const filtered = pastTasks.filter((pt) => pt.templateId === value.templateId);
    return filtered.length > 0 ? filtered[0] : null;
  }
}
