import {AfterViewInit, Component, Input} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, SubmissionInfo, Team} from '../../../openapi';
import {Observable} from 'rxjs';
import {map, switchMap, withLatestFrom} from 'rxjs/operators';

@Component({
    selector: 'app-teams-viewer',
    templateUrl: './teams-viewer.component.html',
    styleUrls: ['./teams-viewer.component.scss']
})
export class TeamsViewerComponent implements AfterViewInit {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;

    submissions: Observable<Map<Team, SubmissionInfo[]>>;

    constructor(protected runService: CompetitionRunService) {}

    ngAfterViewInit(): void {
        this.submissions = this.state.pipe(
            switchMap(s => this.runService.getApiRunWithRunidTaskSubmissions(s.id)),
            withLatestFrom(this.info),
            map(([submissions, info]) => {
                const submissionMap = new Map<Team, SubmissionInfo[]>();
                info.teams.forEach((v, i) => {
                    submissionMap.set(v,  submissions.filter(s => s.team === i));
                });
                return submissionMap;
            })
        );
    }
}

