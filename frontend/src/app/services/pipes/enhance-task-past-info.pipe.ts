import {Pipe, PipeTransform} from '@angular/core';
import {PastTaskInfo, TaskInfo} from '../../../../openapi';

@Pipe({
    name: 'enhanceTaskPastInfo'
})
export class EnhanceTaskPastInfoPipe implements PipeTransform {

    transform(value: TaskInfo, pastTasks: PastTaskInfo[]): PastTaskInfo | null {
        const filtered = pastTasks.filter(pt => pt.descriptionId === value.id);
        return filtered.length > 0 ? filtered[0] : null;
    }
}
