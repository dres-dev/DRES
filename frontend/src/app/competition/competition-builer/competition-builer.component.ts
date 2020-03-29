import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {filter, first, flatMap, tap} from 'rxjs/operators';
import {Competition, CompetitionOverview, CompetitionService, Team} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {FormControl, FormGroup} from '@angular/forms';
import {Observable} from 'rxjs';
import {MatDialog} from '@angular/material/dialog';
import {CompetitionCreateDialogComponent, CompetitionCreateDialogResult} from '../competition-list/competition-create-dialog.component';
import {
  CompetitionBuilderAddTeamDialogComponent,
  CompetitionBuilderAddTeamDialogResult
} from './competition-builder-add-team-dialog.component';

@Component({
  selector: 'app-competition-builer',
  templateUrl: './competition-builer.component.html',
  styleUrls: ['./competition-builer.component.scss']
})
export class CompetitionBuilerComponent implements OnInit {

  competitionId: number;
  competition: Competition;
  form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});
  constructor(private competitionService: CompetitionService,
              private route: ActivatedRoute,
              private routerService: Router,
              private snackBar: MatSnackBar,
              private dialog: MatDialog) { }

  ngOnInit() {
    this.route.params.pipe(
        first()
    ).subscribe(p => {
      this.competitionId = +(p.competitionId);
      this.refresh();
    });
  }


  public save() {

  }

  public back() {
    this.routerService.navigate(['/competition/list']);
  }

  public refresh() {
    this.competitionService.getApiCompetitionWithCompetitionid(this.competitionId).subscribe(
        (c) => {
          this.competition = c;
          this.form.get('name').setValue(c.name);
          this.form.get('description').setValue(c.description);
        },
    (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
      }
    );
  }

  public addTeam() {
    const dialogRef = this.dialog.open(CompetitionBuilderAddTeamDialogComponent, {data: this.competition, width: '500px'});
    dialogRef.afterClosed().pipe(
        filter(r => r != null),
    ).subscribe((r) => {
      this.competition.teams.push(r as Team);
    });
  }
}
