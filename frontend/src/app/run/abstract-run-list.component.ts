import {combineLatest, merge, Observable, Subject, timer} from 'rxjs';
import {CompetitionRunAdminService, CompetitionRunService, RunState} from '../../../openapi';
import {flatMap, map} from 'rxjs/operators';
import {Router} from '@angular/router';


interface RunInfoWithState {
    id: number;
    name: string;
    description?: string;
    teams: number;
    status: RunState.StatusEnum;
    currentTask?: string;
    timeLeft: string;
}

export class AbstractRunListComponent {

    displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'timeLeft', 'description', 'teamCount'];
    runs: Observable<RunInfoWithState[]>;
    updateInterval = 5000; /* TODO: Make configurable. */
    update = new Subject();
    constructor(protected runService: CompetitionRunService,
                protected runAdminService: CompetitionRunAdminService,
                protected router: Router) {

        /**
         * Creates a combined observable that updates the state in a regular interval and the info +
         * state whenever a manual update is triggered.
         */
        const query = combineLatest([this.runService.getApiRunInfo(), this.runService.getApiRunState()]);
        this.runs = merge(timer(0, this.updateInterval), this.update).pipe(
            flatMap(t => query),
            map(([info, state]) => {
                return info.map(i => {
                    const s = state.find((_) => _.id === _.id);
                    return {
                        id: i.id,
                        name: i.name,
                        description: i.description,
                        teams: i.teams.length,
                        status: s.status,
                        currentTask: s.currentTask?.name,
                        timeLeft: s.timeLeft > -1 ? `${Math.round(s.timeLeft)}s` : 'n/a'
                    } as RunInfoWithState;
                });
            })
        );
    }

    /**
     *
     * @param runId
     */
    public navigateToViewer(runId: number) {
        this.router.navigate(['/run/viewer', runId]);
    }
}
