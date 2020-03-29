import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {filter, first, flatMap, tap} from 'rxjs/operators';
import {Competition, CompetitionOverview, CompetitionService, Team} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {FormControl, FormGroup} from '@angular/forms';
import {Observable, Subscription} from 'rxjs';
import {MatDialog} from '@angular/material/dialog';
import {CompetitionCreateDialogComponent, CompetitionCreateDialogResult} from '../competition-list/competition-create-dialog.component';
import {
  CompetitionBuilderAddTeamDialogComponent,
  CompetitionBuilderAddTeamDialogResult
} from './competition-builder-add-team-dialog.component';
import {MatListOption} from '@angular/material/list';

@Component({
  selector: 'app-competition-builer',
  templateUrl: './competition-builer.component.html',
  styleUrls: ['./competition-builer.component.scss']
})
export class CompetitionBuilerComponent implements OnInit, OnDestroy {

  competitionId: number;
  competition: Competition;
  form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});
  dirty = false;
  routeSubscription: Subscription;
  changeSubscription: Subscription;

  constructor(private competitionService: CompetitionService,
              private route: ActivatedRoute,
              private routerService: Router,
              private snackBar: MatSnackBar,
              private dialog: MatDialog) { }

  ngOnInit() {
    this.routeSubscription = this.route.params.subscribe(p => {
      this.competitionId = +(p.competitionId);
      this.refresh();
    });
    this.changeSubscription = this.form.valueChanges.subscribe(() => {
      this.dirty = true;
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription.unsubscribe();
    this.changeSubscription.unsubscribe();
  }

  public save() {}

  public back() {
    if (this.checkDirty()) {
      this.routerService.navigate(['/competition/list']);
    }
  }

  public refresh() {
    if (this.checkDirty()) {
      this.competitionService.getApiCompetitionWithCompetitionid(this.competitionId).subscribe(
          (c) => {
            this.competition = c;
            this.form.get('name').setValue(c.name);
            this.form.get('description').setValue(c.description);
            this.dirty = false;
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
          }
      );
    }
  }

  public addTeam() {
    const dialogRef = this.dialog.open(CompetitionBuilderAddTeamDialogComponent, {data: this.competition, width: '500px'});
    dialogRef.afterClosed().pipe(
        filter(r => r != null),
    ).subscribe((r) => {
      this.competition.teams.push(r as Team);
      this.dirty = true;
    });
  }

  public removeTeams(teams: MatListOption[]) {
    for (const team of teams) {
      this.competition.teams.splice(this.competition.teams.indexOf(team.value));
    }
    this.dirty = true;
  }

  /**
   *
   */
  private checkDirty(): boolean {
    if (!this.dirty) { return true; }
    return confirm('There are unsaved changes in this competition that will be lost. Do you really want to proceed?');
  }
}
