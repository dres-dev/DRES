import {AfterViewInit, Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AppConfig} from '../app.config';
import {
    CompetitionRunAdminService,
    CompetitionRunService,
    CompetitionService,
    PastTaskInfo,
    RestDetailedTeam,
    RunInfo,
    RunState, TaskInfo,
    ViewerInfo
} from '../../../openapi';
import {BehaviorSubject, combineLatest, merge, Observable, of, Subject, timer} from 'rxjs';
import {catchError, filter, flatMap, map, shareReplay, switchMap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {ConfirmationDialogComponent, ConfirmationDialogComponentData} from '../shared/confirmation-dialog/confirmation-dialog.component';
import {RunInfoOverviewTuple} from './admin-run-list.component';


export interface CombinedRun {
    info: RunInfo;
    state: RunState;
}

@Component({
    selector: 'app-run-admin-view',
    templateUrl: './run-admin-view.component.html',
    styleUrls: ['./run-admin-view.component.scss']
})
export class RunAdminViewComponent implements AfterViewInit {

    runId: Observable<string>;
    runIdAsSubject: BehaviorSubject<string> = new BehaviorSubject<string>('');
    run: Observable<CombinedRun>;
    runOverview: Observable<RunInfoOverviewTuple>;
    viewers: Observable<ViewerInfo[]>;
    update = new Subject();
    displayedColumnsTasks: string[] = ['name', 'group', 'type', 'duration', 'past', 'action'];
    teams: Observable<RestDetailedTeam[]>;
    pastTasks = new BehaviorSubject<PastTaskInfo[]>([]);
    pastTasksValue: PastTaskInfo[];

    constructor(private router: Router,
                private activeRoute: ActivatedRoute,
                private config: AppConfig,
                private runService: CompetitionRunService,
                private competitionService: CompetitionService,
                private runAdminService: CompetitionRunAdminService,
                private snackBar: MatSnackBar,
                private dialog: MatDialog) {
        this.runId = this.activeRoute.params.pipe(map(a => a.runId));
        this.runId.subscribe(this.runIdAsSubject);
        this.run = this.runId.pipe(
            switchMap(runId =>
                combineLatest([
                    this.runService.getApiV1RunWithRunidInfo(runId).pipe(
                        catchError((err, o) => {
                            console.log(`[RunAdminViewComponent] There was an error while loading information in the current run state: ${err?.message}`);
                            this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`, null, {duration: 5000});
                            if (err.status === 404) {
                                this.router.navigate(['/competition/list']);
                            }
                            return of(null);
                        }),
                        filter(q => q != null)
                    ),
                    merge(timer(0, 1000), this.update).pipe(
                        switchMap(index => this.runService.getApiV1RunWithRunidState(runId))
                    )
                ])
            ),
            map(([i, s]) => {
                return {info: i, state: s} as CombinedRun;
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading. */
        );
        this.runOverview = this.runId.pipe(
            switchMap(runId =>
                combineLatest([
                    this.runService.getApiV1RunWithRunidInfo(runId).pipe(
                        catchError((err, o) => {
                            console.log(`[RunAdminViewComponent] There was an error while loading information in the current run state: ${err?.message}`);
                            this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`, null, {duration: 5000});
                            if (err.status === 404) {
                                this.router.navigate(['/competition/list']);
                            }
                            return of(null);
                        }),
                        filter(q => q != null)
                    ),
                    merge(timer(0, 1000), this.update).pipe(
                        switchMap(index => this.runAdminService.getApiV1RunAdminWithRunidOverview(runId))
                    )
                ])
            ),
            map(([run, overview]) => {
                return {runInfo: run, overview} as RunInfoOverviewTuple;
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading. */
        );


        this.viewers = this.runId.pipe(
            flatMap(runId => timer(0, 1000).pipe(switchMap(i => this.runAdminService.getApiV1RunAdminWithRunidViewerList(runId))))
        );

        this.teams = this.run.pipe(
            switchMap(runAndInfo => {
                return this.competitionService.getApiV1CompetitionWithCompetitionidTeamListDetails(runAndInfo.info.competitionId);
            }),
            shareReplay({bufferSize: 1, refCount: true})
        );
    }

    public nextTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidTaskNext(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public previousTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidTaskPrevious(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public startTask() {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidTaskStart(id))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public abortTask() {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            data: {
                text: 'Really abort the task?',
                color: 'warn'
            } as ConfirmationDialogComponentData
        });
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidTaskAbort(id))).subscribe(
                    (r) => {
                        this.update.next();
                        this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
                    }, (r) => {
                        this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                    }
                );
            }
        });
    }

    public switchTask(idx: number) {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidTaskSwitchWithIdx(id, idx))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public submissionsOf(task) {
        this.runId.subscribe(r => {
            this.router.navigateByUrl(`run/admin/submissions/${r}/${task.id}`);
        });
    }

    public adjustDuration(duration: number) {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidAdjustWithDuration(id, duration))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public forceViewer(viewerId: string) {
        this.runId.pipe(switchMap(id => this.runAdminService.postApiV1RunAdminWithRunidViewerListWithVieweridForce(id, viewerId))).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public toFormattedTime(sec: number): string {
        const hours = Math.floor(sec / 3600);
        const minutes = Math.floor(sec / 60) % 60;
        const seconds = sec % 60;

        return [hours, minutes, seconds]
            .map(v => v < 10 ? '0' + v : v)
            .filter((v, i) => v !== '00' || i > 0)
            .join(':');
    }

    /**
     * Generates a URL for the logo of the team.
     */
    public teamLogo(team: RestDetailedTeam): string {
        return this.config.resolveApiUrl(`/competition/logo/${team.logoId}`);
    }

    resolveTeamByName(idx: number, item: RestDetailedTeam) {
        return item.name;
    }

    resolveCombinedRunByRunId(_: number, item: CombinedRun) {
        return item.info.id;
    }

    resolveTaskById(_: number, item: TaskInfo){
        return item.id;
    }

    resolveViewerById(_: number, item: ViewerInfo) {
        return item.viewersId;
    }

    userNameOf(user: string): Observable<string> {
        // if (user) {
        //     return this.userService.getApiV1serWithId(user).pipe(
        //         map(u => u.username),
        //         shareReplay(1)
        //     );
        // } else {
        return null;
        // }
    }


    ngAfterViewInit(): void {
        /* Cache past tasks initially */
        this.runId.subscribe(runId => {
            this.runAdminService.getApiV1RunAdminWithRunidTaskPastList(runId).subscribe(arr => this.pastTasksValue = arr);
        });

        /* On each update, update past tasks */
        this.update.subscribe(_ => {
            this.runId.subscribe(runId => {
                this.runAdminService.getApiV1RunAdminWithRunidTaskPastList(runId).subscribe(arr => this.pastTasksValue = arr);
            });
        });

        this.run.subscribe(r => {
            this.runAdminService.getApiV1RunAdminWithRunidTaskPastList(r.info.id).subscribe(arr => this.pastTasksValue = arr);
        });
    }
}
