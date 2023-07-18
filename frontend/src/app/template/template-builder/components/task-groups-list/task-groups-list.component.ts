import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { MatDialog } from "@angular/material/dialog";
import { filter, map, tap } from "rxjs/operators";
import { Observable } from "rxjs";
import { ApiTaskGroup } from "../../../../../../openapi";
import {
  CompetitionBuilderTaskGroupDialogComponent,
  CompetitionBuilderTaskGroupDialogData
} from "../../../../competition/competition-builder/competition-builder-task-group-dialog/competition-builder-task-group.component";
import {
  ActionableDynamicTable,
  ActionableDynamicTableActionType,
  ActionableDynamicTableColumnDefinition,
  ActionableDynamicTableColumnType,
} from "../../../../shared/actionable-dynamic-table/actionable-dynamic-table.component";
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData
} from "../../../../shared/confirmation-dialog/confirmation-dialog.component";

@Component({
  selector: "app-task-groups-list",
  templateUrl: "./task-groups-list.component.html",
  styleUrls: ["./task-groups-list.component.scss"]
})
export class TaskGroupsListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {
  groups: Observable<ApiTaskGroup[]>;

  columns: ActionableDynamicTableColumnDefinition[] = [
    {key: 'name', header: 'Name', property: 'name', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'type', header: 'Type', property: 'type', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'actions', header: 'Actions', type: ActionableDynamicTableColumnType.ACTION, actions: [ActionableDynamicTableActionType.REMOVE],}
  ];

  displayedColumns= ['name', 'type', 'actions'];

  @ViewChild("groupTable") groupTable: ActionableDynamicTable<ApiTaskGroup>;

  constructor(
    builderService: TemplateBuilderService,
    private dialog: MatDialog
  ) {
    super(builderService);
  }

  ngOnInit(): void {
    this.onInit();

  }

  ngOnDestroy(): void {
    this.onDestroy();
  }

  onChange() {
    this.groups = this.builderService.templateAsObservable().pipe(map((t) => {
      if (t) {
        return t.taskGroups;
      } else {
        return [];
      }
    }), tap((t) => this.groupTable?.renderRows()));
  }

  public remove = (group: ApiTaskGroup) => {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {data: {text: "Are you sure to delete this task group? Deletion of task groups causes associated tasks to be deleted as well."} as ConfirmationDialogComponentData})
    dialogRef.afterClosed().subscribe((s) => {
      if(s){
        this.removeTaskGroup(group);
      }
    })
  }

  public removeTaskGroup(group: ApiTaskGroup) {
    this.builderService.removeTaskGroup(group);
    this.groupTable.renderRows();
  }

  public addTaskGroup() {
    const dialogRef = this.dialog.open(CompetitionBuilderTaskGroupDialogComponent, {
      data: {
        types: this.builderService.getTemplate().taskTypes,
        group: null
      } as CompetitionBuilderTaskGroupDialogData, width: "600px"
    });
    dialogRef.afterClosed().pipe(filter((g) => g != null)).subscribe((g) => {
      this.builderService.getTemplate().taskGroups.push(g);
      this.builderService.update();
      this.groupTable.renderRows();
    });
  }

}
