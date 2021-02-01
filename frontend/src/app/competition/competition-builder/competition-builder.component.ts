import {Component, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router, RouterStateSnapshot} from '@angular/router';
import {filter} from 'rxjs/operators';
import {
    CompetitionService,
    ConfiguredOptionOptions,
    ConfiguredOptionQueryComponentType,
    ConfiguredOptionScoringType,
    ConfiguredOptionSubmissionFilterType,
    ConfiguredOptionTargetType,
    RestCompetitionDescription,
    RestTaskDescription,
    RestTeam,
    TaskGroup,
    TaskType
} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {FormControl, FormGroup} from '@angular/forms';
import {Observable, Subscription} from 'rxjs';
import {MatDialog} from '@angular/material/dialog';
import {CompetitionBuilderTeamDialogComponent} from './competition-builder-team-dialog/competition-builder-team-dialog.component';
import {
    CompetitionBuilderTaskGroupDialogComponent,
    CompetitionBuilderTaskGroupDialogData
} from './competition-builder-task-group-dialog/competition-builder-task-group.component';
import {MatTable} from '@angular/material/table';
import {CompetitionBuilderTaskTypeDialogComponent} from './competition-builder-task-type-dialog/competition-builder-task-type-dialog.component';
import {
    CompetitionBuilderTaskDialogComponent,
    CompetitionBuilderTaskDialogData
} from './competition-builder-task-dialog/competition-builder-task-dialog.component';
import {AppConfig} from '../../app.config';
import {DeactivationGuarded} from '../../services/can-deactivate.guard';

@Component({
    selector: 'app-competition-builer',
    templateUrl: './competition-builder.component.html',
    styleUrls: ['./competition-builder.component.scss']
})
export class CompetitionBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded {

