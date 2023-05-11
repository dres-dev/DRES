import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { MatDialog } from "@angular/material/dialog";
import { ApiTaskType } from "../../../../../../openapi";
import { Observable } from "rxjs";
import { filter, map } from "rxjs/operators";
import {
  CompetitionBuilderTaskTypeDialogComponent
} from "../../../../competition/competition-builder/competition-builder-task-type-dialog/competition-builder-task-type-dialog.component";
import {
  ActionableDynamicTable,
  ActionableDynamicTableActionType,
  ActionableDynamicTableColumnDefinition,
  ActionableDynamicTableColumnType
} from "../../../../shared/actionable-dynamic-table/actionable-dynamic-table.component";

@Component({
  selector: "app-task-types-list",
  templateUrl: "./task-types-list.component.html",
  styleUrls: ["./task-types-list.component.scss"]
})
export class TaskTypesListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

  public static TKIS_PRESET = {
    name: 'Textual Known Item Search',
    duration: 420,
    targetOption: "SINGLE_MEDIA_SEGMENT",
    scoreOption: "KIS",
    hintOptions: ["TEXT"],
    submissionOptions: ["NO_DUPLICATES", "LIMIT_CORRECT_PER_TEAM", "TEMPORAL_SUBMISSION"],
    taskOptions: ["HIDDEN_RESULTS"],
    configuration: {["LIMIT_CORRECT_PER_TEAM.limit"]: "1"}
  } as ApiTaskType;

  public static VKIS_PRESET = {
    name: 'Visual Known Item Search',
    duration: 300,
    targetOption: "SINGLE_MEDIA_SEGMENT",
    scoreOption: "KIS",
    hintOptions: ["VIDEO_ITEM_SEGMENT"],
    submissionOptions: ["NO_DUPLICATES", "LIMIT_CORRECT_PER_TEAM", "TEMPORAL_SUBMISSION"],
    taskOptions: [],
    configuration: {["LIMIT_CORRECT_PER_TEAM.limit"]: "1"}
  } as ApiTaskType;

  public static AVS_PRESET = {
    name: 'Ad-hoc Video Search',
    duration: 300,
    targetOption: "JUDGEMENT",
    scoreOption: "AVS",
    hintOptions: ["TEXT"],
    submissionOptions: ["NO_DUPLICATES", "TEMPORAL_SUBMISSION"],
    taskOptions: ["MAP_TO_SEGMENT"]
  } as ApiTaskType;

  public static LSC_PRSET = {
    name: 'Lifelog Search Challenge Topic',
    duration: 300,
    targetOption: "SINGLE_MEDIA_ITEM", // TODO MULTIPLE_MEDIA_ITEMS is missing
    scoreOption: "KIS",
    hintOptions: ["TEXT"],
    submissionOptions: ["NO_DUPLICATES", "LIMIT_CORRECT_PER_TEAM"],
    taskOptions: ["HIDDEN_RESULTS"],
    configuration: {["LIMIT_CORRECT_PER_TEAM.limit"]: "1"}
  } as ApiTaskType;

  types: Observable<ApiTaskType[]> = new Observable<ApiTaskType[]>((o) => o.next([]));
  tkisPreset = TaskTypesListComponent.TKIS_PRESET;
  vkisPreset = TaskTypesListComponent.VKIS_PRESET;
  avsPreset = TaskTypesListComponent.AVS_PRESET;

  lscPreset = TaskTypesListComponent.LSC_PRSET;

  columns: ActionableDynamicTableColumnDefinition[] = [
    {key: 'name', header: 'Name', property: 'name', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'duration', header: 'Duration', property: 'duration', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'target', header: 'Target', property: 'targetOption', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'hints', header: 'Hint Options', type: ActionableDynamicTableColumnType.CUSTOM},
    {key: 'submissions', header: 'Submission Options', type: ActionableDynamicTableColumnType.CUSTOM},
    {key: 'tasks', header: 'Task Options', type: ActionableDynamicTableColumnType.CUSTOM},
    {key: 'score', header: 'Score', property: 'scoreOption', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'actions', header: 'Actions', type: ActionableDynamicTableColumnType.ACTION, actions: [ActionableDynamicTableActionType.REMOVE],}
  ];

  displayedColumns= ['name', 'duration', 'target', 'hints','submissions', 'tasks','score', 'actions'];

  @ViewChild("typesTable") table: ActionableDynamicTable<ApiTaskType>;

  constructor(
    builder: TemplateBuilderService,
    private dialog: MatDialog
  ) {
    super(builder);
  }

  ngOnDestroy(): void {
    this.onDestroy();
  }

  ngOnInit(): void {
    this.onInit();
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

  //public remove = (type: ApiTaskType) => this.remove(type);

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

  public remove(taskType: ApiTaskType) {
    this.builderService.removeTaskType(taskType);
    this.table?.renderRows();
  }



}
