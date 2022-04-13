import {AfterViewInit, Component} from '@angular/core';
import {BehaviorSubject, combineLatest, merge, Observable, of, Subject, timer} from 'rxjs';
import {
    CompetitionRunAdminService,
    CompetitionRunScoresService,
    CompetitionRunService,
    CompetitionService,
    DownloadService,
    PastTaskInfo,
    RestDetailedTeam,
    TeamTaskOverview
} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {AppConfig} from '../../app.config';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {catchError, filter, map, shareReplay, switchMap} from 'rxjs/operators';
import {RunInfoOverviewTuple} from '../admin-run-list.component';

@Component({
    selector: 'app-run-async-admin-view',
    templateUrl: './run-async-admin-view.component.html',
    styleUrls: ['./run-async-admin-view.component.scss']
})
export class RunAsyncAdminViewComponent implements AfterViewInit {

    runId: BehaviorSubject<string> = new BehaviorSubject<string>('');
    run: Observable<RunInfoOverviewTuple>;
    update = new Subject();

    displayedColumnsTasks: string[] = ['name', 'group', 'type', 'duration', 'past', 'action'];
    displayedColumnsTeamTasks: string[] = ['name', 'state', 'group', 'type', 'duration', 'past', 'action'];
    teams: Observable<RestDetailedTeam[]>;
    pastTasks = new BehaviorSubject<PastTaskInfo[]>([]);
    pastTasksValue: PastTaskInfo[];

    constructor(private router: Router,
                private activeRoute: ActivatedRoute,
                private config: AppConfig,
                private runService: CompetitionRunService,
                private competitionService: CompetitionService,
                private runAdminService: CompetitionRunAdminService,
                private scoreService: CompetitionRunScoresService,
                private downloadService: DownloadService,
                private snackBar: MatSnackBar,
                private dialog: MatDialog) {
        this.activeRoute.params.pipe(map(a => a.runId)).subscribe(this.runId);
        this.run = this.runId.pipe(
            switchMap(runId =>
                combineLatest([
                    this.runService.getApiV1RunWithRunidInfo(runId).pipe(
                        catchError((err, o) => {
                            console.log(`[RunAdminViewComponent] There was an error while loading information in the current run state: ${err?.message}`);
                            this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`);
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

        this.teams = this.run.pipe(
            switchMap(runAndOverview => {
                return this.competitionService.getApiV1CompetitionWithCompetitionidTeamListDetails(runAndOverview.runInfo.competitionId);
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading. */
        );
    }

    public submissionsOf(task, property = 'id') {
        this.runId.subscribe(r => {
            this.router.navigateByUrl(`run/admin/submissions/${r}/${task[property]}`);
        });
    }

    public resolveTeamOverviewByTeamId(index: number, item: TeamTaskOverview) {
        return item.teamId;
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
            this.runAdminService.getApiV1RunAdminWithRunidTaskPastList(r.runInfo.id).subscribe(arr => this.pastTasksValue = arr);
        });
    }


}
