import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../app.config';
import { combineLatest, merge, mergeMap, Observable, of, Subject, timer} from 'rxjs';
import { catchError, filter, map, scan, shareReplay, switchMap, take } from "rxjs/operators";
import { WebSocketService } from '../services/websocket.service';
import { ServerMessageType } from '../model/ws/server-message-type.enum';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { RunInfoOverviewTuple } from './admin-run-list.component';
import { mergeTeamOverview } from '../utilities/api.utilities';
import {
  ApiEvaluationInfo,
  ApiEvaluationOverview,
  ApiEvaluationState,
  ApiSubmissionInfo,
  ApiTaskTemplateInfo,
  ApiTeam,
  ApiTeamTaskOverview,
  ApiViewerInfo,
  EvaluationAdministratorService,
  EvaluationService,
  TemplateService,
} from '../../../openapi';

export interface CombinedRun {
  info: ApiEvaluationInfo;
  state: ApiEvaluationState;
}

@Component({
  selector: 'app-run-admin-view',
  templateUrl: './run-admin-view.component.html',
  styleUrls: ['./run-admin-view.component.scss'],
  standalone: false,
})
export class RunAdminViewComponent implements OnInit, OnDestroy {

  private static VIEWER_POLLING_FREQUENCY = 3 * 1000; //ms
  private static STATE_POLLING_FREQUENCY = 1 * 1000; //ms
  private static OVERVIEW_POLLING_FREQUENCY = 5 * 1000; //ms

