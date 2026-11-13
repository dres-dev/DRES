import { AfterViewInit, Component } from '@angular/core';
import {
  ApiCreateEvaluation,
  ApiEvaluation,
  ApiEvaluationOverview,
  ApiEvaluationTemplate,
  ApiEvaluationStartMessage,
  ApiEvaluationTemplateOverview,
  DownloadService,
  EvaluationAdministratorService,
  ApiRunProperties,
  SuccessStatus,
  TemplateService,
} from '../../../../openapi';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TemplateCreateDialogComponent } from '../template-create-dialog/template-create-dialog.component';
import { filter, take, tap } from 'rxjs/operators';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData,
} from '../../shared/confirmation-dialog/confirmation-dialog.component';
import {
  EvaluationStartDialogComponent,
  EvaluationStartDialogResult,
} from '../evaluation-start-dialog/evaluation-start-dialog.component';
import { mergeMap } from 'rxjs';
import { instanceOfEvaluation, instanceOfTemplate } from '../../utilities/api.utilities';

@Component({
  selector: 'app-template-list',
  templateUrl: './template-list.component.html',
  styleUrls: ['./template-list.component.scss'],
  standalone: false,
})
export class TemplateListComponent implements AfterViewInit {
  displayedColumns = ['actions', 'id', 'name', 'description', 'nbTasks', 'nbTeams'];
  templates: ApiEvaluationTemplateOverview[] = [];

  /** Map of runs that are currently being generated. */
  public waitingForRun = new Map<string, boolean>();

  constructor(
    private templateService: TemplateService,
    private evaluationAdminService: EvaluationAdministratorService,
    private downloadService: DownloadService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  public create() {
    const dialogRef = this.dialog.open(TemplateCreateDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        mergeMap((r: ApiCreateEvaluation) => {
          return this.templateService.postApiV2Template(r);
        })
      )
      .subscribe(
        (r: SuccessStatus) => {
          this.refresh();
          this.snackBar.open(`Success: ${r?.description}`, null, { duration: 5000 });
        },
        (err) => {
          this.snackBar.open(`Error: ${err?.description}`, null, { duration: 5000 });
        }
      );
  }

  /** Imports a template JSON or an evaluation export containing a template as a new template. */
  importHandler = (contents: unknown) => {
    const template = instanceOfTemplate(contents)
      ? contents
      : instanceOfEvaluation(contents)
        ? contents.template
        : null;
    if (!template) {
      this.snackBar.open('Error: The file does not contain an evaluation template.', null, { duration: 5000 });
      return;
    }
    this.templateService.postApiV2TemplateImport(template).subscribe({
      next: (imported) => {
        this.refresh();
        this.snackBar.open(`Successfully imported template '${imported.name}' (${imported.id}).`, null, { duration: 5000 });
      },
      error: (err) => {
        this.snackBar.open(`Error: ${err?.error?.description ?? err?.message}`, null, { duration: 5000 });
      },
    });
  };

  importErrorHandler = (_error: unknown) => {
    this.snackBar.open('Error: The selected file could not be read as JSON.', null, { duration: 5000 });
  };

  public refresh() {
    this.templateService.getApiV2TemplateList().subscribe({
      next: (results: ApiEvaluationTemplateOverview[]) => {
        this.templates = results;
      },
      error: (err) => {
        this.templates = [];
        this.snackBar.open(`Error: ${err?.error?.description}`, null, { duration: 5000 });
      },
    });
  }

  public createEvaluation(id: string) {}

  ngAfterViewInit(): void {
    this.refresh();
  }

  public edit(templateId: string) {
    this.router.navigate(['/template/builder', templateId]).then((s) => {});
  }

  public copy(templateId: string) {
    this.templateService.postApiV2TemplateByTemplateIdClone(templateId).subscribe(
      (value) => {
        this.refresh();
        this.snackBar.open(`Successfully copied ${templateId}`, null, { duration: 5000 });
      },
      (error) => {
        this.snackBar.open(`Error: ${error.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public delete(templateId: string) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: `Do you really want to delete competition with ID ${templateId}?`,
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.templateService.deleteApiV2TemplateByTemplateId(templateId).subscribe(
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

  public createRun(templateId: string) {
    const dialogRef = this.dialog.open(EvaluationStartDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        tap((r) => (this.waitingForRun[templateId] = true)),
        mergeMap((r: EvaluationStartDialogResult) => {
          return this.evaluationAdminService.postApiV2EvaluationAdminCreate({
            templateId: templateId,
            name: r.name,
            type: r.type,
            properties: {
              participantCanView: r.participantsCanView,
              shuffleTasks: r.shuffleTasks,
              allowRepeatedTasks: r.allowRepeatedTasks,
              limitSubmissionPreviews: r.limit,
            } as ApiRunProperties,
          } as ApiEvaluationStartMessage);
        })
      )
      .subscribe(
        (r: SuccessStatus) => {
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          this.waitingForRun[templateId] = false;
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          this.waitingForRun[templateId] = false;
        }
      );
  }

  downloadProvider = (templateId) => {
    return this.downloadService.getApiV2DownloadTemplateByTemplateId(templateId).pipe(take(1));
  };

  fileProvider = (name: string) => {
    return () => name;
  };

  resolveEvaluationOverviewById(_: number, item: ApiEvaluationOverview) {
    return `${item}`;
  }
}
