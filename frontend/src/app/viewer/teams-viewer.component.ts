import {AfterViewInit, Component, Input} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, Submission} from '../../../openapi';
import {Observable} from 'rxjs';
import {map, shareReplay, switchMap, withLatestFrom} from 'rxjs/operators';

@Component({
    selector: 'app-teams-viewer',
    templateUrl: './teams-viewer.component.html',
    styleUrls: ['./teams-viewer.component.scss']
})
export class TeamsViewerComponent implements AfterViewInit {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;

    submissions: Observable<Submission[][]>;
    scores: Observable<ScoreOverview>;

    constructor(protected runService: CompetitionRunService) {}

    ngAfterViewInit(): void {
        this.submissions = this.state.pipe(
            switchMap(s => this.runService.getApiRunWithRunidTaskSubmissions(s.id)),
            withLatestFrom(this.info),
            map(([submissions, info]) => {
                return info.teams.map((v, i) => {
                    return submissions.filter(s => s.team === i);
                });
            }),
            shareReplay(1)
        );

        this.scores = this.state.pipe(
            switchMap(s => this.runService.getApiRunWithRunidScoreTask(s.id)),
            shareReplay(1)
        );
    }

    /**
     *
     * @param team
     */
    public submissionForTeam(team: number): Observable<Submission[]> {
        return this.submissions.pipe(
            map(s => {
                if (s != null) {
                    return s[team];
                } else {
                    return [];
                }
            })
        );
    }

    public score(team: number): Observable<string> {
        return this.scores.pipe(map(scores => scores.scores.find(s => s.teamId === team)?.score.toFixed(0)));
    }

    public correctSubmissions(team: number): Observable<number> {
        return this.submissions.pipe(
            map(submissions => submissions[team].filter(s => s.status === 'CORRECT').length)
        );
    }

    public wrongSubmissions(team: number): Observable<number> {
        return this.submissions.pipe(
            map(submissions => submissions[team].filter(s => s.status === 'WRONG').length)
        );
    }

    public indeterminate(team: number): Observable<number> {
        return this.submissions.pipe(
            map(submissions => submissions[team].filter(s => s.status === 'INDETERMINATE').length)
        );
    }
}

