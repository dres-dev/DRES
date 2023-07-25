import { Component, HostListener, OnDestroy, OnInit, ViewChild } from "@angular/core";
import {AbstractTemplateBuilderComponent} from './components/abstract-template-builder.component';
import {DeactivationGuarded} from '../../services/can-deactivate.guard';
import {Observable, Subscription} from 'rxjs';
import { ApiTaskGroup, ApiTaskTemplate, ApiTaskType, DownloadService, TemplateService, UserService } from "../../../../openapi";
import {ActivatedRoute, Router, RouterStateSnapshot} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {TemplateBuilderService} from './template-builder.service';
import {take} from 'rxjs/operators';
import { TaskTemplateEditorLauncher } from "./components/tasks-list/task-templates-list.component";
import { TaskTemplateEditorComponent } from "./components/task-template-editor/task-template-editor.component";

@Component({
  selector: 'app-template-builder',
  templateUrl: './template-builder.component.html',
  styleUrls: ['./template-builder.component.scss']
})
export class TemplateBuilderComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded, TaskTemplateEditorLauncher {
  onChange() {

  }

  @ViewChild('taskTemplateEditor', {static: true}) taskEditor: TaskTemplateEditorComponent;


  changeSub: Subscription;

  isSaving=false;
  constructor(
      templateService: TemplateService,
      private userService: UserService,
      private downloadService: DownloadService,
      route: ActivatedRoute,
      private router: Router,
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

  }

  fileProvider = () => {
    return this.builderService.getTemplate()?.name ? this.builderService.getTemplate().name : 'evaluation-template-download.json'
  }

  downloadProvider = () => {
    if(this.builderService.hasTemplate()){
      return this.downloadService.getApiV2DownloadTemplateByTemplateId(this.builderService.getTemplate()?.id).pipe(take(1));
    }
  }

  public save(){
    // FIXME re-enable form validation. possibly on the form-builder?
    this.isSaving = true;
    console.log("save")
    this.templateService.patchApiV2TemplateByTemplateId(this.builderService.getTemplate().id, this.builderService.getTemplateCleaned()).subscribe((s) => {
      this.snackBar.open(s.description, null, {duration: 5000});
      this.builderService.unmarkDirty();
      console.log("TemplateBuilder: Saved successfully", this.builderService.isDirty())
      this.isSaving = false;
      this.refresh();
    }, (r) => {
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
