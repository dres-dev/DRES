import {combineLatest, merge, mergeMap, Observable, Subject, timer} from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ApiRunProperties,
  ApiTaskStatus, DownloadService,
  EvaluationAdministratorService,
  EvaluationScoresService,
  EvaluationService, RunManagerStatus
} from "../../../openapi";

export interface RunInfoWithState {
  id: string;
  name: string;
  description?: string;
  teams: number;
  runStatus: RunManagerStatus;
  taskRunStatus?: ApiTaskStatus;
  currentTask?: string;
  currentTaskName?: string;
  timeLeft: string;
  asynchronous: boolean;
  runProperties: ApiRunProperties;
}

export class AbstractRunListComponent {
  displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'timeLeft', 'description', 'teamCount'];
  runs: Observable<RunInfoWithState[]>;
  updateInterval = 5000; /* TODO: Make configurable. */
  refreshSubject: Subject<void> = new Subject();

  postRefresh: () => void = () => {};

  constructor(
    protected runService: EvaluationService,
    protected runAdminService: EvaluationAdministratorService,
    protected scoreService: EvaluationScoresService,
    protected downloadService: DownloadService,
    protected router: Router,
    protected snackBar: MatSnackBar
  ) {
    this.initStateUpdates();
  }

  /**
   * Navigates to run viewer (for viewers and guests).
   *
   * @param runId ID of the run to navigate to.
   */
  public navigateToViewer(runId: string) {
    console.log("Navigate (AbstractList): ", runId)
    /* TODO: Setup depends on type of competition run. */
    this.router.navigate([
      '/evaluation/viewer',
      runId,
      {
        center: 'player',
        left: 'competition_score',
        right: 'task_type_score',
        bottom: 'team_score',
      },
    ]);
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
    this.router.navigate([`/evaluation/admin${async ? '/async' : ''}`, runId]);
  }

  /**
   * Navigates to score history (for admins).
   *
   * @param runId ID of the run to navigate to.
   */
  public navigateToScoreHistory(runId: string) {
    this.router.navigate(['/evaluation/scores', runId]);
  }

  public downloadScores(runId: string) {
    this.downloadService.getApiV2DownloadEvaluationByEvaluationIdScores(runId).subscribe((scoresCSV) => {
      const csvBlob = new Blob([scoresCSV], { type: 'text/csv' });
      const fake = document.createElement('a');
      fake.href = URL.createObjectURL(csvBlob);
      fake.download = `scores-${runId}.csv`;
      fake.click();
      URL.revokeObjectURL(fake.href);
    });
  }

  scoreDownloadProvider = (runId: string) => {
    return this.downloadService
      .getApiV2DownloadEvaluationByEvaluationIdScores(runId, 'body', false, { httpHeaderAccept: 'text/plain' }) // FIXME was text/css, might require openapi specs adjustment
      .pipe(take(1));
  };

  scoreFileProvider = (name: string) => {
    return () => `scores-${name}.csv`;
  };

  downloadProvider = (runId) => {
    return this.downloadService.getApiV2DownloadEvaluationByEvaluationId(runId).pipe(take(1));
    // .toPromise();
  };

  fileProvider = (name: string) => {
    return () => name;
  };

  resolveRunWithStateById(_: number, item: RunInfoWithState) {
    return item.id;
  }

  public refresh(){
    this.initStateUpdates();
    this.postRefresh()
  }

  protected initStateUpdates() {
    /**
     * Creates a combined observable that updates the state in a regular interval and the info +
     * state whenever a manual update is triggered.
     */
    const query = combineLatest([this.runService.getApiV2EvaluationInfoList(), this.runService.getApiV2EvaluationStateList()]);
    this.runs = merge(timer(0, this.updateInterval), this.refreshSubject).pipe(
      mergeMap((t) => query),
      map(([info, state]) => {
        return info.map((v, i) => {
          const s = state.find((_) => _.evaluationId === v.id);
          return {
            id: v.id,
            name: v.name,
            description: v.templateDescription,
            teams: v.teams.length,
            runStatus: s.evaluationStatus,
            taskRunStatus: s.taskStatus,
            currentTask: s.taskTemplateId,
            currentTaskName: v.taskTemplates.find(it => it.templateId === s.taskTemplateId)?.name,
            timeLeft: s.timeLeft > -1 ? `${Math.round(s.timeLeft)}s` : 'n/a',
            timeElapsed: s.timeElapsed,
            asynchronous: v.type === 'ASYNCHRONOUS',
            runProperties: v.properties,
          } as RunInfoWithState;
        });
      })
    );
  }
}
