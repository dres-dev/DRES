import {Component, Input} from '@angular/core';
import {RunInfo, RunState} from '../../../openapi';
import {Observable} from 'rxjs';

@Component({
    selector: 'app-task-viewer',
    templateUrl: './task-viewer.component.html'
})
export class TaskViewerComponent {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
}
