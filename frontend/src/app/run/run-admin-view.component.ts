import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {AppConfig} from '../app.config';
import {CompetitionRunService, RunInfo, RunState} from '../../../openapi';
import {interval, Observable} from 'rxjs';
import {flatMap, map, shareReplay, switchMap, withLatestFrom} from 'rxjs/operators';


export interface CombinedRun {
    info: RunInfo;
    state: RunState;
}

@Component({
    selector: 'app-run-admin-view',
    templateUrl: './run-admin-view.component.html'
})
export class RunAdminViewComponent {

    run: Observable<CombinedRun>;

    /**
     *
     * @param activeRoute
     * @param config
     * @param runService
     */
    constructor(private activeRoute: ActivatedRoute, private config: AppConfig, private runService: CompetitionRunService) {
        this.run = this.activeRoute.params.pipe(
            map(a => a.runId),
            flatMap(runId =>
                this.runService.getApiRunInfoWithRunid(runId).pipe(
                    withLatestFrom(interval(5000).pipe(
                        switchMap(i => this.runService.getApiRunStateWithRunid(runId))
                    )),
                    map(([i, s]) => {
                        return {info: i, state: s} as CombinedRun;
                    })
                )
            ),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading. */
        );
    }
}
