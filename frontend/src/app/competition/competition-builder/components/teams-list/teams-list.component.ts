import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {RestCompetitionDescription, RestTeam, UserService} from '../../../../../../openapi';
import {MatDialog} from '@angular/material/dialog';
import {AppConfig} from '../../../../app.config';
import {MatTable} from '@angular/material/table';
import {CompetitionBuilderTeamDialogComponent} from '../../competition-builder-team-dialog/competition-builder-team-dialog.component';
import {filter, map} from 'rxjs/operators';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {Observable} from 'rxjs';
import {CompetitionBuilderService} from '../../competition-builder.service';

@Component({
    selector: 'app-teams-list',
    templateUrl: './teams-list.component.html',
    styleUrls: ['./teams-list.component.scss']
})
export class TeamsListComponent extends AbstractCompetitionBuilderComponent implements OnInit, OnDestroy {

    displayedColumnsTeams: string[] = ['logo', 'name', 'action'];
    @ViewChild('teamTable')
    teamTable: MatTable<RestTeam>;

    teams: Observable<Array<RestTeam>> = new Observable<Array<RestTeam>>((o) => o.next([]));

    constructor(builderService: CompetitionBuilderService,
                private userService: UserService,
                private dialog: MatDialog,
                private config: AppConfig) {
        super(builderService);
    }

    ngOnInit(): void {
        this.onInit();
    }

    onChange(competition: RestCompetitionDescription) {
        this.teams = new Observable<Array<RestTeam>>((o) => {
            if(competition){
                o.next(competition.teams)
            }else{
                o.next([])
            }
        })
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
        const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, {width: '500px'});
        dialogRef
            .afterClosed()
            .pipe(filter((t) => t != null))
            .subscribe((t) => {
                this.competition.teams.push(t);
                this.update();
                this.teamTable.renderRows();
            });
    }

    public editTeam(team: RestTeam) {
        const index = this.competition.teams.indexOf(team);
        if (index > -1) {
            const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, {data: team, width: '500px'});
            dialogRef
                .afterClosed()
                .pipe(filter((t) => t != null))
                .subscribe((t: RestTeam) => {
                    this.competition.teams[index] = t;
                    this.update();
                    this.teamTable.renderRows();
                });
        }
    }

    public removeTeam(team: RestTeam) {
        this.competition.teams.splice(this.competition.teams.indexOf(team), 1);
        this.update();
        this.teamTable.renderRows();
    }

    ngOnDestroy() {
        this.onDestroy();
    }

}
