import { Component, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import {
  ApiEvaluationTemplate,
  ApiHint,
  ApiTarget,
  ApiTaskGroup,
  ApiTaskTemplate,
  ApiTaskType,
  TemplateService
} from "../../../../../../openapi";
import { MatTable } from "@angular/material/table";
import { Observable, Subscription } from "rxjs";
import { SelectionModel } from "@angular/cdk/collections";
import { map, switchMap, tap } from "rxjs/operators";
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData
} from "../../../../shared/confirmation-dialog/confirmation-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";
import {CdkDragDrop, moveItemInArray} from '@angular/cdk/drag-drop';

export interface TaskTemplateEditorLauncher {
  editTask(taskType: ApiTaskType, taskGroup: ApiTaskGroup, task?: ApiTaskTemplate);
}

@Component({
  selector: "app-task-templates-list",
  templateUrl: "./task-templates-list.component.html",
  styleUrls: ["./task-templates-list.component.scss"]
})
export class TaskTemplatesListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

  @Input()
  editorLauncher: TaskTemplateEditorLauncher;

  // TODO After dynact table fanciness (conditional multi component projection), rewrite to use dynact table

  @ViewChild("taskTable")
  taskTable: MatTable<ApiTaskTemplate>;
  tasks: Observable<ApiTaskTemplate[]>;
  displayedColumns = ["name", "comment", "group", "type", "duration", "actions"];

  groups: Observable<ApiTaskGroup[]>;

  selection = new SelectionModel(false, [], false);


  private selectedTaskSub: Subscription;

  constructor(builder: TemplateBuilderService,
              route: ActivatedRoute,
              templateService: TemplateService,
              snackBar: MatSnackBar,
              private dialog: MatDialog) {
    super(builder,route,templateService,snackBar);
  }

  ngOnInit(): void {
    this.onInit();
    this.tasks = this.builderService.taskTemplatesAsObservable();
    this.groups = this.builderService.taskGroupsAsObservable();
    this.selectedTaskSub = this.builderService.selectedTaskTemplateAsObservable().subscribe((t) => {
      if (!t) {
        this.selection.clear();
      }
    });
  }

  ngOnDestroy(): void {
    this.onDestroy();
  }

  public logTasks() {
    console.log("TRIGGER", this.builderService.getTemplate().tasks);
  }

  public addTask(group: ApiTaskGroup) {
    const newTask = new class implements ApiTaskTemplate {
      collectionId: string;
      duration: number;
      hints: Array<ApiHint>;
      id: string;
      name: string;
      targets: Array<ApiTarget>;
      taskGroup: string;
      taskType: string;
      comment: string;
    };
    newTask.taskGroup = group.name;
    newTask.targets = [];
    newTask.hints = [];
    newTask.taskType = this.builderService.findTypeForGroup(group).name;
    this.builderService.selectTaskTemplate(newTask);
    this.selection.toggle(newTask);
  }

  public editTask(task: ApiTaskTemplate) {
    this.builderService.selectTaskTemplate(task);
    this.selection.toggle(task);
  }

  public tasksLength() {
    return this.builderService.getTemplate().tasks.length;
  }

  public removeTask(task: ApiTaskTemplate) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: "Really want to delete this task template?",
        color: "warn"
      } as ConfirmationDialogComponentData
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.builderService.removeTask(task);
      }
    });

  }

  onChange() {
    this.taskTable?.renderRows();
  }

  public dropTable(event: CdkDragDrop<any, any>) {
    moveItemInArray(this.builderService.getTemplate().tasks, event.previousIndex, event.currentIndex);
    this.builderService.update();
    this.taskTable.renderRows();
  }
}
