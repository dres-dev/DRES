import {Component, OnInit, ViewChild} from '@angular/core';
import {RouteBasedCompetitionAwareComponent} from '../shared/route-based-competition-aware.component';
import {CompetitionService, DownloadService, RestTeam, UserService} from '../../../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {AppConfig} from '../../../../app.config';
import {MatTable} from '@angular/material/table';
import {CompetitionBuilderTeamDialogComponent} from '../../competition-builder-team-dialog/competition-builder-team-dialog.component';
import {filter} from 'rxjs/operators';

@Component({
  selector: 'app-teams-list',
  templateUrl: './teams-list.component.html',
  styleUrls: ['./teams-list.component.scss']
})
export class TeamsListComponent extends RouteBasedCompetitionAwareComponent implements OnInit {

  displayedColumnsTeams: string[] = ['logo', 'name', 'action'];
  @ViewChild('teamTable')
  teamTable: MatTable<any>;

  constructor(competitionService: CompetitionService,
              private userService: UserService,
              route: ActivatedRoute,
              snackBar: MatSnackBar,
              private dialog: MatDialog,
              private config: AppConfig) {
    super(route, competitionService, snackBar);
  }

  ngOnInit(): void {
    this.onInit();
  }

  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(team: RestTeam): string {
    if (team.logoData != null) {
      return team.logoData;
    } else {
      return this.config.resolveApiUrl(`/competition/logo/${team.logoId}`);
    }
  }

  public addTeam() {
    const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, { width: '500px' });
    dialogRef
        .afterClosed()
        .pipe(filter((t) => t != null))
        .subscribe((t) => {
          this.competition.teams.push(t);
          this.dirty = true;
          this.teamTable.renderRows();
        });
  }

  public editTeam(team: RestTeam) {
    const index = this.competition.teams.indexOf(team);
    if (index > -1) {
      const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, { data: team, width: '500px' });
      dialogRef
          .afterClosed()
          .pipe(filter((t) => t != null))
          .subscribe((t: RestTeam) => {
            this.competition.teams[index] = t;
            this.dirty = true;
            this.teamTable.renderRows();
          });
    }
  }

  public removeTeam(team: RestTeam) {
    this.competition.teams.splice(this.competition.teams.indexOf(team), 1);
    this.dirty = true;
    this.teamTable.renderRows();
  }

}
