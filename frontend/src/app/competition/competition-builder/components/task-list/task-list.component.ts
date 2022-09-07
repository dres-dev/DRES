import {Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {CompetitionBuilderService} from '../../competition-builder.service';
import {RestCompetitionDescription, RestTaskDescription, TaskGroup} from '../../../../../../openapi';
import {
    CompetitionBuilderTaskDialogComponent,
    CompetitionBuilderTaskDialogData
} from '../../competition-builder-task-dialog/competition-builder-task-dialog.component';
import {filter} from 'rxjs/operators';
import {MatTable} from '@angular/material/table';
import {MatDialog} from '@angular/material/dialog';
import {Observable} from 'rxjs';
import {TasksTabComponent} from '../../tabs/tasks-tab/tasks-tab.component';
import {SelectionModel} from '@angular/cdk/collections';

@Component({
    selector: 'app-task-list',
    templateUrl: './task-list.component.html',
    styleUrls: ['./task-list.component.scss']
})
export class TaskListComponent extends AbstractCompetitionBuilderComponent implements OnInit, OnDestroy {

    @Input()
    parent: TasksTabComponent;

    @ViewChild('taskTable')
    taskTable: MatTable<any>;
    displayedColumnsTasks: string[] = ['name', 'group', 'type', 'duration', 'action'];
    tasks = new Observable<Array<RestTaskDescription>>((o) => o.next([]));
    groups = new Observable<Array<TaskGroup>>((o) => o.next([]));

    selection = new SelectionModel(false,[], false)

    constructor(builderService: CompetitionBuilderService,
                private dialog: MatDialog) {
        super(builderService);
    }


    /**
     * Opens the dialog to add a new task description.
     *
     * @param group The TaskGroup to add a description to.
     */
    public addTask(group: TaskGroup) {
        const type = this.competition.taskTypes.find((v) => v.name === group.type);
        this.parent.editTask(type, group, null);
        /*
        const width = 750;
        const dialogRef = this.dialog.open(CompetitionBuilderTaskDialogComponent, {
            data: {taskGroup: group, taskType: type, task: null} as CompetitionBuilderTaskDialogData,
            width: `${width}px`,
        });
        dialogRef
            .afterClosed()
            .pipe(filter((t) => t != null))
            .subscribe((t) => {
                this.competition.tasks.push(t);
                this.update();
                this.taskTable.renderRows();
            });*/
    }

    /**
     * Opens the dialog to edit a {@link RestTaskDescription}.
     *
     * @param task The {@link RestTaskDescription} to edit.
     */
    public editTask(task: RestTaskDescription) {
        const index = this.competition.tasks.indexOf(task);
        const width = 750;
        if (index > -1) {
            this.selection.toggle(task)
            this.parent.editTask(
                this.competition.taskTypes.find((g) => g.name === task.taskType),
                this.competition.taskGroups.find((g) => g.name === task.taskGroup),
                task
            );
            /*const dialogRef = this.dialog.open(CompetitionBuilderTaskDialogComponent, {
                data: {
                    taskGroup: this.competition.taskGroups.find((g) => g.name === task.taskGroup),
                    taskType: this.competition.taskTypes.find((g) => g.name === task.taskType),
                    task,
                } as CompetitionBuilderTaskDialogData,
                width: `${width}px`,
            });
            dialogRef
                .afterClosed()
                .pipe(filter((t) => t != null))
                .subscribe((t) => {
                    this.competition.tasks[index] = t;
                    this.update();
                    this.taskTable.renderRows();
                });*/
        }
    }

    /**
     * Moves the selected task up in the list, thus changing the order of tasks.
     *
     * @param task The {@link RestTaskDescription} that should be moved.
     */
    public moveTaskUp(task: RestTaskDescription) {
        const oldIndex = this.competition.tasks.indexOf(task);
        if (oldIndex > 0) {
            const buffer = this.competition.tasks[oldIndex - 1];
            this.competition.tasks[oldIndex - 1] = task;
            this.competition.tasks[oldIndex] = buffer;
            this.taskTable.renderRows();
            this.update();
        }
    }

    /**
     * Moves the selected task down in the list, thus changing the order of tasks.
     *
     * @param task The {@link RestTaskDescription} that should be moved.
     */
    public moveTaskDown(task: RestTaskDescription) {
        const oldIndex = this.competition.tasks.indexOf(task);
        if (oldIndex < this.competition.tasks.length - 1) {
            const buffer = this.competition.tasks[oldIndex + 1];
            this.competition.tasks[oldIndex + 1] = task;
            this.competition.tasks[oldIndex] = buffer;
            this.taskTable.renderRows();
            this.update();
        }
    }

    /**
     * Removes the selected {@link RestTaskDescription} from the list of {@link RestTaskDescription}s.
     *
     * @param task The {@link RestTaskDescription} to remove.
     */
    public removeTask(task: RestTaskDescription) {
        this.builderService.removeTask(task);
    }

    ngOnDestroy(): void {
        this.onDestroy();
    }

    ngOnInit(): void {
        this.onInit();
    }

    onChange(competition: RestCompetitionDescription) {
        this.tasks = new Observable<Array<RestTaskDescription>>((o) => {
            if (competition) {
                o.next(competition.tasks);
            } else {
                o.next([]);
            }
        });
      this.groups = new Observable<Array<TaskGroup>>((o) => {
        if(competition){
          o.next(competition.taskGroups)
        }else{
          o.next([])
        }
      })
    }

}
