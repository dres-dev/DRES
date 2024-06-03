import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { ApiTeam, TemplateService, UserService } from "../../../../../../openapi";
import { MatTable, MatTableDataSource } from "@angular/material/table";
import { Observable, Subscription } from "rxjs";
import { TemplateBuilderService } from "../../template-builder.service";
import { MatDialog } from "@angular/material/dialog";
import { AppConfig } from "../../../../app.config";
import { filter, map, tap } from "rxjs/operators";
import { TeamBuilderDialogComponent } from "../team-builder-dialog/team-builder-dialog.component";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";
import { MatSort } from "@angular/material/sort";

@Component({
  selector: 'app-teams-list',
  templateUrl: './teams-list.component.html',
  styleUrls: ['./teams-list.component.scss']
})
export class TeamsListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, AfterViewInit {

  displayedColumns = ['logo', 'name', 'action'];

  dataSource: MatTableDataSource<ApiTeam>

  @ViewChild('teamTable')
  teamTable: MatTable<ApiTeam>;
  @ViewChild(MatSort) sort: MatSort;

  private updateSub: Subscription;

  constructor(
    builderService: TemplateBuilderService,
    route: ActivatedRoute,
    templateService: TemplateService,
    snackBar: MatSnackBar,
    private userService: UserService,
    private dialog: MatDialog,
    private config: AppConfig
  ) {
    super(builderService, route, templateService, snackBar);
    this.dataSource = new MatTableDataSource()
  }

  ngOnInit(): void {
    this.onInit();
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
  }

  onChange() {
    this.updateSub = this.builderService.templateAsObservable().subscribe(t => {
      if(t){
        this.dataSource.data = t.teams;
        this.teamTable?.renderRows();
      }
    })
  }

  ngOnDestroy() {
    this.onDestroy();
    this.updateSub.unsubscribe();
  }

  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(team: ApiTeam): string {
    if ( team.logoData != null) {
      return team.logoData;
    } else {
      return this.config.resolveApiUrl(`/template/logo/${team.id}`);
    }
  }

  public trackByTeamId(index: number, team: ApiTeam){
    return team.id;
  }

  public addTeam(){
    const dialogRef = this.dialog.open(TeamBuilderDialogComponent, {width: '600px',closeOnNavigation: false});
    dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t) => {
      this.builderService.getTemplate().teams.push(t);
      this.builderService.update();
      this.teamTable.renderRows();
    })
  }

  public editTeam(team: ApiTeam){
    const index = this.builderService.getTemplate().teams.indexOf(team);
    if(index > -1){
      const dialogRef = this.dialog.open(TeamBuilderDialogComponent, {data: team, width: '600px',closeOnNavigation: false});
      dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t)=>{
        this.builderService.getTemplate().teams[index] = t;
        this.builderService.update();
        this.teamTable.renderRows();
      })
    }
  }

  public removeTeam(team: ApiTeam){
    this.builderService.getTemplate().teams.splice(this.builderService.getTemplate().teams.indexOf(team), 1);
    this.builderService.update();
    this.teamTable.renderRows();
  }

}
