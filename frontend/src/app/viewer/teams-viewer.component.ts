import {Component, Input} from '@angular/core';
import {RunInfo, RunState} from '../../../openapi';
import {Observable} from 'rxjs';

@Component({
    selector: 'app-teams-viewer',
    templateUrl: './teams-viewer.component.html',
    styleUrls: ['./teams-viewer.component.scss']
})
export class TeamsViewerComponent {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
}

