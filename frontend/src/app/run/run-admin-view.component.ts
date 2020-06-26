import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {AppConfig} from '../app.config';
import {CompetitionRunAdminService, CompetitionRunService, RunInfo, RunState} from '../../../openapi';
import {combineLatest, merge, Observable, Subject, timer} from 'rxjs';
import {map, shareReplay, switchMap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';


export interface CombinedRun {
    info: RunInfo;
    state: RunState;
}

@Component({
    selector: 'app-run-admin-view',
    templateUrl: './run-admin-view.component.html',
    styleUrls: ['./run-admin-view.component.scss']
})
export class RunAdminViewComponent {

    runId: Observable<number>;
    run: Observable<CombinedRun>;
    update = new Subject();
    x
    /**
     *
     * @param activeRoute
     * @param config
     * @param runService
     * @param runAdminService
     * @param snackBar
     */
    constructor(private activeRoute: ActivatedRoute,
                private config: AppConfig,
                private runService: CompetitionRunService,
                private runAdminService: CompetitionRunAdminService,
                private snackBar: MatSnackBar
    ) {
        this.runId = this.activeRoute.params.pipe(map(a => a.runId));
        this.run = this.runId.pipe(
            switchMap(runId =>
                combineLatest([
                    this.runService.getApiRunInfoWithRunid(runId),
                    merge(timer(0, 5000), this.update).pipe(
                        switchMap(index => this.runService.getApiRunStateWithRunid(runId))
                    )
                ])
            ),
            map(([i, s]) => {
                return {info: i, state: s} as CombinedRun;
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading. */
        );
    }

    public start() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidStart(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public terminate() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidTerminate(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public nextTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidTaskNext(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public previousTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidTaskPrevious(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public startTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidTaskStart(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public abortTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidTaskAbort(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public switchTask(idx: number) {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiRunAdminWithRunidTaskSwitchWithIdx(id, idx))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }
}
