import { AfterViewInit, Component } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CompetitionCreateDialogComponent } from './competition-create-dialog.component';
import { filter, flatMap, take, tap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { CompetitionStartDialogComponent, CompetitionStartDialogResult } from './competition-start-dialog.component';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData,
} from '../../shared/confirmation-dialog/confirmation-dialog.component';
import {
  ApiCreateEvaluation,
  ApiEvaluationOverview, ApiEvaluationStartMessage, ApiEvaluationTemplateOverview,
  DownloadService,
  EvaluationAdministratorService, RunProperties, SuccessStatus,
  TemplateService
} from "../../../../openapi";

/**
 * @deprecated Replaced by TemplateList
 */
@Component({
  selector: 'app-competition-list',
  templateUrl: './competition-list.component.html',
  styleUrls: ['./competition-list.component.scss'],
})
export class CompetitionListComponent implements AfterViewInit {
  /** */
  displayedColumns = ['actions', 'id', 'name', 'description', 'taskCount', 'teamCount'];
  competitions: ApiEvaluationTemplateOverview[] = [];
  waitingForRun = new Map<string, boolean>()

  constructor(
    private evaluationService: TemplateService,
    private runAdminService: EvaluationAdministratorService,
    private downloadService: DownloadService,
    private routerService: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  public create() {
    const dialogRef = this.dialog.open(CompetitionCreateDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        flatMap((r: ApiCreateEvaluation) => {
          return this.evaluationService.postApiV2Template(r);
        })
      )
      .subscribe(
        (r: SuccessStatus) => {
          this.refresh();
          this.snackBar.open(`Success: ${r?.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r?.error?.description}`, null, { duration: 5000 });
        }
      );
  }

  public createRun(id: string) {
    const dialogRef = this.dialog.open(CompetitionStartDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        tap((r) => (this.waitingForRun[id] = true)),
        flatMap((r: CompetitionStartDialogResult) => {
          const properties = {
            participantCanView: r.participantCanView,
            shuffleTasks: r.shuffleTasks,
            allowRepeatedTasks: r.allowRepeatedTasks,
            limitSubmissionPreviews: r.limit
          } as RunProperties; // ApiEvaluationProperties
          return this.runAdminService.postApiV2EvaluationAdminCreate({
            templateId: id,
            name: r.name,
            type: r.type,
            properties: properties,
          } as ApiEvaluationStartMessage);
        })
      )
      .subscribe(
        (r: SuccessStatus) => {
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          this.waitingForRun[id] = false;
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          this.waitingForRun[id] = false;
        }
      );
  }

  public edit(competitionId: string) {
    this.routerService.navigate(['/template/builder', competitionId]);
  }

  public delete(competitionId: string) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: `Do you really want to delete competition with ID ${competitionId}?`,
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.evaluationService.deleteApiV2TemplateByTemplateId(competitionId).subscribe(
          (r) => {
            this.refresh();
            this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          }
        );
      }
    });
  }

  public refresh() {
    this.evaluationService.getApiV2TemplateList().subscribe(
      (results: ApiEvaluationTemplateOverview[]) => {
        this.competitions = results;
      },
      (r) => {
        this.competitions = [];
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  ngAfterViewInit(): void {
    this.refresh();
  }

  downloadProvider = (competitionId) => {
    return this.downloadService.getApiV2DownloadTemplateByTemplateId(competitionId).pipe(take(1));
    // .toPromise();
  };

  fileProvider = (name: string) => {
    return () => name;
  };

  resolveCompetitionOverviewById(_: number, item: ApiEvaluationOverview) {
    return `${item}`; // FIXME re-add ID? or remove trackedBy
  }
}
