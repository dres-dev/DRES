import { Component, Input, OnInit } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { RunInfoOverviewTuple } from '../admin-run-list.component';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfig } from '../../app.config';
import { MatDialog } from '@angular/material/dialog';
import { take } from 'rxjs/operators';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData,
} from '../../shared/confirmation-dialog/confirmation-dialog.component';
import { NavigationService } from '../../services/navigation/navigation.service';
import {
  DownloadService,
  EvaluationAdministratorService,
  EvaluationScoresService,
  EvaluationService,
  TemplateService
} from '../../../../openapi';

@Component({
  selector: 'app-run-admin-toolbar',
  templateUrl: './run-admin-toolbar.component.html',
  styleUrls: ['./run-admin-toolbar.component.scss'],
})
export class RunAdminToolbarComponent implements OnInit {
  @Input() runId: BehaviorSubject<string> = new BehaviorSubject<string>('');
  @Input() run: Observable<RunInfoOverviewTuple>;
  @Input() update = new Subject();

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private config: AppConfig,
    private runService: EvaluationService,
    private competitionService: TemplateService,
    private runAdminService: EvaluationAdministratorService,
    private scoreService: EvaluationScoresService,
    private downloadService: DownloadService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private navigation: NavigationService
  ) {}

  public start() {
    const runId = this.runId.value;
    this.runAdminService.postApiV2EvaluationAdminByEvaluationIdStart(runId).subscribe(
      (r) => {
        this.update.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public terminate() {
    this.dialog
      .open(ConfirmationDialogComponent, {
        data: {
          text: 'You are about to terminate this run. This action cannot be udone. Do you want to prceed?',
          color: 'warn',
        } as ConfirmationDialogComponentData,
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          const runId = this.runId.value;
          this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTerminate(runId).subscribe(
            (r) => {
              this.update.next();
              this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
              this.navigation.back();
            },
            (r) => {
              this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
            }
          );
        }
      });
  }

  public navigateToViewer() {
    const runId = this.runId.value;

    /* TODO: Setup depends on type of competition run. */
    this.router.navigate([
      '/run/viewer',
      runId,
      {
        center: 'player',
        left: 'competition_score',
        right: 'task_type_score',
        bottom: 'team_score',
      },
    ]);
  }

  public navigateToJudgement() {
    const runId = this.runId.value;
    this.router.navigate(['/judge', runId]);
  }

  /**
   * Navigates to audience voting judgment viewer.
   *
   */
  public navigateToVoting() {
    const runId = this.runId.value;
    this.router.navigate(['/vote', runId]);
  }

  /**
   * Navigates to admin viewer (for admins).
   *
   * @param runId ID of the run to navigate to.
   */
  public navigateToAdmin() {
    const runId = this.runId.value;
    this.router.navigate(['/run/admin', runId]);
  }

  /**
   * Navigates to score history (for admins).
   *
   * @param runId ID of the run to navigate to.
   */
  public navigateToScoreHistory() {
    const runId = this.runId.value;
    this.router.navigate(['/run/scores', runId]);
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
        // FIXME httpHeaderAccept was text/csv -- might have to adjust openapi info
      .getApiV2DownloadEvaluationByEvaluationIdScores(runId, 'body', false, { httpHeaderAccept: 'text/plain' })
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

  ngOnInit(): void {}
}
