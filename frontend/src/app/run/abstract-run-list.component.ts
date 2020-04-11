import {interval, merge, Observable, Subject} from 'rxjs';
import {CompetitionRunAdminService, CompetitionRunService} from '../../../openapi';
import {map, switchMap, withLatestFrom} from 'rxjs/operators';
import {Router} from '@angular/router';
import {RunState} from '../../../openapi';
import {AfterViewInit, OnInit} from '@angular/core';


interface RunInfoWithState {
    id: number;
    name: string;
    description?: string;
    teams: number;
    status: RunState.StatusEnum;
    currentTask?: string;
    timeLeft: string;
}

export class AbstractRunListComponent implements AfterViewInit {

    displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'timeLeft', 'description', 'teamCount'];
    runs: Observable<RunInfoWithState[]>;

    protected update = new Subject();

    constructor(protected runService: CompetitionRunService,
                protected runAdminService: CompetitionRunAdminService,
                protected router: Router) {


        /**
         * Creates a combined observable that updates the state in a regular interval and the info +
         * state whenever a manual update is triggered.
         */
        const infoUpdate = this.update.pipe(
            switchMap(() => this.runService.getApiRunInfo())
        );
        const stateUpdate = merge(this.update, interval(5000)).pipe(
            switchMap(() => this.runService.getApiRunState())
        );

        this.runs = stateUpdate.pipe(
            withLatestFrom(infoUpdate),
            map(([state, info]) => {
                return info.map(i => {
                    const s = state.find((_) => _.id === _.id);
                    return {
                        id: i.id,
                        name: i.name,
                        description: i.description,
                        teams: i.teams.length,
                        status: s.status,
                        currentTask: s.currentTask?.name,
                        timeLeft: s.timeLeft > -1 ? `${Math.round(s.timeLeft / 1000)}s` : 'n/a'
                    } as RunInfoWithState;
                });
            })
        );
    }

    /**
     * Reloads the RunInfo once the view has been initialized.
     */
    ngAfterViewInit(): void {
        this.update.next();
    }


    /**
     *
     * @param runId
     */
    public navigateToViewer(runId: number) {
        this.router.navigate(['/run/viewer', runId]);
    }
}
