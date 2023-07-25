import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { ApiTeam, TemplateService, UserService } from "../../../../../../openapi";
import { MatTable } from "@angular/material/table";
import { Observable } from "rxjs";
import { TemplateBuilderService } from "../../template-builder.service";
import { MatDialog } from "@angular/material/dialog";
import { AppConfig } from "../../../../app.config";
import { filter, map } from "rxjs/operators";
import { TeamBuilderDialogComponent } from "../team-builder-dialog/team-builder-dialog.component";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";

@Component({
  selector: 'app-teams-list',
  templateUrl: './teams-list.component.html',
  styleUrls: ['./teams-list.component.scss']
})
export class TeamsListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

  displayedColumns = ['logo', 'name', 'action'];

  @ViewChild('teamTable')
  teamTable: MatTable<ApiTeam>;

  teams: Observable<ApiTeam[]> = new Observable<ApiTeam[]>((o) => o.next([]));
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
  }

  ngOnInit(): void {
    this.onInit();
  }

  onChange() {
    this.teams = this.builderService.templateAsObservable().pipe(
      map((t) => {
        if (t) {
          return t.teams;
        } else {
          return [];
        }
      })
    );
  }

  ngOnDestroy() {
    this.onDestroy();
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
    const dialogRef = this.dialog.open(TeamBuilderDialogComponent, {width: '600px'});
    dialogRef.afterClosed().pipe(filter((t) => t != null)).subscribe((t) => {
      this.builderService.getTemplate().teams.push(t);
      this.builderService.update();
      this.teamTable.renderRows();
    })
  }

  public editTeam(team: ApiTeam){
    const index = this.builderService.getTemplate().teams.indexOf(team);
    if(index > -1){
      const dialogRef = this.dialog.open(TeamBuilderDialogComponent, {data: team, width: '600px'});
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
