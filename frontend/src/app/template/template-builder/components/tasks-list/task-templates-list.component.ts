import { Component, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { ApiEvaluationTemplate, ApiHint, ApiTarget, ApiTaskGroup, ApiTaskTemplate, ApiTaskType } from "../../../../../../openapi";
import { MatTable } from "@angular/material/table";
import { Observable } from "rxjs";
import { SelectionModel } from "@angular/cdk/collections";
import { map, tap } from "rxjs/operators";

export interface TaskTemplateEditorLauncher{
  editTask(taskType: ApiTaskType, taskGroup: ApiTaskGroup, task?: ApiTaskTemplate);
}

@Component({
  selector: "app-task-templates-list",
  templateUrl: "./task-templates-list.component.html",
  styleUrls: ["./task-templates-list.component.scss"]
})
export class TaskTemplatesListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

  @Input()
  editorLauncher: TaskTemplateEditorLauncher

  // TODO After dynact table fanciness (conditional multi component projection), rewrite to use dynact table

  @ViewChild('taskTable')
  taskTable: MatTable<ApiTaskTemplate>;
  tasks: Observable<ApiTaskTemplate[]>;
  displayedColumns = ['name', 'group', 'type', 'duration', 'actions'];

  groups : Observable<ApiTaskGroup[]>;

  selection = new SelectionModel(false, [], false);


  constructor(builder: TemplateBuilderService) {
    super(builder);
  }

  ngOnInit(): void {
    this.onInit();
    this.tasks = this.builderService.taskTemplatesAsObservable();
    this.groups = this.builderService.taskGroupsAsObservable();
  }

  ngOnDestroy(): void {
    this.onDestroy();
  }

  public logTasks(){
    console.log("TRIGGER", this.builderService.getTemplate().tasks)
  }

  public addTask(group: ApiTaskGroup){
    const newTask = new class implements ApiTaskTemplate {
      collectionId: string;
      duration: number;
      hints: Array<ApiHint>;
      id: string;
      name: string;
      targets: Array<ApiTarget>;
      taskGroup: string;
      taskType: string;
    };
    newTask.taskGroup = group.name;
    newTask.targets = [];
    newTask.hints = [];
    newTask.taskType = this.builderService.findTypeForGroup(group).name;
    this.builderService.selectTaskTemplate(newTask);
    this.selection.toggle(newTask);
  }

  public editTask(task: ApiTaskTemplate){
    this.builderService.selectTaskTemplate(task);
    this.selection.toggle(task);
  }

  public moveTaskUp(task: ApiTaskTemplate){
    const oldIndex = this.builderService.getTemplate().tasks.indexOf(task);
    if(oldIndex > 0){
      const buffer = this.builderService.getTemplate().tasks[oldIndex - 1];
      this.builderService.getTemplate().tasks[oldIndex - 1] = task;
      this.builderService.getTemplate().tasks[oldIndex] = buffer;
      this.builderService.update();
      this.taskTable.renderRows();
    }
  }

  public moveTaskDown(task: ApiTaskTemplate){
    const oldIndex = this.builderService.getTemplate().tasks.indexOf(task);
    if(oldIndex < this.builderService.getTemplate().tasks.length - 1){
      const buffer = this.builderService.getTemplate().tasks[oldIndex + 1];
      this.builderService.getTemplate().tasks[oldIndex + 1] = task;
      this.builderService.getTemplate().tasks[oldIndex] = buffer;
      this.builderService.update();
      this.taskTable.renderRows();
    }
  }

  public tasksLength(){
    return this.builderService.getTemplate().tasks.length;
  }

  public removeTask(task: ApiTaskTemplate){
    this.builderService.removeTask(task);
  }

  onChange() {
    this.taskTable?.renderRows();
  }
}
