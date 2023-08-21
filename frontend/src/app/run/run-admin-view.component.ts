import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../app.config';
import { combineLatest, merge, mergeMap, Observable, of, Subject, timer} from 'rxjs';
import { catchError, filter, map, shareReplay, switchMap } from "rxjs/operators";
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { RunInfoOverviewTuple } from './admin-run-list.component';
import {
    ApiEvaluationInfo,
    ApiEvaluationState, ApiSubmissionInfo, ApiTaskTemplateInfo, ApiTeam,
    ApiViewerInfo,
    EvaluationAdministratorService,
    EvaluationService,
    TemplateService
} from '../../../openapi';

export interface CombinedRun {
  info: ApiEvaluationInfo;
  state: ApiEvaluationState;
}

@Component({
  selector: 'app-run-admin-view',
  templateUrl: './run-admin-view.component.html',
  styleUrls: ['./run-admin-view.component.scss'],
})
export class RunAdminViewComponent {

  private static VIEWER_POLLING_FREQUENCY = 3 * 1000; //ms
  private static STATE_POLLING_FREQUENCY = 1 * 1000; //ms
  private static OVERVIEW_POLLING_FREQUENCY = 5 * 1000; //ms

  runId: Observable<string>;
  run: Observable<CombinedRun>;
  runOverview: Observable<RunInfoOverviewTuple>;
  viewers: Observable<ApiViewerInfo[]>;
  refreshSubject: Subject<void> = new Subject();
  displayedColumnsTasks: string[] = ['name', 'group', 'type', 'duration', 'past', 'action'];
  teams: Observable<ApiTeam[]>;
  submissionsForPastTasks: Observable<Map<string,number>>;
  submissionsForCurrentTask: Observable<Map<string,number>>;

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private config: AppConfig,
    private runService: EvaluationService,
    private competitionService: TemplateService,
    private runAdminService: EvaluationAdministratorService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.runId = this.activeRoute.params.pipe(map((a) => a.runId));
    this.run = this.runId.pipe(
      switchMap((runId) =>
        combineLatest([
          this.runService.getApiV2EvaluationByEvaluationIdInfo(runId).pipe(
            catchError((err, o) => {
              console.log(
                `[RunAdminViewComponent] There was an error while loading information in the current run state: ${err?.message}`
              );
              this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`, null, {
                duration: 5000,
              });
              if (err.status === 404) {
                this.router.navigate(['/template/list']);
              }
              return of(null);
            }),
            filter((q) => q != null)
          ),
          merge(timer(0, RunAdminViewComponent.STATE_POLLING_FREQUENCY), this.refreshSubject).pipe(switchMap((index) => this.runService.getApiV2EvaluationByEvaluationIdState(runId))),
        ])
      ),
      map(([i, s]) => {
        return { info: i, state: s } as CombinedRun;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    /** Observable for list of past tasks. */
    this.submissionsForPastTasks = this.run.pipe(
        switchMap(s => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdTaskPastList(s.info.id).pipe(
            catchError((err, o) => {
              console.log(`[RunAdminViewComponent] There was an error while loading the past task list: ${err?.message}`);
              return of(null);
            }),
            filter(((pastTasks: Array<ApiTaskTemplateInfo>) => pastTasks != null)),
            map((pastTasks: Array<ApiTaskTemplateInfo>) => {
              const map = new Map<string,number>();
              for (let p of pastTasks) {
                map.set(p.templateId, 0);
              }
              return map;
            })
        )),
        shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    /** Observable for list of submissions for current task. */
    this.submissionsForCurrentTask = this.run.pipe(
        switchMap(s => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(s.info.id, s.state.taskTemplateId).pipe(
            catchError((err, o) => {
              console.log(`[RunAdminViewComponent] There was an error while submissions for the running task: ${err?.message}`);
              return of(null);
            }),
            filter((q) => q != null),
            map((submissions: Array<ApiSubmissionInfo>) => {
              const map = new Map<string,number>();
              if (submissions.length > 0) {
                map.set(s.state.taskTemplateId, submissions[submissions.length - 1].submissions.length);
              } else {
                map.set(s.state.taskTemplateId, 0);
              }
              return map;
            })
        )),
        shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    )

    this.runOverview = this.runId.pipe(
      switchMap((runId) =>
        combineLatest([
          this.runService.getApiV2EvaluationByEvaluationIdInfo(runId).pipe(
            catchError((err, o) => {
              console.log(
                `[RunAdminViewComponent] There was an error while loading information in the current run state: ${err?.message}`
              );
              this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`, null, {
                duration: 5000,
              });
              if (err.status === 404) {
                this.router.navigate(['/template/list']);
              }
              return of(null);
            }),
            filter((q) => q != null)
          ),
          merge(timer(0, RunAdminViewComponent.OVERVIEW_POLLING_FREQUENCY), this.refreshSubject).pipe(
            switchMap((index) => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview(runId))
          ),
        ])
      ),
      map(([run, overview]) => {
        return { runInfo: run, overview } as RunInfoOverviewTuple;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    this.viewers = this.runId.pipe(
      mergeMap((runId) => timer(0, RunAdminViewComponent.VIEWER_POLLING_FREQUENCY).pipe(switchMap((i) => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList(runId))))
    );


    this.teams = this.run.pipe(
      switchMap((runAndInfo) => {
        //return runAndInfo.info.teams
        return this.competitionService.getApiV2TemplateByTemplateIdTeamList(runAndInfo.info.templateId);
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }
  stateFromCombined(combined: Observable<CombinedRun>): Observable<ApiEvaluationState>{
    return combined.pipe(map((c) => c.state))
  }



  public switchTask(idx: number) {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskSwitchByIdx(id, idx))).subscribe(
      (r) => {
        this.refreshSubject.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public submissionsOf(task) {
    console.log(task);
    this.runId.subscribe((r) => {
      this.router.navigateByUrl(`evaluation/admin/submissions/${r}/${task.templateId}`);
    });
  }

  public forceViewer(viewerId: string) {
    this.runId
      .pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdViewerListByViewerIdForce(id, viewerId)))
      .subscribe(
        (r) => {
          this.refreshSubject.next();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
  }


  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(team: ApiTeam): string {
    return this.config.resolveApiUrl(`/template/logo/${team.id}`);
  }

  resolveTeamByName(idx: number, item: ApiTeam) {
    return item.name;
  }

  resolveCombinedRunByRunId(_: number, item: CombinedRun) {
    return item.info.id;
  }

  resolveTaskById(_: number, item: ApiTaskTemplateInfo) {
    return item.templateId;
  }

  resolveViewerById(_: number, item: ApiViewerInfo) {
    return item.viewersId;
  }
}
