import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {filter} from 'rxjs/operators';
import {Competition, CompetitionService, Task, TaskDescription, Team} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {FormControl, FormGroup} from '@angular/forms';
import { Subscription} from 'rxjs';
import {MatDialog} from '@angular/material/dialog';
import {
  CompetitionBuilderTeamDialogComponent,
} from './competition-builder-team-dialog.component';
import TaskTypeEnum = TaskDescription.TaskTypeEnum;
import {CompetitionBuilderTaskDialogComponent, CompetitionBuilderTaskDialogData} from './competition-builder-task-dialog.component';

@Component({
  selector: 'app-competition-builer',
  templateUrl: './competition-builder.component.html',
  styleUrls: ['./competition-builder.component.scss']
})
export class CompetitionBuilderComponent implements OnInit, OnDestroy {

  competitionId: number;
  competition: Competition;
  form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});
  dirty = false;
  routeSubscription: Subscription;
  changeSubscription: Subscription;
  taskTypes = [TaskTypeEnum.AVS, TaskTypeEnum.KISTEXTUAL, TaskTypeEnum.KISVISUAL];

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

  public save() {
    if (this.form.valid) {
      this.competition.name = this.form.get('name').value;
      this.competition.description = this.form.get('description').value;
      this.competitionService.patchApiCompetition(this.competition).subscribe(
          (c) => {
            this.snackBar.open(c.description, null, { duration: 5000});
            this.dirty = false;
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
          }
      );
    }
  }

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

  public addTask(taskType: TaskTypeEnum) {
    const dialogRef = this.dialog.open(
        CompetitionBuilderTaskDialogComponent,
        {data: {taskType: taskType, task: null} as CompetitionBuilderTaskDialogData, width: '750px'}
    );
    dialogRef.afterClosed().pipe(
        filter(t => t != null),
    ).subscribe((t) => {
      this.competition.tasks.push(t);
      this.dirty = true;
    });
  }

  public editTask(task: Task) {
    const index = this.competition.tasks.indexOf(task);
    if (index > -1) {
      const dialogRef = this.dialog.open(
          CompetitionBuilderTaskDialogComponent,
          {data: {taskType: task.description.taskType, task: task} as CompetitionBuilderTaskDialogData, width: '750px'}
      );
      dialogRef.afterClosed().pipe(
          filter(t => t != null),
      ).subscribe((t) => {
        this.competition.tasks[index] = t;
        this.dirty = true;
      });
    }
  }

  public removeTask(task: Task) {
    this.competition.tasks.splice(this.competition.tasks.indexOf(task), 1);
    this.dirty = true;
  }

  public addTeam() {
    const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, {width: '500px'});
    dialogRef.afterClosed().pipe(
        filter(t => t != null),
    ).subscribe((t) => {
      this.competition.teams.push(t);
      this.dirty = true;
    });
  }

  public editTeam(team: Team) {
    const index = this.competition.teams.indexOf(team);
    if (index > -1) {
      const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, {data: team, width: '500px'});
      dialogRef.afterClosed().pipe(
          filter(t => t != null),
      ).subscribe((t: Team) => {
        this.competition.teams[index] = t;
        this.dirty = true;
      });
    }
  }

  public removeTeam(team: Team) {
    this.competition.teams.splice(this.competition.teams.indexOf(team), 1);
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
