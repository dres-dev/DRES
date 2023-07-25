import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { MatDialog } from "@angular/material/dialog";
import { ApiTaskType, TemplateService } from "../../../../../../openapi";
import { Observable } from "rxjs";
import { filter, map, shareReplay } from "rxjs/operators";
import {
  CompetitionBuilderTaskTypeDialogComponent
} from "../../../../competition/competition-builder/competition-builder-task-type-dialog/competition-builder-task-type-dialog.component";
import {
  ActionableDynamicTable,
  ActionableDynamicTableActionType,
  ActionableDynamicTableColumnDefinition,
  ActionableDynamicTableColumnType
} from "../../../../shared/actionable-dynamic-table/actionable-dynamic-table.component";
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData
} from "../../../../shared/confirmation-dialog/confirmation-dialog.component";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";

@Component({
  selector: "app-task-types-list",
  templateUrl: "./task-types-list.component.html",
  styleUrls: ["./task-types-list.component.scss"]
})
export class TaskTypesListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {


  types: Observable<ApiTaskType[]> = new Observable<ApiTaskType[]>((o) => o.next([]));

  presets: Observable<ApiTaskType[]>= new Observable<ApiTaskType[]>((o) => o.next([]));

  columns: ActionableDynamicTableColumnDefinition[] = [
    {key: 'name', header: 'Name', property: 'name', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'duration', header: 'Duration', property: 'duration', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'target', header: 'Target', property: 'targetOption', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'hints', header: 'Hint Options', type: ActionableDynamicTableColumnType.CUSTOM},
    {key: 'submissions', header: 'Submission Options', type: ActionableDynamicTableColumnType.CUSTOM},
    {key: 'tasks', header: 'Task Options', type: ActionableDynamicTableColumnType.CUSTOM},
    {key: 'score', header: 'Score', property: 'scoreOption', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'actions', header: 'Actions', type: ActionableDynamicTableColumnType.ACTION, actions: [ActionableDynamicTableActionType.DOWNLOAD, ActionableDynamicTableActionType.REMOVE],}
  ];

  displayedColumns= ['name', 'duration', 'target', 'hints','submissions', 'tasks','score', 'actions'];

  @ViewChild("typesTable") table: ActionableDynamicTable<ApiTaskType>;

  constructor(
    builder: TemplateBuilderService,
    route: ActivatedRoute,
    templateService: TemplateService,
    snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    super(builder,route,templateService,snackBar);
  }

  ngOnDestroy(): void {
    this.onDestroy();
  }

  ngOnInit(): void {
    this.onInit();
    this.presets = this.templateService.getApiV2TemplateTypePresetsList().pipe(shareReplay(1))
  }

  onChange() {
    this.types = this.builderService.templateAsObservable().pipe(map((t) => {
      if (t) {
        return t.taskTypes;
      } else {
        return [];
      }
    }));
  }


  public addTaskType(type?: ApiTaskType) {
    const dialogRef = this.dialog.open(CompetitionBuilderTaskTypeDialogComponent, { data: type ?? null, width: "750px" });
    dialogRef.afterClosed()
      .pipe(filter((t) => t != null))
      .subscribe((t) => {
        this.builderService.getTemplate().taskTypes.push(t);
        this.builderService.update();
        this.table?.renderRows();
      });
  }

  public remove = (taskType: ApiTaskType) => {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {data: {text: "Are you sure to delete this task type? Deletion of task types causes associated task groups and tasks to be deleted as well."} as ConfirmationDialogComponentData})
    dialogRef.afterClosed().subscribe((s) => {
      if(s){
        this.removeTaskType(taskType);
      }
    })
  }

  public download = (taskType: ApiTaskType) => {
    const file = new Blob([JSON.stringify(taskType, null, ' ')], { type: "application/json" });
    const fake = document.createElement('a');
    fake.href = URL.createObjectURL(file);
    fake.download = `${taskType.name.replace(/\s/g, '-')}.json`
    fake.click();
    URL.revokeObjectURL(fake.href);
  }

  public removeTaskType(taskType: ApiTaskType) {
    this.builderService.removeTaskType(taskType);
    this.table?.renderRows();
  }



}
