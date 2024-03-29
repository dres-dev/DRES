import { Component, HostListener, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "./components/abstract-template-builder.component";
import { DeactivationGuarded } from "../../services/can-deactivate.guard";
import { forkJoin, Observable, Subscription } from "rxjs";
import {
  ApiEvaluation,
  ApiEvaluationTemplate, ApiEvaluationTemplateOverview,
  ApiTaskGroup,
  ApiTaskTemplate,
  ApiTaskType,
  DownloadService,
  TemplateService,
  UserService
} from "../../../../openapi";
import { ActivatedRoute, Router, RouterStateSnapshot } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";
import { TemplateBuilderService } from "./template-builder.service";
import { map, switchMap, take } from "rxjs/operators";
import { TaskTemplateEditorLauncher } from "./components/tasks-list/task-templates-list.component";
import { TaskTemplateEditorComponent } from "./components/task-template-editor/task-template-editor.component";
import { MatDialog } from "@angular/material/dialog";
import {
  TemplateImportDialogComponent,
  TemplateImportDialogData
} from "./components/template-import-dialog/template-import-dialog.component";
import { TemplateImportTreeBranch } from "./components/template-import-tree/template-import-tree.component";
import { instanceOfEvaluation, instanceOfTemplate } from "../../utilities/api.utilities";
import { UploadJsonButtonComponent } from "../../shared/upload-json-button/upload-json-button.component";
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData
} from "../../shared/confirmation-dialog/confirmation-dialog.component";
import { InformationDialogComponent } from "../../shared/information-dialog/information-dialog.component";

@Component({
  selector: 'app-template-builder',
  templateUrl: './template-builder.component.html',
  styleUrls: ['./template-builder.component.scss']
})
export class TemplateBuilderComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded, TaskTemplateEditorLauncher {
  onChange() {

  }

  @ViewChild('taskTemplateEditor', {static: true}) taskEditor: TaskTemplateEditorComponent;
  @ViewChild('jsonUpload', {static: true}) jsonUpload: UploadJsonButtonComponent;


  changeSub: Subscription;

  isSaving=false;
  constructor(
      templateService: TemplateService,
      private userService: UserService,
      private downloadService: DownloadService,
      route: ActivatedRoute,
      private router: Router,
      private dialg: MatDialog,
      snackBar: MatSnackBar,
      public builderService: TemplateBuilderService
) {
    super(builderService, route, templateService, snackBar);
  }

  canDeactivate(nextState?: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    return this.builderService.checkDirty();
  }

  ngOnDestroy(): void {
    this.onDestroy();
    this.changeSub?.unsubscribe();
    this.routeSub?.unsubscribe();
  }

  ngOnInit(): void {
    this.onInit();
    this.routeSub = this.route.params.subscribe( (p) => {
      this.templateService.getApiV2TemplateByTemplateId(p.templateId).subscribe((t) => {
          /* initialise from route */
          this.builderService.initialise(t);
        },
        (r) => {
          this.snackBar.open(`Error: ${r?.error?.description}`, null,{duration: 5000});
        });
    });
    this.jsonUpload.handler = (contents => this.onUpload(contents))
  }

  fileProvider = () => {
    return this.builderService.getTemplate()?.name ? this.builderService.getTemplate().name : 'evaluation-template-download.json'
  }

  downloadProvider = () => {
    if(this.builderService.hasTemplate()){
      return this.downloadService.getApiV2DownloadTemplateByTemplateId(this.builderService.getTemplate()?.id).pipe(take(1));
    }
  }
  private openImportDialog(templates: ApiEvaluationTemplate[]){
    const ownIdx = templates.map(it => it.id).indexOf(this.builderService.getTemplate().id)
    if(ownIdx !== -1){
      /* In case something went wrong, ownIdx results in -1, which with the splice would result in splicing the last element, which in this case we dont want */
      templates.splice(ownIdx,1)
    }
    const dialogref = this.dialg.open(TemplateImportDialogComponent, {width: '800px', data: {templates: templates, branches: TemplateImportTreeBranch.ALL} as TemplateImportDialogData})
    dialogref.afterClosed().subscribe( d => {
      this.onImport(d)
    })
  }
  public onUpload(contents: any){
    let tmplt: ApiEvaluationTemplate
    if(instanceOfTemplate(contents)){
      tmplt = contents as ApiEvaluationTemplate
    }else if(instanceOfEvaluation(contents)){
      tmplt = (contents as ApiEvaluation).template
    }else{
      this.snackBar.open(`Uploaded file was either not a JSON file or did not conform to the expected types (EvaluationTemplate or Evaluation)`, null, {duration: 5000})
      console.warn("Uploaded file does not conform to expected types")
      return
    }
    this.openImportDialog([tmplt]);
  }

  public import(){
    let templateList : Observable<ApiEvaluationTemplate[]>;
    templateList = this.templateService.getApiV2TemplateList().pipe(
      map(overviews => overviews.map(o => this.templateService.getApiV2TemplateByTemplateId(o.id))),
      switchMap(templateList => forkJoin(...templateList))
    );
    templateList.subscribe(templates => this.openImportDialog(templates))
  }



  public onImport(templateToImportFrom: ApiEvaluationTemplate){
    if(templateToImportFrom){
      this.builderService.importFrom(templateToImportFrom);
    }
  }

  public save(){
    if(!this.taskEditor?.isFormValid()){
      this.dialg.open(InformationDialogComponent, {width: '600px', data: {text: "There are validation errors on the task builder, please fix them before saving."} as ConfirmationDialogComponentData})
      return;
    }
    this.isSaving = true;
    console.log("save")
    this.templateService.patchApiV2TemplateByTemplateId(this.builderService.getTemplate().id, this.builderService.getTemplateCleaned()).subscribe((s) => {
      this.snackBar.open(s.description, null, {duration: 5000});
      this.builderService.unmarkDirty();
      console.log("TemplateBuilder: Saved successfully", this.builderService.isDirty())
      this.isSaving = false;
      this.refresh();
    }, (r) => {
      console.log(r)
      this.snackBar.open(`Error: ${r?.error?.description}`, null, {duration: 5000})
      this.isSaving = false
    }, () => {
      this.isSaving = false
    });
  }

  public back(){
    if(this.builderService.checkDirty()){
      this.router.navigate(['/template/list']);
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (!this.builderService.checkDirty()) {
      event.preventDefault();
      event.returnValue = '';
      return;
    }
    delete event.returnValue;
  }

  refresh() {
    if(this.builderService.checkDirty()){
      this.ngOnInit();
      this.builderService.selectTaskTemplate(null);
    }
  }

  editTask(taskType: ApiTaskType, taskGroup: ApiTaskGroup, task?: ApiTaskTemplate) {
    this.taskEditor.taskType = taskType;
    this.taskEditor.taskGroup = taskGroup;
    this.taskEditor.task = task;
    this.taskEditor.init();
  }

}
