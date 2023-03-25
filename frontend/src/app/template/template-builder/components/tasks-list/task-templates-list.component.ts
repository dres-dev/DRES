import { Component, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { ApiEvaluationTemplate, ApiTaskGroup, ApiTaskTemplate, ApiTaskType } from "../../../../../../openapi";
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
  tasks = new Observable<ApiTaskTemplate[]>((o) => o.next([]));
  displayedColumns = ['name', 'group', 'type', 'duration', 'actions'];

  groups = new Observable<ApiTaskGroup[]>((o) => o.next([]));

  selection = new SelectionModel(false, [], false);


  constructor(builder: TemplateBuilderService) {
    super(builder);
  }

  ngOnInit(): void {
    this.onInit();
  }

  ngOnDestroy(): void {
    this.onDestroy();
  }

  public addTask(group: ApiTaskGroup){
    const type = this.builderService.getTemplate().taskTypes.find((v) => v.name === group.type);
    this.editorLauncher.editTask(type, group, null);
  }

  public editTask(task: ApiTaskTemplate){
    const index = this.builderService.getTemplate().tasks.indexOf(task);
    if(index > -1){
      // task exists
      this.selection.toggle(task);
      this.editorLauncher.editTask(
        this.builderService.getTemplate().taskTypes.find((v) => v.name === task.taskType),
        this.builderService.getTemplate().taskGroups.find((v) => v.name === task.taskGroup),
        task
      );
    }
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
    this.tasks = this.builderService.templateAsObservable().pipe(map((t) => {
      if(t){
        console.log("templates list: updated", t.tasks)
        return t.tasks;
      }else{
        return [];
      }
    }));
    this.taskTable?.renderRows();
    this.groups = this.builderService.templateAsObservable().pipe(map((t) => {
      if(t){
        return t.taskGroups;
      }else{
        return [];
      }
    }));

  }
}
