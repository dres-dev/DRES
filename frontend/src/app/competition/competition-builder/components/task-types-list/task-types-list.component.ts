import {Component, OnDestroy, OnInit} from '@angular/core';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {CompetitionBuilderService} from '../../competition-builder.service';
import {
  ConfiguredOptionQueryComponentOption,
  ConfiguredOptionScoringOption, ConfiguredOptionSimpleOption, ConfiguredOptionSubmissionFilterOption,
  ConfiguredOptionTargetOption,
  RestCompetitionDescription,
  TaskType
} from '../../../../../../openapi';
import {
  CompetitionBuilderTaskTypeDialogComponent
} from '../../competition-builder-task-type-dialog/competition-builder-task-type-dialog.component';
import {filter} from 'rxjs/operators';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-task-types-list',
  templateUrl: './task-types-list.component.html',
  styleUrls: ['./task-types-list.component.scss']
})
export class TaskTypesListComponent extends AbstractCompetitionBuilderComponent implements OnInit, OnDestroy {

  /**
   * The official VBS Textual Known Item Search task type template
   */
  public static TKIS_TEMPLATE = {
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
  } as TaskType;

  /**
   * The official VBS Visual Known Item Search task type template
   */
  public static VKIS_TEMPLATE = {
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
  } as TaskType;

  /**
   * The official VBS Ad-hoc Video Search task type template
   */
  public static AVS_TEMPLATE = {
    name: 'Ad-hoc Video Search',
    taskDuration: 300,
    targetType: { option: ConfiguredOptionTargetOption.OptionEnum.JUDGEMENT, parameters: {} },
    score: { option: ConfiguredOptionScoringOption.OptionEnum.AVS, parameters: {} },
    components: [{ option: ConfiguredOptionQueryComponentOption.OptionEnum.TEXT, parameters: {} }],
    filter: [
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.NO_DUPLICATES, parameters: { limit: 1 } },
      { option: ConfiguredOptionSubmissionFilterOption.OptionEnum.TEMPORAL_SUBMISSION, parameters: {} },
    ],
    options: [{ option: ConfiguredOptionSimpleOption.OptionEnum.MAP_TO_SEGMENT, parameters: {} }],
  } as TaskType;

  /**
   * Ref to template for easy access in thml
   */
  tkisTemplate = TaskTypesListComponent.TKIS_TEMPLATE;
  /**
   * Ref to template for easy access in thml
   */
  vkisTemplate = TaskTypesListComponent.VKIS_TEMPLATE;
  /**
   * Ref to template for easy access in thml
   */
  avsTemplate = TaskTypesListComponent.AVS_TEMPLATE;
  /**
   * Ref to template for easy access in thml
   */
  lscTemplate = TaskTypesListComponent.LSC_TEMPLATE;

  /**
   * The official LSC task type template
   */
  public static LSC_TEMPLATE = {
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
  } as TaskType;

  types: Observable<Array<TaskType>> = new Observable<Array<TaskType>>((o) => o.next([]))

  constructor(builderService: CompetitionBuilderService,
              private dialog: MatDialog,
              private snackbar: MatSnackBar) {
    super(builderService);
  }

  public addTaskType(type?: TaskType) {
    const dialogRef = this.dialog.open(CompetitionBuilderTaskTypeDialogComponent, { data: type ? type : null, width: '750px' });
    dialogRef
        .afterClosed()
        .pipe(filter((g) => g != null))
        .subscribe((g) => {
          this.competition.taskTypes.push(g);
          this.update()
        });
  }

  /**
   * Removes a task type and all associated task groups.
   *
   * @param taskType The {@link TaskType} to remove.
   */
  public removeTaskType(taskType: TaskType) {
    this.builderService.removeTaskType(taskType)
  }

  /**
   * Summarises a task type to present detailed info as tooltip.
   *
   * @param taskType The {@link TaskType} to summarize.
   */
  summariseTaskType(taskType: TaskType): string {
    return `Consists of ${taskType.components.map((c) => c.option).join(', ')}, has filters: ${taskType.filter
        .map((f) => f.option)
        .join(', ')} and options: ${taskType.options.map((o) => o.option).join(', ')}`;
  }

  ngOnInit(): void {
    this.onInit()
  }

  onChange(competition: RestCompetitionDescription) {
    this.types = new Observable<Array<TaskType>>((o) => {
      if(competition){
        o.next(competition.taskTypes)
      }else{
        o.next([])
      }
    })
  }

  ngOnDestroy(): void {
    this.onDestroy()
  }

}
