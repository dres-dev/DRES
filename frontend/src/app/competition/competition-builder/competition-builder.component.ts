import { Component, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterStateSnapshot } from '@angular/router';
import { filter, map, shareReplay, take } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { CompetitionBuilderTeamDialogComponent } from './competition-builder-team-dialog/competition-builder-team-dialog.component';
import {
  CompetitionBuilderTaskGroupDialogComponent,
  CompetitionBuilderTaskGroupDialogData,
} from './competition-builder-task-group-dialog/competition-builder-task-group.component';
import { MatTable } from '@angular/material/table';
import { CompetitionBuilderTaskTypeDialogComponent } from './competition-builder-task-type-dialog/competition-builder-task-type-dialog.component';
import {
  CompetitionBuilderTaskDialogComponent,
  CompetitionBuilderTaskDialogData,
} from './competition-builder-task-dialog/competition-builder-task-dialog.component';
import { AppConfig } from '../../app.config';
import { DeactivationGuarded } from '../../services/can-deactivate.guard';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import {
  ApiCreateEvaluation,
  ApiEvaluationTemplate,
  ApiRole, ApiTaskGroup, ApiTaskTemplate, ApiTaskType, ApiTeam,
  ApiUser,
  DownloadService,
  EvaluationService, SuccessStatus,
  TemplateService,
  UserService
} from '../../../../openapi';

@Component({
  selector: 'app-competition-builer',
  templateUrl: './competition-builder.component.html',
  styleUrls: ['./competition-builder.component.scss'],
})
export class CompetitionBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded {

  // FIXME make compiler happy fast. care about templates later

  /**
   * The official VBS Textual Known Item Search task type template
   */
  /*public static TKIS_TEMPLATE = {
    name: 'Textual KIS',
    taskDuration: 420,
    targetType: { option: ConfiguredOptionTargetOption.OptionEnum.SINGLE_MEDIA_SEGMENT, parameters: {} },
    score: { option: ConfiguredOptionScoringOption.OptionEnum.KIS, parameters: {} },
    components: [{ option: ConfiguredOptionQueryComponentOption.OptionEnum.TEXT, parameters: {} }],
    filter: [
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.NO_DUPLICATES, parameters: {} },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.LIMIT_CORRECT_PER_TEAM, parameters: { limit: 1 } },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.TEMPORAL_SUBMISSION, parameters: {} },
    ],
    options: [{ option: ConfiguredOptionSimpleOption.OptionEnum.HIDDEN_RESULTS, parameters: {} }],
  } as ApiTaskType;*/

  /**
   * The official VBS Visual Known Item Search task type template
   */
 /* public static VKIS_TEMPLATE = {
    name: 'Visual KIS',
    taskDuration: 300,
    targetType: { option: ConfiguredOptionTargetOption.OptionEnum.SINGLE_MEDIA_SEGMENT, parameters: {} },
    score: { option: ConfiguredOptionScoringOption.OptionEnum.KIS, parameters: {} },
    components: [{ option: ConfiguredOptionQueryComponentOption.OptionEnum.VIDEO_ITEM_SEGMENT, parameters: {} }],
    filter: [
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.NO_DUPLICATES, parameters: {} },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.LIMIT_CORRECT_PER_TEAM, parameters: { limit: 1 } },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.TEMPORAL_SUBMISSION, parameters: {} },
    ],
    options: [],
  } as ApiTaskType;*/

  /**
   * The official VBS Ad-hoc Video Search task type template
   */
  /*public static AVS_TEMPLATE = {
    name: 'Ad-hoc Video Search',
    taskDuration: 300,
    targetType: { option: ConfiguredOptionTargetOption.OptionEnum.JUDGEMENT, parameters: {} },
    score: { option: ConfiguredOptionScoringOption.OptionEnum.AVS2, parameters: {} },
    components: [{ option: ConfiguredOptionQueryComponentOption.OptionEnum.TEXT, parameters: {} }],
    filter: [
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.NO_DUPLICATES, parameters: { limit: 1 } },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.TEMPORAL_SUBMISSION, parameters: {} },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.LIMIT_CORRECT_PER_ITEM_AND_TEAM, parameters: {} },
    ],
    options: [{ option: ConfiguredOptionSimpleOption.OptionEnum.MAP_TO_SEGMENT, parameters: {} }],
  } as ApiTaskType;*/

