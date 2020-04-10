import {Component, Input} from '@angular/core';
import { RunInfo, RunState} from '../../../openapi';
import {Observable} from 'rxjs';

@Component({
    selector: 'app-scoreboard-viewer',
    templateUrl: './scoreboard-viewer.component.html',
    styleUrls: ['./scoreboard-viewer.component.scss']
})
export class ScoreboardViewerComponent {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
}