  runId: Observable<string>;
  run: Observable<CombinedRun>;
  runOverview: Observable<RunInfoOverviewTuple>;
  viewers: Observable<ApiViewerInfo[]>;
  refreshSubject: Subject<void> = new Subject();
  displayedColumnsTasks: string[] = ['name', 'comment', 'group', 'type', 'duration', 'past', 'action'];
  teams: Observable<ApiTeam[]>;
  submissionsForPastTasks: Observable<Map<string, number>>;
  submissionsForCurrentTask: Observable<Map<string, number>>;

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private config: AppConfig,
    private runService: EvaluationService,
    private competitionService: TemplateService,
    private runAdminService: EvaluationAdministratorService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private wsService: WebSocketService
  ) {
    /* WS messages that signal the run's state may have changed (some carry a state diff
       directly, others — TASK_PREPARE, TASK_UPDATED, COMPETITION_UPDATE, COMPETITION_END —
       never do and always fall back to an HTTP fetch). */
    const taskStateWs$ = this.wsService.messages$.pipe(
      filter((msg) => [
        ServerMessageType.ServerMessageTypeEnum.TASK_START,
        ServerMessageType.ServerMessageTypeEnum.TASK_END,
        ServerMessageType.ServerMessageTypeEnum.TASK_PREPARE,
        ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
        ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
        ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
      ].includes(msg.type))
    );

    /* WS messages that may carry a full overview diff (task transitions, score changes),
       plus the two lifecycle types that never carry one and always need an HTTP refetch. */
    const overviewWs$ = this.wsService.messages$.pipe(
      filter((msg) => [
        ServerMessageType.ServerMessageTypeEnum.TASK_START,
        ServerMessageType.ServerMessageTypeEnum.TASK_END,
        ServerMessageType.ServerMessageTypeEnum.TASK_PREPARE,
        ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
        ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
      ].includes(msg.type))
    );

    /* WS messages that carry a single-team overview diff (a submission from that team). */
    const teamOverviewWs$ = this.wsService.messages$.pipe(
      filter((msg) => msg.type === ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED)
    );

    /* Fires only when a viewer actually connects or signals ready. */
    const viewerWs$ = this.wsService.messages$.pipe(
      filter((msg) => msg.type === ServerMessageType.ServerMessageTypeEnum.VIEWER_UPDATE)
    );

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
          merge(
            /* Safety fallback: full HTTP fetch every 30 s, on manual refresh, or after a
               WebSocket reconnect — any event missed while disconnected needs a full resync. */
            merge(timer(0, 30_000), this.refreshSubject, this.wsService.reconnected$).pipe(
              switchMap(() => this.runService.getApiV2EvaluationByEvaluationIdState(runId))
            ),
            /* Apply diff directly when the WS message carries state — no HTTP needed. */
            taskStateWs$.pipe(
              filter((msg) => msg.state != null),
              map((msg) => msg.state as ApiEvaluationState)
            ),
            /* Fallback HTTP for task-state events whose payload is absent. */
            taskStateWs$.pipe(
              filter((msg) => msg.state == null),
              switchMap(() => this.runService.getApiV2EvaluationByEvaluationIdState(runId))
            )
          ),
        ])
      ),
      map(([i, s]) => {
        return { info: i, state: s } as CombinedRun;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    /** Observable for list of past tasks. */
    this.submissionsForPastTasks = this.run.pipe(
      switchMap((s) =>
        this.runAdminService.getApiV2EvaluationAdminByEvaluationIdTaskPastList(s.info.id).pipe(
          catchError((err, o) => {
            console.log(`[RunAdminViewComponent] There was an error while loading the past task list: ${err?.message}`);
            return of(null);
          }),
          filter((pastTasks: Array<ApiTaskTemplateInfo>) => pastTasks != null),
          map((pastTasks: Array<ApiTaskTemplateInfo>) => {
            const map = new Map<string, number>();
            for (let p of pastTasks) {
              map.set(p.templateId, 0);
            }
            return map;
          })
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    /* Fires when a task-state event OR a new submission arrives — needed to keep the
       submission count current without waiting for the next 30 s poll. */
    const submissionWs$ = this.wsService.messages$.pipe(
      filter((msg) => msg.type === ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED)
    );

    /** Observable for list of submissions for current task. */
    this.submissionsForCurrentTask = this.run.pipe(
      switchMap((s) =>
        this.runAdminService
          .getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(s.info.id, s.state.taskTemplateId)
          .pipe(
            catchError((err, o) => {
              console.log(`[RunAdminViewComponent] There was an error while submissions for the running task: ${err?.message}`);
              return of(null);
            }),
            filter((q) => q != null),
            map((submissions: Array<ApiSubmissionInfo>) => {
              const map = new Map<string, number>();
              if (submissions.length > 0) {
                map.set(s.state.taskTemplateId, submissions[submissions.length - 1].submissions.length);
              } else {
                map.set(s.state.taskTemplateId, 0);
              }
              return map;
            })
          )
      ),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

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
          merge(
            /* Safety fallback: full HTTP fetch every 30 s, on manual refresh, whenever a
               relevant WS message arrives without a usable payload, or after a WebSocket
               reconnect — any event missed while disconnected needs a full resync. */
            merge(
              timer(0, 30_000),
              this.refreshSubject,
              this.wsService.reconnected$,
              overviewWs$.pipe(filter((msg) => msg.overview == null)),
              teamOverviewWs$.pipe(filter((msg) => msg.teamOverview == null))
            ).pipe(
              switchMap(() => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview(runId)),
              map((overview) => ({ full: overview } as { full?: ApiEvaluationOverview; team?: ApiTeamTaskOverview }))
            ),
            /* Apply diff directly when the WS message carries a full overview — no HTTP needed. */
            overviewWs$.pipe(
              filter((msg) => msg.overview != null),
              map((msg) => ({ full: msg.overview as ApiEvaluationOverview }))
            ),
            /* Apply a scoped single-team diff directly — no HTTP needed, and no need to touch
               any other team's overview. */
            teamOverviewWs$.pipe(
              filter((msg) => msg.teamOverview != null),
              map((msg) => ({ team: msg.teamOverview as ApiTeamTaskOverview }))
            )
          ).pipe(
            scan((acc: ApiEvaluationOverview, update: { full?: ApiEvaluationOverview; team?: ApiTeamTaskOverview }) => {
              if (update.full) {
                return update.full;
              }
              return acc ? mergeTeamOverview(acc, update.team) : acc;
            }, null as ApiEvaluationOverview),
            filter((overview) => overview != null)
          ),
        ])
      ),
      map(([run, overview]) => {
        return { runInfo: run, overview } as RunInfoOverviewTuple;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    this.viewers = this.runId.pipe(
      mergeMap((runId) => merge(timer(0, 30_000), viewerWs$, taskStateWs$, this.wsService.reconnected$).pipe(switchMap(() => this.runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList(runId))))
    );

    this.teams = this.run.pipe(
      switchMap((runAndInfo) => {
        //return runAndInfo.info.teams
        return this.competitionService.getApiV2TemplateByTemplateIdTeamList(runAndInfo.info.templateId);
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }
  ngOnInit(): void {
    this.runId.pipe(take(1)).subscribe((id) => this.wsService.connect(id));
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }

  stateFromCombined(combined: Observable<CombinedRun>): Observable<ApiEvaluationState>{
    return combined.pipe(map((c) => c.state))
  }

  public switchTask(idx: number) {
    this.runId
      .pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskSwitchByIdx(id, idx)))
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

  public submissionsOf(task) {
    console.log(task);
    this.runId.subscribe((r) => {
      this.router.navigateByUrl(`evaluation/admin/submissions/${r}/${task.templateId}`);
    });
  }

  public forceViewer(viewerId: string) {
    this.runId
      .pipe(
        switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdViewerListByViewerIdForce(id, viewerId))
      )
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
