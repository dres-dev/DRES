import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';
import { ApiEndedEvaluationInfo, DownloadService, EvaluationAdministratorService } from '../../../openapi';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData,
} from '../shared/confirmation-dialog/confirmation-dialog.component';

/** Lists stopped evaluations and lets administrators export or permanently delete them. */
@Component({
  selector: 'app-ended-run-list',
  templateUrl: './ended-run-list.component.html',
  styleUrls: ['./ended-run-list.component.scss'],
  standalone: false,
})
export class EndedRunListComponent implements OnInit {
  displayedColumns = ['actions', 'name', 'template', 'type', 'started', 'ended', 'tasks', 'submissions', 'id'];
  runs: ApiEndedEvaluationInfo[] = [];
  loading = false;
  deleting = new Set<string>();

  constructor(
    private runAdminService: EvaluationAdministratorService,
    private downloadService: DownloadService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.runAdminService.getApiV2EvaluationAdminEndedList().subscribe({
      next: (runs) => {
        this.runs = runs;
        this.loading = false;
      },
      error: (r) => {
        this.loading = false;
        this.snackBar.open(`Error: ${r?.error?.description ?? r.message}`, null, { duration: 5000 });
      },
    });
  }

  delete(run: ApiEndedEvaluationInfo): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text:
          `Permanently delete the evaluation "${run.name}" and its ${run.submissionCount} submissions? ` +
          'Scores and submissions cannot be recovered. The reusable evaluation template is kept.',
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.deleting.add(run.evaluationId);
      this.runAdminService.deleteApiV2EvaluationAdminByEvaluationId(run.evaluationId).subscribe({
        next: (result) => {
          this.deleting.delete(run.evaluationId);
          this.snackBar.open(`Success: ${result.description}`, null, { duration: 5000 });
          this.refresh();
        },
        error: (r) => {
          this.deleting.delete(run.evaluationId);
          this.snackBar.open(`Error: ${r?.error?.description ?? r.message}`, null, { duration: 5000 });
          this.refresh();
        },
      });
    });
  }

  downloadProvider(runId: string): Observable<any> {
    return this.downloadService.getApiV2DownloadEvaluationByEvaluationId(runId).pipe(take(1));
  }

  fileProvider(name: string): () => string {
    const safeName = name.replace(/[\\/:*?"<>|]/g, '_');
    return () => `${safeName}.json`;
  }

  trackById(_: number, run: ApiEndedEvaluationInfo): string {
    return run.evaluationId;
  }
}