  /**
   * The official legacy (pre 2023) VBS Ad-hoc Video Search task type template
   */
  /*public static LEGACY_AVS_TEMPLATE = {
    name: 'Ad-hoc Video Search (Legacy)',
    taskDuration: 300,
    targetType: { option: ConfiguredOptionTargetOption.OptionEnum.JUDGEMENT, parameters: {} },
    score: { option: ConfiguredOptionScoringOption.OptionEnum.AVS, parameters: {} },
    components: [{ option: ConfiguredOptionQueryComponentOption.OptionEnum.TEXT, parameters: {} }],
    filter: [
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.NO_DUPLICATES, parameters: { limit: 1 } },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.TEMPORAL_SUBMISSION, parameters: {} },
    ],
    options: [{ option: ConfiguredOptionSimpleOption.OptionEnum.MAP_TO_SEGMENT, parameters: {} }],
  } as ApiTaskType;*/

  /**
   * The official LSC task type template
   */
  /*public static LSC_TEMPLATE = {
    name: 'LSC',
    taskDuration: 300,
    targetType: { option: ConfiguredOptionTargetOption.OptionEnum.MULTIPLE_MEDIA_ITEMS, parameters: {} },
    score: { option: ConfiguredOptionScoringOption.OptionEnum.KIS, parameters: {} },
    components: [{ option: ConfiguredOptionQueryComponentOption.OptionEnum.TEXT, parameters: {} }],
    filter: [
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.NO_DUPLICATES, parameters: {} },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.LIMIT_CORRECT_PER_TEAM, parameters: {} },
    ],
    options: [{ option: ConfiguredOptionSimpleOption.OptionEnum.HIDDEN_RESULTS, parameters: {} }],
  } as ApiTaskType;*/

  competitionId: string;
  competition: ApiEvaluationTemplate;
  @ViewChild('taskTable')
  taskTable: MatTable<any>;
  @ViewChild('teamTable')
  teamTable: MatTable<any>;
  @ViewChild('judgesTable')
  judgesTable: MatTable<ApiUser>;
  displayedColumnsTeams: string[] = ['logo', 'name', 'action'];
  displayedColumnsJudges: string[] = ['name', 'action'];
  displayedColumnsTasks: string[] = ['name', 'group', 'type', 'duration', 'action'];
  form: FormGroup = new FormGroup({ name: new FormControl(''), description: new FormControl('') });
  dirty = false;
  routeSubscription: Subscription;
  changeSubscription: Subscription;

  availableJudges: Observable<ApiUser[]>;

  // FIXME make compiler happy fast. care about templates later

  /**
   * Ref to template for easy access in thml
   */
  tkisTemplate = null // CompetitionBuilderComponent.TKIS_TEMPLATE;
  /**
   * Ref to template for easy access in thml
   */
  vkisTemplate = null // CompetitionBuilderComponent.VKIS_TEMPLATE;
  /**
   * Ref to template for easy access in thml
   */
  avsTemplate = null // CompetitionBuilderComponent.AVS_TEMPLATE;
  /**
   * Ref to template for easy access in thml
   */
  lscTemplate = null // CompetitionBuilderComponent.LSC_TEMPLATE;

  constructor(
    private competitionService: TemplateService,
    private userService: UserService,
    private downloadService: DownloadService,
    private route: ActivatedRoute,
    private routerService: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private config: AppConfig
  ) {
    this.availableJudges = this.userService.getApiV2UserList().pipe(
      map((users) => users.filter((user) => user.role === ApiRole.JUDGE)),
      shareReplay(1)
    );
  }

