import {combineLatest, merge, Observable, Subject, timer} from 'rxjs';
import {
    CompetitionRunAdminService,
    CompetitionRunScoresService,
    CompetitionRunService,
    DownloadService,
    RunProperties,
    RunState
} from '../../../openapi';
import {flatMap, map, take} from 'rxjs/operators';
import {Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';

export interface RunInfoWithState {
    id: string;
    name: string;
    description?: string;
    teams: number;
    runStatus: RunState.RunStatusEnum;
    taskRunStatus: RunState.TaskRunStatusEnum;
    currentTask?: string;
    timeLeft: string;
    asynchronous: boolean;
    runProperties: RunProperties;
}

export class AbstractRunListComponent {

    displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'timeLeft', 'description', 'teamCount'];
    runs: Observable<RunInfoWithState[]>;
    updateInterval = 5000; /* TODO: Make configurable. */
    update = new Subject();

    constructor(protected runService: CompetitionRunService,
                protected runAdminService: CompetitionRunAdminService,
                protected scoreService: CompetitionRunScoresService,
                protected downloadService: DownloadService,
                protected router: Router,
                protected snackBar: MatSnackBar) {
        this.initStateUpdates();
    }

    /**
     * Navigates to run viewer (for viewers and guests).
     *
     * @param runId ID of the run to navigate to.
     */
    public navigateToViewer(runId: string) {
        /* TODO: Setup depends on type of competition run. */
        this.router.navigate(['/run/viewer', runId,  {
            center: 'player',
            left: 'competition_score',
            right: 'task_type_score',
            bottom: 'team_score',
        }]);
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
     * Navigates to audience voting judgment viewer.
     *
     * @param runId ID of the run to navigate to.
     */
    public navigateToVoting(runId: string) {
        this.router.navigate(['/vote', runId]);
    }

    /**
     * Navigates to admin viewer (for admins).
     */
    public navigateToAdmin(runId: string, async: boolean = false) {
        this.router.navigate([`/run/admin${async ? '/async' : ''}`, runId]);
    }

    /**
     * Navigates to score history (for admins).
     *
     * @param runId ID of the run to navigate to.
     */
    public navigateToScoreHistory(runId: string) {
        this.router.navigate(['/run/scores', runId]);
    }

    public downloadScores(runId: string) {
        this.downloadService.getApiV1DownloadRunWithRunidScores(runId).subscribe(scoresCSV => {
            const csvBlob = new Blob([scoresCSV], {type: 'text/csv'});
            const fake = document.createElement('a');
            fake.href = URL.createObjectURL(csvBlob);
            fake.download = `scores-${runId}.csv`;
            fake.click();
            URL.revokeObjectURL(fake.href);
        });
    }

    public nextTask(runId: string) {
        this.runAdminService.postApiV1RunAdminWithRunidTaskNext(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public startTask(runId: string) {
        this.runAdminService.postApiV1RunAdminWithRunidTaskStart(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    scoreDownloadProvider = (runId: string) => {
        return this.downloadService.getApiV1DownloadRunWithRunidScores(runId, 'body', false, {httpHeaderAccept: 'text/csv'}).pipe(take(1));
    };

    scoreFileProvider = (name: string) => {
        return () => `scores-${name}.csv`;
    };

    downloadProvider = (runId) => {
        return this.downloadService.getApiV1DownloadRunWithRunid(runId)
            .pipe(take(1));
        // .toPromise();
    }

    fileProvider = (name: string) => {
        return () => name;
    }

    protected initStateUpdates() {
        /**
         * Creates a combined observable that updates the state in a regular interval and the info +
         * state whenever a manual update is triggered.
         */
        const query = combineLatest([this.runService.getApiV1RunInfoList(), this.runService.getApiV1RunStateList()]);
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
                        runStatus: s.runStatus,
                        taskRunStatus: s.taskRunStatus,
                        currentTask: s.currentTask?.name,
                        timeLeft: s.timeLeft > -1 ? `${Math.round(s.timeLeft)}s` : 'n/a',
                        asynchronous: v.type === 'ASYNCHRONOUS',
                        runProperties: v.properties
                    } as RunInfoWithState;
                });
            })
        );
    }
}
