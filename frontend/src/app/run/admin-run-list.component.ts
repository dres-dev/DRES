import { Component } from '@angular/core';
import { AbstractRunListComponent, RunInfoWithState } from './abstract-run-list.component';
import {
  ApiEvaluationInfo, ApiEvaluationState,
  CompetitionRunAdminService,
  CompetitionRunScoresService,
  DownloadService, EvaluationService,
} from '../../../openapi';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData,
} from '../shared/confirmation-dialog/confirmation-dialog.component';
import { forkJoin, merge, timer } from 'rxjs';
import { flatMap, map, switchMap } from 'rxjs/operators';
import RunStatusEnum = ApiEvaluationState.RunStatusEnum;

export interface RunInfoOverviewTuple {
  runInfo: ApiEvaluationInfo;
  overview: AdminRunOverview;
}

@Component({
  selector: 'app-admin-run-list',
  templateUrl: './admin-run-list.component.html',
})
export class AdminRunListComponent extends AbstractRunListComponent {
  constructor(
    runService: EvaluationService,
    runAdminService: CompetitionRunAdminService,
    scoreService: CompetitionRunScoresService,
    downloadService: DownloadService,
    router: Router,
    snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    super(runService, runAdminService, scoreService, downloadService, router, snackBar);
  }

  public start(runId: string) {
    this.runAdminService.postApiV1RunAdminWithRunidStart(runId).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public terminate(runId: string) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: 'You are about to terminate this run. This action cannot be undone. Do you want to proceed?',
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.runAdminService.postApiV1RunAdminWithRunidTerminate(runId).subscribe(
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

  public previousTask(runId: string) {
    this.runAdminService.postApiV1RunAdminWithRunidTaskPrevious(runId).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public abortTask(runId: string) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: 'Really abort the task?',
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.runAdminService.postApiV1RunAdminWithRunidTaskAbort(runId).subscribe(
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

  protected initStateUpdates() {
    this.runs = merge(timer(0, this.updateInterval), this.update).pipe(
      flatMap((t) => this.runService.getApiV1RunInfoList()),
      map((runInfo) =>
        runInfo.map((run) =>
          this.runAdminService.getApiV1RunAdminWithRunidOverview(run.id).pipe(
            map((overview) => {
              return {
                id: run.id,
                name: run.name,
                description: run.description,
                teams: run.teams.length,
                runStatus: overview.state,
                taskRunStatus: RunStatusEnum.ACTIVE, // FIXME how to handle async and sync?,
                currentTask: 'n/a',
                timeLeft: 'n/a',
                asynchronous: run.type === 'ASYNCHRONOUS',
                runProperties: run.properties,
              } as RunInfoWithState;
            })
          )
        )
      ),
      switchMap((runs$) => forkJoin(...runs$))
      // https://betterprogramming.pub/how-to-turn-an-array-of-observable-into-an-observable-of-array-in-angular-d6cfe42a72d4
    );
  }
}
