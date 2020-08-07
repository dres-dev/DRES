import {combineLatest, merge, Observable, Subject, timer} from 'rxjs';
import {CompetitionRunAdminService, CompetitionRunService, RunState} from '../../../openapi';
import {flatMap, map} from 'rxjs/operators';
import {Router} from '@angular/router';
import {RunInfo} from '../../../openapi/model/runInfo';


interface RunInfoWithState {
    id: string;
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
        const query = combineLatest([this.runService.getApiRunInfoList(), this.runService.getApiRunStateList()]);
        this.runs = merge(timer(0, this.updateInterval), this.update).pipe(
            flatMap(t => query),
            map(([info, state]) => {
                return info.map((v, i) => {
                    const s = state.find((_) => _.id === v.id);
                    return {
                        id: v.id,
                        name: v.name,
                        description: v.description,
                        teams: v.teams.length,
                        status: s.status,
                        currentTask: s.currentTask?.name,
                        timeLeft: s.timeLeft > -1 ? `${Math.round(s.timeLeft)}s` : 'n/a'
                    } as RunInfoWithState;
                });
            })
        );
    }

    /**
     * Navigates to run viewer (for viewers and guests).
     *
     * @param runId ID of the run to navigate to.
     */
    public navigateToViewer(runId: string) {
        this.router.navigate(['/run/viewer', runId]);
    }

    /**
     * Navigates to judgment viewer (for judges).
     *
     * @param runId ID of the run to navigate to.
     */
    public navigateToJudgement(runId: string) {
        this.router.navigate(['/judge', runId]);
    }

    /**
     * Navigates to admin viewer (for admins).
     *
     * @param runId ID of the run to navigate to.
     */
    public navigateToAdmin(runId: string) {
        this.router.navigate(['/run/admin', runId]);
    }
}