    /**
     * The official VBS Textual Known Item Search task type template
     */
    public static TKIS_TEMPLATE = {
        name: 'Textual KIS',
        taskDuration: 420,
        targetType: {option: ConfiguredOptionTargetType.OptionEnum.SINGLE_MEDIA_SEGMENT, parameters: {}},
        score: {option: ConfiguredOptionScoringType.OptionEnum.KIS, parameters: {}},
        components: [
            {option: ConfiguredOptionQueryComponentType.OptionEnum.TEXT, parameters: {}}
        ],
        filter: [
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.NO_DUPLICATES, parameters: {}},
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.LIMIT_CORRECT_PER_TEAM, parameters: {limit: 1}},
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.TEMPORAL_SUBMISSION, parameters: {}}
        ],
        options: [
            {option: ConfiguredOptionOptions.OptionEnum.HIDDEN_RESULTS, parameters: {}},
        ]
    } as TaskType;

    /**
     * The official VBS Visual Known Item Search task type template
     */
    public static VKIS_TEMPLATE = {
        name: 'Visual KIS',
        taskDuration: 300,
        targetType: {option: ConfiguredOptionTargetType.OptionEnum.SINGLE_MEDIA_SEGMENT, parameters: {}},
        score: {option: ConfiguredOptionScoringType.OptionEnum.KIS, parameters: {}},
        components: [
            {option: ConfiguredOptionQueryComponentType.OptionEnum.VIDEO_ITEM_SEGMENT, parameters: {}}
        ],
        filter: [
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.NO_DUPLICATES, parameters: {}},
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.LIMIT_CORRECT_PER_TEAM, parameters: {limit: 1}},
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.TEMPORAL_SUBMISSION, parameters: {}}
        ],
        options: []
    } as TaskType;

    /**
     * The official VBS Ad-hoc Video Search task type template
     */
    public static AVS_TEMPLATE = {
        name: 'Ad-hoc Video Search',
        taskDuration: 300,
        targetType: {option: ConfiguredOptionTargetType.OptionEnum.JUDGEMENT, parameters: {}},
        score: {option: ConfiguredOptionScoringType.OptionEnum.AVS, parameters: {}},
        components: [
            {option: ConfiguredOptionQueryComponentType.OptionEnum.TEXT, parameters: {}}
        ],
        filter: [
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.NO_DUPLICATES, parameters: {limit: 1}},
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.TEMPORAL_SUBMISSION, parameters: {}}
        ],
        options: [
            {option: ConfiguredOptionOptions.OptionEnum.MAP_TO_SEGMENT,  parameters: {}}
        ]
    } as TaskType;

    /**
     * The official LSC task type template
     */
    public static LSC_TEMPLATE = {
        name: 'LSC',
        taskDuration: 300,
        targetType: {option: ConfiguredOptionTargetType.OptionEnum.MULTIPLE_MEDIA_ITEMS, parameters: {}},
        score: {option: ConfiguredOptionScoringType.OptionEnum.KIS, parameters: {}},
        components: [
            {option: ConfiguredOptionQueryComponentType.OptionEnum.TEXT, parameters: {}}
        ],
        filter: [
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.NO_DUPLICATES, parameters: {}},
            {option: ConfiguredOptionSubmissionFilterType.OptionEnum.LIMIT_CORRECT_PER_TEAM, parameters: {}}
        ],
        options: [
            {option: ConfiguredOptionOptions.OptionEnum.HIDDEN_RESULTS,  parameters: {}}
        ]
    } as TaskType;

    competitionId: string;
    competition: RestCompetitionDescription;
    @ViewChild('taskTable')
    taskTable: MatTable<any>;
    @ViewChild('teamTable')
    teamTable: MatTable<any>;
    displayedColumnsTeams: string[] = ['logo', 'name', 'action'];
    displayedColumnsTasks: string[] = ['name', 'group', 'type', 'duration', 'action'];
    form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});
    dirty = false;
    routeSubscription: Subscription;
    changeSubscription: Subscription;

    /**
     * Ref to template for easy access in thml
     */
    tkisTemplate = CompetitionBuilderComponent.TKIS_TEMPLATE;
    /**
     * Ref to template for easy access in thml
     */
    vkisTemplate = CompetitionBuilderComponent.VKIS_TEMPLATE;
    /**
     * Ref to template for easy access in thml
     */
    avsTemplate = CompetitionBuilderComponent.AVS_TEMPLATE;
    /**
     * Ref to template for easy access in thml
     */
    lscTemplate = CompetitionBuilderComponent.LSC_TEMPLATE;

    constructor(private competitionService: CompetitionService,
                private route: ActivatedRoute,
                private routerService: Router,
                private snackBar: MatSnackBar,
                private dialog: MatDialog,
                private config: AppConfig) {
    }

    ngOnInit() {
        this.routeSubscription = this.route.params.subscribe(p => {
            this.competitionId = p.competitionId;
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

    private fetchDataToCompetition(){
        this.competition.name = this.form.get('name').value;
        this.competition.description = this.form.get('description').value;
        // TODO fetch other stuff
    }

    public save() {
        if (this.form.valid) {
            this.fetchDataToCompetition();
            this.competitionService.patchApiCompetition(this.competition).subscribe(
                (c) => {
                    this.snackBar.open(c.description, null, {duration: 5000});
                    this.dirty = false;
                },
                (r) => {
                    this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                }
            );
        }
    }

    fileProvider = () => {
        this.fetchDataToCompetition();
        return this.competition?.name ? this.competition.name : 'competition-download.json';
    }

    downloadProvider = () => {
        this.fetchDataToCompetition();
        return JSON.stringify(this.competition);
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
                    // TODO fetch other stuff
                    this.dirty = false;
                },
                (r) => {
                    this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                }
            );
        }
    }


    /**
     * Opens the dialog to add a new task group.
     */
    public addTaskGroup(group?: TaskGroup) {
        const dialogRef = this.dialog.open(
            CompetitionBuilderTaskGroupDialogComponent,
            {
                data: {types: this.competition.taskTypes, group: group ? group : null} as CompetitionBuilderTaskGroupDialogData,
                width: '750px'
            }
        );
        dialogRef.afterClosed().pipe(
            filter(g => g != null),
        ).subscribe((g) => {
            this.competition.taskGroups.push(g);
            this.dirty = true;
        });
    }

    public addTaskType(type?: TaskType) {
        const dialogRef = this.dialog.open(
            CompetitionBuilderTaskTypeDialogComponent,
            {data: type ? type : null, width: '750px'}
        );
        dialogRef.afterClosed().pipe(
            filter(g => g != null),
        ).subscribe((g) => {
            this.competition.taskTypes.push(g);
            this.dirty = true;
        });
    }

    /**
     * Removes a Task Group and all associated tasks.
     */
    public removeTaskGroup(group: TaskGroup) {
        this.competition.taskGroups.splice(this.competition.taskGroups.indexOf(group));
        // assuming taskGroup:string in task is the actual name --> late will be replaced by uuid
        this.competition.tasks.filter(t => t.taskGroup === group.name).map(t => this.competition.tasks.indexOf(t)).forEach(i => {
            this.competition.tasks.splice(i);
        });
        this.dirty = true;
    }

    /**
     * Opens the dialog to add a new task description.
     *
     * @param group The TaskGroup to add a description to.
     */
    public addTask(group: TaskGroup) {
        const type = this.competition.taskTypes.find(v => v.name === group.type);
        // const width = window.screen.width * .75;
        const width = 750;
        const dialogRef = this.dialog.open(
            CompetitionBuilderTaskDialogComponent,
            {data: {taskGroup: group, taskType: type, task: null} as CompetitionBuilderTaskDialogData, width: `${width}px`}
        );
        dialogRef.afterClosed().pipe(
            filter(t => t != null),
        ).subscribe((t) => {
            this.competition.tasks.push(t);
            this.dirty = true;
            this.taskTable.renderRows();
        });
    }

    /**
     * Opens the dialog to edit a {@link RestTaskDescription}.
     *
     * @param task The {@link RestTaskDescription} to edit.
     */
    public editTask(task: RestTaskDescription) {
        const index = this.competition.tasks.indexOf(task);
        // const width = window.screen.width * .75;
        const width = 750;
        if (index > -1) {
            const dialogRef = this.dialog.open(
                CompetitionBuilderTaskDialogComponent,
                {
                    data: {
                        taskGroup: this.competition.taskGroups.find(g => g.name === task.taskGroup),
                        taskType: this.competition.taskTypes.find(g => g.name === task.taskType),
                        task
                    } as CompetitionBuilderTaskDialogData, width: `${width}px`
                }
            );
            dialogRef.afterClosed().pipe(
                filter(t => t != null),
            ).subscribe((t) => {
                this.competition.tasks[index] = t;
                this.dirty = true;
                this.taskTable.renderRows();
            });
        }
    }



    /**
     * Removes the selected {@link RestTaskDescription} from the list of {@link RestTaskDescription}s.
     *
     * @param task The {@link RestTaskDescription} to remove.
     */
    public removeTask(task: RestTaskDescription) {
        this.competition.tasks.splice(this.competition.tasks.indexOf(task), 1);
        this.dirty = true;
        this.taskTable.renderRows();
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
        dialogRef.afterClosed().pipe(
            filter(t => t != null),
        ).subscribe((t) => {
            this.competition.teams.push(t);
            this.dirty = true;
            this.teamTable.renderRows();
        });
    }

    public editTeam(team: RestTeam) {
        const index = this.competition.teams.indexOf(team);
        if (index > -1) {
            const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, {data: team, width: '500px'});
            dialogRef.afterClosed().pipe(
                filter(t => t != null),
            ).subscribe((t: RestTeam) => {
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

    removeTaskType(taskType: TaskType) {
        this.competition.taskTypes.splice(this.competition.taskTypes.indexOf(taskType), 1);
        this.dirty = true;
    }

    /**
     * Summarises a task type to present detailed info as tooltip.
     *
     * @param taskType The {@link TaskType} to summarize.
     */
    summariseTaskType(taskType: TaskType): string {
        return `Consists of ${taskType.components.map(c => c.option).join(', ')}, has filters: ${taskType.filter.map(f => f.option).join(', ')} and options: ${taskType.options.map(o => o.option).join(', ')}`;
    }

    /**
     *
     */
    private checkDirty(): boolean {
        if (!this.dirty) {
            return true;
        }
        return confirm('There are unsaved changes in this competition that will be lost. Do you really want to proceed?');
    }

    canDeactivate(nextState?: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
        return this.checkDirty();
    }

    @HostListener('window:beforeunload', ['$event'])
    handleBeforeUnload(event: BeforeUnloadEvent){
        if (!this.checkDirty()){
            event.preventDefault();
            event.returnValue = '';
            return;
        }
        delete event.returnValue;
    }
}
