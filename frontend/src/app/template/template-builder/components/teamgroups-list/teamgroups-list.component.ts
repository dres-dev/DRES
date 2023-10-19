import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { TemplateBuilderService } from "../../template-builder.service";
import { ApiTaskType, ApiTeam, ApiTeamGroup, TemplateService } from "../../../../../../openapi";
import { Observable } from "rxjs";
import { filter, map, tap } from "rxjs/operators";
import { MatDialog } from "@angular/material/dialog";
import {
  ActionableDynamicTable,
  ActionableDynamicTableActionType,
  ActionableDynamicTableColumnDefinition,
  ActionableDynamicTableColumnType
} from "../../../../shared/actionable-dynamic-table/actionable-dynamic-table.component";
import { TeamgroupsDialogComponent } from "../teamgroups-dialog/teamgroups-dialog.component";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";

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
    route: ActivatedRoute,
    templateService: TemplateService,
    snackBar: MatSnackBar,
    private dialog: MatDialog
  ){
    super(builderService, route, templateService, snackBar);
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
      }),
      tap((_ => this.table?.renderRows()))
    );
  }

  public remove = (teamGroup: ApiTeamGroup) => this.removeTeamGroup(teamGroup);

  public edit = (teamGroup: ApiTeamGroup) => this.editTeamGroup(teamGroup);
  public removeTeamGroup(teamGroup: ApiTeamGroup){
    this.builderService.removeTeamGroup(teamGroup);
    this.table?.renderRows();
  }

  public trackById(index: number, teamGroup: ApiTeamGroup){
    return teamGroup?.id || String(index)
  }

  public add(){
    const dialogRef = this.dialog.open(TeamgroupsDialogComponent, {width: '600px',closeOnNavigation: false});
    dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t) => {
      this.builderService.getTemplate().teamGroups.push(t);
      this.builderService.update();
      this.table.renderRows();
    })
  }

  public editTeamGroup(teamGroup: ApiTeamGroup){
    const index = this.builderService.getTemplate().teamGroups.indexOf(teamGroup);
    if(index > -1){
      const dialogRef = this.dialog.open(TeamgroupsDialogComponent, {data: teamGroup, width: '600px', closeOnNavigation: false});
      dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t) => {
        this.builderService.getTemplate().teamGroups[index] = t;
        this.builderService.update();
        this.table.renderRows();
      })
    }
  }
}
