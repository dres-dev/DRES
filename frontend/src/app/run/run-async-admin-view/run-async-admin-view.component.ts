import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { BehaviorSubject, combineLatest, forkJoin, merge, Observable, of, Subject, timer } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../../app.config';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { catchError, filter, map, shareReplay, switchMap, take } from 'rxjs/operators';
import { RunInfoOverviewTuple } from '../admin-run-list.component';
import { MatAccordion } from '@angular/material/expansion';
import {
  ApiTaskTemplateInfo,
  ApiTeam,
  ApiTeamInfo,
  ApiTeamTaskOverview,
  DownloadService,
  EvaluationAdministratorService,
  EvaluationClientService,
  EvaluationScoresService,
  EvaluationService,
  TemplateService,
} from '../../../../openapi';

@Component({
  selector: 'app-run-async-admin-view',
  templateUrl: './run-async-admin-view.component.html',
  styleUrls: ['./run-async-admin-view.component.scss'],
  standalone: false,
})
export class RunAsyncAdminViewComponent implements AfterViewInit, OnDestroy {
  @ViewChild(MatAccordion) accordion: MatAccordion;

  runId: BehaviorSubject<string> = new BehaviorSubject<string>('');
  run: Observable<RunInfoOverviewTuple>;
  update = new Subject();

  displayedColumnsTasks: string[] = ['name', 'comment', 'group', 'type', 'duration', 'past'];
  displayedColumnsTeamTasks: string[] = ['name', 'comment', 'state', 'group', 'type', 'duration', 'past', 'action'];
  teams: Observable<ApiTeamInfo[]>;
  taskSubmissionCounts: Observable<Map<string, number>>;
  pastTasks = new BehaviorSubject<ApiTaskTemplateInfo[]>([]);
  pastTasksValue: ApiTaskTemplateInfo[];
  nbOpenTeamOverviews = 0;

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private config: AppConfig,
    private runService: EvaluationClientService,
    private evaluationService: EvaluationService,
    private runAdminService: EvaluationAdministratorService,
    private scoreService: EvaluationScoresService,
    private downloadService: DownloadService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.activeRoute.params.pipe(map((a) => a.runId)).subscribe(this.runId);
    this.run = this.runId.pipe(
      switchMap((runId) =>
        combineLatest([
          this.evaluationService.getApiV2EvaluationByEvaluationIdInfo(runId).pipe(
            catchError((err, o) => {
              console.log(
                `[RunAdminViewComponent] There was an error while loading information in the current run state: ${err?.message}`
              );
              this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`);
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

    this.teams = this.run.pipe(
      map((runAndOverview) => {
        return runAndOverview.runInfo.teams;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading. */
    );

    this.taskSubmissionCounts = merge(timer(0, 15000), this.update).pipe(
      switchMap(() => this.run.pipe(take(1))),
      switchMap((run) => {
        const runId = this.runId.getValue();
        const templates = run?.runInfo?.taskTemplates ?? [];
        if (templates.length === 0) return of(new Map<string, number>());
        return forkJoin(
          templates.map((t) =>
            this.runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(runId, t.templateId).pipe(
              map((infos) => ({ key: t.templateId, count: infos.flatMap((i) => i.submissions).length })),
              catchError(() => of({ key: t.templateId, count: 0 }))
            )
          )
        ).pipe(map((results) => new Map(results.map((r) => [r.key, r.count]))));
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  public submissionsOf(task, property = 'id') {
    console.log('S of ', task);
    this.runId.subscribe((r) => {
      this.router.navigateByUrl(`evaluation/admin/submissions/${r}/${task[property]}`);
    });
  }

  public resolveTeamOverviewByTeamId(index: number, item: ApiTeamTaskOverview) {
    return item.teamId;
  }

  public resolveTeamById(index: number, item: ApiTeamInfo) {
    return item.id;
  }

  ngAfterViewInit(): void {
    /* Cache past tasks initially */
    this.runId.subscribe((runId) => {
      this.runAdminService
        .getApiV2EvaluationAdminByEvaluationIdTaskPastList(runId)
        .subscribe((arr) => (this.pastTasksValue = arr));
    });

    /* On each update, update past tasks */
    this.update.subscribe((_) => {
      this.runId.subscribe((runId) => {
        this.runAdminService
          .getApiV2EvaluationAdminByEvaluationIdTaskPastList(runId)
          .subscribe((arr) => (this.pastTasksValue = arr));
      });
    });

    this.run.subscribe((r) => {
      this.runAdminService
        .getApiV2EvaluationAdminByEvaluationIdTaskPastList(r.runInfo.id)
        .subscribe((arr) => (this.pastTasksValue = arr));
    });
  }

  public openAllTeamOverviews() {
    this.accordion.openAll();
    this.nbOpenTeamOverviews = 10;
  }

  public closeAllTeamOverviews() {
    this.accordion.closeAll();
    this.nbOpenTeamOverviews = 0;
  }

  ngOnDestroy(): void {
    this.update?.unsubscribe();
  }
}
