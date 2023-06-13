import { AfterViewInit, Component } from "@angular/core";
import {
  ApiCreateEvaluation,
  ApiEvaluationOverview, ApiEvaluationStartMessage,
  DownloadService,
  EvaluationAdministratorService, RunProperties, SuccessStatus,
  TemplateService
} from "../../../../openapi";
import { Router } from "@angular/router";
import { MatDialog } from "@angular/material/dialog";
import { MatSnackBar } from "@angular/material/snack-bar";
import { TemplateCreateDialogComponent } from "../template-create-dialog/template-create-dialog.component";
import { filter, flatMap, take, tap } from "rxjs/operators";
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData
} from "../../shared/confirmation-dialog/confirmation-dialog.component";
import { EvaluationStartDialogComponent, EvaluationStartDialogResult } from "../evaluation-start-dialog/evaluation-start-dialog.component";

@Component({
  selector: 'app-template-list',
  templateUrl: './template-list.component.html',
  styleUrls: ['./template-list.component.scss']
})
export class TemplateListComponent implements AfterViewInit{


  displayedColumns = ['actions', 'id', 'name', 'description', 'nbTasks', 'nbTeams']
  templates: ApiEvaluationOverview[] = [];

  /** Map of runs that are currently being generated. */
  public waitingForRun = new Map<string, boolean>()

  constructor(
    private templateService: TemplateService,
    private evaluationAdminService: EvaluationAdministratorService,
    private downloadService: DownloadService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {  }

  public create(){
    const dialogRef = this.dialog.open(TemplateCreateDialogComponent, {width: '500px'});
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r !=null),
        flatMap((r: ApiCreateEvaluation) => {
          return this.templateService.postApiV2Template(r);
        })
      ).subscribe(
      (r: SuccessStatus) => {
        this.refresh();
        this.snackBar.open(`Success: ${r?.description}`, null, {duration: 5000});
      },
      (err) => {
        this.snackBar.open(`Error: ${err?.description}`, null, {duration: 5000});
      }
    )
  }

  public refresh(){
    this.templateService.getApiV2TemplateList().subscribe(
      (results: ApiEvaluationOverview[]) => {
        this.templates = results;
      },
      (err) => {
        this.templates = [];
        this.snackBar.open(`Error: ${err?.error?.description}`, null, {duration: 5000})
      }
    );
  }

  public createEvaluation(id: string){
  }

  ngAfterViewInit(): void {
    this.refresh();
  }

  public edit(templateId: string){
    this.router.navigate(['/template/builder', templateId]).then(s => {});
  }

  public delete(templateId: string){
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: `Do you really want to delete competition with ID ${templateId}?`,
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if(result){
        this.templateService.deleteApiV2TemplateByTemplateId(templateId).subscribe((r) => {
          this.refresh();
          this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
        }, (r) => {
          this.snackBar.open(`Error: ${r.error.description}`,null,{duration: 5000});
        })
      }
    })
  }

  public createRun(templateId: string){
    const dialogRef = this.dialog.open(EvaluationStartDialogComponent, {width: '500px'});
    dialogRef.afterClosed().pipe(
      filter((r) => r!= null),
      tap((r) => (this.waitingForRun[templateId] = true)),
      flatMap((r: EvaluationStartDialogResult) => {
        return this.evaluationAdminService.postApiV2EvaluationAdminCreate({
          templateId: templateId,
          name: r.name,
          type: r.type,
          properties: {
            participantCanView: r.participantsCanView,
            shuffleTasks: r.shuffleTasks,
            allowRepeatedTasks: r.allowRepeatedTasks,
            limitSubmissionPreviews: r.limit
          } as RunProperties // TODO Rename in BE to ApiEvaluationProperties
        } as ApiEvaluationStartMessage)
      })
    ).subscribe((r: SuccessStatus) => {
      this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
      this.waitingForRun[templateId] = false;
    },(r) => {
      this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      this.waitingForRun[templateId] = false;
    });
  }

  downloadProvider = (templateId) => {
    return this.downloadService.getApiV2DownloadTemplateByTemplateId(templateId).pipe(take(1));
  }

  fileProvider = (name: string) => {
    return () => name;
  }

  resolveEvaluationOverviewById(_: number, item: ApiEvaluationOverview){
    return `${item}`
  }
}
