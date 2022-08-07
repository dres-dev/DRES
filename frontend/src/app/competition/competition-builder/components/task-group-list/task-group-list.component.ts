import {Component, OnDestroy, OnInit} from '@angular/core';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {CompetitionBuilderService} from '../../competition-builder.service';
import {RestCompetitionDescription, TaskGroup} from '../../../../../../openapi';
import {
  CompetitionBuilderTaskGroupDialogComponent,
  CompetitionBuilderTaskGroupDialogData
} from '../../competition-builder-task-group-dialog/competition-builder-task-group.component';
import {filter} from 'rxjs/operators';
import {MatDialog} from '@angular/material/dialog';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-task-group-list',
  templateUrl: './task-group-list.component.html',
  styleUrls: ['./task-group-list.component.scss']
})
export class TaskGroupListComponent extends AbstractCompetitionBuilderComponent implements OnInit, OnDestroy {

  groups = new Observable<Array<TaskGroup>>((o) => o.next([]))

  constructor(builderService: CompetitionBuilderService,
              private dialog: MatDialog) {
    super(builderService)
  }

  ngOnInit(): void {
    this.onInit()
  }

  ngOnDestroy() {
    this.onDestroy()
  }

  onChange(competition: RestCompetitionDescription) {
    this.groups = new Observable<Array<TaskGroup>>((o) => {
      if(competition){
        o.next(competition.taskGroups)
      }else{
        o.next([])
      }
    })
  }

  /**
   * Opens the dialog to add a new task group.
   */
  public addTaskGroup(group?: TaskGroup) {
    const dialogRef = this.dialog.open(CompetitionBuilderTaskGroupDialogComponent, {
      data: { types: this.competition.taskTypes, group: group ? group : null } as CompetitionBuilderTaskGroupDialogData,
      width: '750px',
    });
    dialogRef
        .afterClosed()
        .pipe(filter((g) => g != null))
        .subscribe((g) => {
          this.competition.taskGroups.push(g);
          this.update()
        });
  }

  /**
   * Removes a task group and all associated tasks.
   *
   * @param group The {@link TaskGroup} to remove
   */
  public removeTaskGroup(group: TaskGroup) {
    this.builderService.removeTaskGroup(group);
  }

}
