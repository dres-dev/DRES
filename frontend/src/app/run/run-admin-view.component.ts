import { AfterViewInit, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../app.config';
import { BehaviorSubject, combineLatest, merge, Observable, of, Subject, timer } from 'rxjs';
import { catchError, filter, flatMap, map, shareReplay, switchMap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData,
} from '../shared/confirmation-dialog/confirmation-dialog.component';
import { RunInfoOverviewTuple } from './admin-run-list.component';
import {
    ApiEvaluationInfo,
    ApiEvaluationState, ApiSubmissionInfo, ApiTaskTemplateInfo, ApiTeam,
    ApiTeamInfo,
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
  runId: Observable<string>;
  runIdAsSubject: BehaviorSubject<string> = new BehaviorSubject<string>('');
  run: Observable<CombinedRun>;
  runOverview: Observable<RunInfoOverviewTuple>;
  viewers: Observable<ApiViewerInfo[]>;
  update = new Subject();
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
    this.runId.subscribe(this.runIdAsSubject);
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
          merge(timer(0, 1000), this.update).pipe(switchMap((index) => this.runService.getApiV2EvaluationByEvaluationIdState(runId))),
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
                map.set(p.templateId, 0); // FIXME ApiTaskTemplateInfo doesn't have the information about number of submissions anymore
              }
              return map;
            })
        ))
    );

    /** Observable for list of submissions for current task. */
    this.submissionsForCurrentTask = this.run.pipe(
        switchMap(s => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(s.info.id, s.state.currentTemplate.templateId).pipe(
            catchError((err, o) => {
              console.log(`[RunAdminViewComponent] There was an error while submissions for the running task: ${err?.message}`);
              return of(null);
            }),
            filter((q) => q != null),
            map((submissions: Array<ApiSubmissionInfo>) => {
              const map = new Map<string,number>();
              if (submissions.length > 0) {
                map.set(s.state.currentTemplate.templateId, submissions[submissions.length - 1].submissions.length);
              } else {
                map.set(s.state.currentTemplate.templateId, 0);
              }
              return map;
            })
        ))
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
          merge(timer(0, 1000), this.update).pipe(
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
      flatMap((runId) => timer(0, 1000).pipe(switchMap((i) => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList(runId))))
    );

    this.teams = this.run.pipe(
      switchMap((runAndInfo) => {
        return this.competitionService.getApiV2TemplateByTemplateIdTeamList(runAndInfo.info.templateId);
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  public nextTask() {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskNext(id))).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public previousTask() {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskPrevious(id))).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public startTask() {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskStart(id))).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public abortTask() {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: 'Really end the task?',
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskAbort(id))).subscribe(
          (r) => {
            this.update.next();
            this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          }
        );
      }
    });
  }

  public switchTask(idx: number) {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskSwitchByIdx(id, idx))).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public submissionsOf(task) {
    this.runId.subscribe((r) => {
      this.router.navigateByUrl(`run/admin/submissions/${r}/${task.id}`);
    });
  }

  public adjustDuration(duration: number) {
    this.runId
      .pipe(switchMap((id) => this.runAdminService.patchApiV2EvaluationAdminByEvaluationIdAdjustByDuration(id, duration)))
      .subscribe(
        (r) => {
          this.update.next();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
  }

  public forceViewer(viewerId: string) {
    this.runId
      .pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdViewerListByViewerIdForce(id, viewerId)))
      .subscribe(
        (r) => {
          this.update.next();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
  }

  public toFormattedTime(sec: number): string {
    const hours = Math.floor(sec / 3600);
    const minutes = Math.floor(sec / 60) % 60;
    const seconds = sec % 60;

    return [hours, minutes, seconds]
      .map((v) => (v < 10 ? '0' + v : v))
      .filter((v, i) => v !== '00' || i > 0)
      .join(':');
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