  ngOnInit() {
    this.routeSubscription = this.route.params.subscribe((p) => {
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

  private fetchDataToCompetition() {
    this.competition.name = this.form.get('name').value;
    this.competition.description = this.form.get('description').value;
    // TODO fetch other stuff
  }

  public save() {
    if (this.form.valid) {
      this.fetchDataToCompetition();
      let obs: Observable<SuccessStatus>;
      if(this.competition.id){
        /* saving existing */
        obs = this.competitionService.patchApiV2TemplateByTemplateId(this.competitionId, this.competition)
      }else{
        /* saving new */
        obs = this.competitionService.postApiV2Template(this.competition as ApiCreateEvaluation)
      }
      obs.subscribe(
        (c) => {
          this.snackBar.open(c.description, null, { duration: 5000 });
          this.dirty = false;
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
    } else {
      console.log("Save failed due to validation error:")
      console.log(this.form.errors)
    }
  }

  fileProvider = () => {
    this.fetchDataToCompetition();
    return this.competition?.name ? this.competition.name : 'competition-download.json';
  };

  downloadProvider = () => {
    return this.downloadService.getApiV2DownloadEvaluationByEvaluationId(this.competitionId).pipe(take(1));
    // .toPromise();
  };

  public back() {
    if (this.checkDirty()) {
      this.routerService.navigate(['/template/list']);
    }
  }

  public refresh() {
    if (this.checkDirty()) {
      this.competitionService.getApiV2TemplateByTemplateId(this.competitionId).subscribe(
        (c) => {
          this.competition = c;
          this.form.get('name').setValue(c.name);
          this.form.get('description').setValue(c.description);
          this.dirty = false;
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
    }
  }

  public addTaskType(type?: ApiTaskType) {
    const dialogRef = this.dialog.open(CompetitionBuilderTaskTypeDialogComponent, { data: type ? type : null, width: '750px' });
    dialogRef
      .afterClosed()
      .pipe(filter((g) => g != null))
      .subscribe((g) => {
        this.competition.taskTypes.push(g);
        this.dirty = true;
      });
  }

  /**
   * Removes a task type and all associated task groups.
   *
   * @param ApiTaskType The {@link ApiTaskType} to remove.
   */
  public removeTaskType(taskType: ApiTaskType) {
    this.competition.taskTypes.splice(this.competition.taskTypes.indexOf(taskType), 1);
    this.competition.taskGroups
      .filter((t) => t.type === taskType.name)
      .forEach((g) => {
        this.removeTaskGroup(g);
      });
    this.dirty = true;
  }

  /**
   * Opens the dialog to add a new task group.
   */
  public addTaskGroup(group?: ApiTaskGroup) {
    const dialogRef = this.dialog.open(CompetitionBuilderTaskGroupDialogComponent, {
      data: { types: this.competition.taskTypes, group: group ? group : null } as CompetitionBuilderTaskGroupDialogData,
      width: '750px',
    });
    dialogRef
      .afterClosed()
      .pipe(filter((g) => g != null))
      .subscribe((g) => {
        this.competition.taskGroups.push(g);
        this.dirty = true;
      });
  }

  /**
   * Removes a task group and all associated tasks.
   *
   * @param group The {@link TaskGroup} to remove
   */
  public removeTaskGroup(group: ApiTaskGroup) {
    this.competition.taskGroups.splice(this.competition.taskGroups.indexOf(group), 1);
    this.competition.tasks
      .filter((t) => t.taskGroup === group.name)
      .forEach((t) => {
        this.removeTask(t);
      });
    this.dirty = true;
  }

  /**
   * Opens the dialog to add a new task description.
   *
   * @param group The TaskGroup to add a description to.
   */
  public addTask(group: ApiTaskGroup) {
    const type = this.competition.taskTypes.find((v) => v.name === group.type);
    const width = 750;
    const dialogRef = this.dialog.open(CompetitionBuilderTaskDialogComponent, {
      data: { taskGroup: group, taskType: type, task: null } as CompetitionBuilderTaskDialogData,
      width: `${width}px`,
    });
    dialogRef
      .afterClosed()
      .pipe(filter((t) => t != null))
      .subscribe((t) => {
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
  public editTask(task: ApiTaskTemplate) {
    const index = this.competition.tasks.indexOf(task);
    const width = 750;
    if (index > -1) {
      const dialogRef = this.dialog.open(CompetitionBuilderTaskDialogComponent, {
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
          this.dirty = true;
          this.taskTable.renderRows();
        });
    }
  }

  /**
   * Moves the selected task up in the list, thus changing the order of tasks.
   *
   * @param task The {@link RestTaskDescription} that should be moved.
   */
  public moveTaskUp(task: ApiTaskTemplate) {
    const oldIndex = this.competition.tasks.indexOf(task);
    if (oldIndex > 0) {
      const buffer = this.competition.tasks[oldIndex - 1];
      this.competition.tasks[oldIndex - 1] = task;
      this.competition.tasks[oldIndex] = buffer;
      this.taskTable.renderRows();
      this.dirty = true;
    }
  }

  /**
   * Moves the selected task down in the list, thus changing the order of tasks.
   *
   * @param task The {@link RestTaskDescription} that should be moved.
   */
  public moveTaskDown(task: ApiTaskTemplate) {
    const oldIndex = this.competition.tasks.indexOf(task);
    if (oldIndex < this.competition.tasks.length - 1) {
      const buffer = this.competition.tasks[oldIndex + 1];
      this.competition.tasks[oldIndex + 1] = task;
      this.competition.tasks[oldIndex] = buffer;
      this.taskTable.renderRows();
      this.dirty = true;
    }
  }

  /**
   * Removes the selected {@link RestTaskDescription} from the list of {@link RestTaskDescription}s.
   *
   * @param task The {@link RestTaskDescription} to remove.
   */
  public removeTask(task: ApiTaskTemplate) {
    this.competition.tasks.splice(this.competition.tasks.indexOf(task), 1);
    this.dirty = true;
    this.taskTable.renderRows();
  }

  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(team: ApiTeam): string {
    if (team.logoData != null) {
      return team.logoData;
    } else {
      return this.config.resolveApiUrl(`/template/logo/${team.id}`);
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

  public editTeam(team: ApiTeam) {
    const index = this.competition.teams.indexOf(team);
    if (index > -1) {
      const dialogRef = this.dialog.open(CompetitionBuilderTeamDialogComponent, { data: team, width: '500px' });
      dialogRef
        .afterClosed()
        .pipe(filter((t) => t != null))
        .subscribe((t: ApiTeam) => {
          this.competition.teams[index] = t;
          this.dirty = true;
          this.teamTable.renderRows();
        });
    }
  }

  public removeTeam(team: ApiTeam) {
    this.competition.teams.splice(this.competition.teams.indexOf(team), 1);
    this.dirty = true;
    this.teamTable.renderRows();
  }

  public judgeFor(id: string): Observable<ApiUser> {
    return this.availableJudges.pipe(map((users) => users.find((u) => u.id === id)));
  }

  public addJudge(event: MatAutocompleteSelectedEvent) {
    if (this.competition.judges.includes(event.option.value.id)) {
      return;
    }
    this.competition.judges.push(event.option.value.id);
    this.dirty = true;
    this.judgesTable.renderRows();
  }

  public removeJudge(judgeId: string) {
    this.competition.judges.splice(this.competition.judges.indexOf(judgeId), 1);
    this.dirty = true;
    this.judgesTable.renderRows();
  }

  public dispJudge(user: ApiUser) {
    return user.username;
  }

  /**
   * Summarises a task type to present detailed info as tooltip.
   *
   * @param taskType The {@link ApiTaskType} to summarize.
   */
  summariseTaskType(taskType: ApiTaskType): string {
    return `Consists of ${taskType.hintOptions.map((c) => c).join(', ')}, has filters: ${taskType.submissionOptions
      .map((f) => f)
      .join(', ')} and options: ${taskType.taskOptions.map((o) => o).join(', ')}`;
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
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (!this.checkDirty()) {
      event.preventDefault();
      event.returnValue = '';
      return;
    }
    delete event.returnValue;
  }
}
