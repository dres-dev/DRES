import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { ApiTeam, ApiTeamGroup } from "../../../../../../openapi";
import { Observable } from "rxjs";
import { filter, map } from "rxjs/operators";
import { MatDialog } from "@angular/material/dialog";
import {
  ActionableDynamicTable,
  ActionableDynamicTableActionType,
  ActionableDynamicTableColumnDefinition,
  ActionableDynamicTableColumnType
} from "../../../../shared/actionable-dynamic-table/actionable-dynamic-table.component";
import { TeamgroupsDialogComponent } from "../teamgroups-dialog/teamgroups-dialog.component";

@Component({
  selector: 'app-teamgroups-list',
  templateUrl: './teamgroups-list.component.html',
  styleUrls: ['./teamgroups-list.component.scss']
})
export class TeamgroupsListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy{

  teamGroups : Observable<ApiTeamGroup[]> = new Observable<ApiTeamGroup[]>((o) => o.next([]))

  columns: ActionableDynamicTableColumnDefinition[] = [
    {key: 'name', header: 'Name', property: 'name', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'aggregation', header: 'Aggregation', property: 'aggregation', type: ActionableDynamicTableColumnType.TEXT},
    {key: 'actions', header: 'Actions', type: ActionableDynamicTableColumnType.ACTION, actions: [ActionableDynamicTableActionType.EDIT, ActionableDynamicTableActionType.REMOVE]}
  ]

  displayedColumns = ['name', 'aggregation', 'actions']

  @ViewChild("teamgroupsTable") table: ActionableDynamicTable<ApiTeamGroup>;

  constructor(
    builderService: TemplateBuilderService,
    private dialog: MatDialog
  ){
    super(builderService);
  }

  ngOnInit() {
    this.onInit();
  }

  ngOnDestroy() {
    this.onDestroy();
  }

  onChange() {
    this.teamGroups = this.builderService.templateAsObservable().pipe(
      map((t) => {
        if(t){
          return t.teamGroups;
        }else{
          return [];
        }
      })
    );
  }

  public remove(teamGroup: ApiTeamGroup){
    this.builderService.removeTeamGroup(teamGroup);
    this.table?.renderRows();
  }

  public trackById(index: number, teamGroup: ApiTeamGroup){
    return teamGroup?.id || String(index)
  }

  public add(){
    const dialogRef = this.dialog.open(TeamgroupsDialogComponent, {width: '600px'});
    dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t) => {
      this.builderService.getTemplate().teamGroups.push(t);
      this.builderService.update();
      this.table.renderRows();
    })
  }

  public edit(teamGroup: ApiTeamGroup){
    const index = this.builderService.getTemplate().teamGroups.indexOf(teamGroup);
    if(index > -1){
      const dialogRef = this.dialog.open(TeamgroupsDialogComponent, {data: teamGroup, width: '600px'});
      dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t) => {
        this.builderService.getTemplate().teamGroups[index] = teamGroup;
        this.builderService.update();
        this.table.renderRows();
      })
    }
  }
}
