import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {TaskGroup, TaskType} from '../../../../openapi';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import TargetTypeEnum = TaskType.TargetTypeEnum;

@Component({
  selector: 'app-competition-builder-task-type',
  templateUrl: './competition-builder-task-type-dialog.component.html',
  styleUrls: ['./competition-builder-task-type-dialog.component.scss']
})
export class CompetitionBuilderTaskTypeDialogComponent implements OnInit {

  /** FromGroup for this dialog. */
  form: FormGroup;
  targetTypes = [TargetTypeEnum.JUDGEMENT, TargetTypeEnum.MULTIPLEMEDIAITEMS, TargetTypeEnum.SINGLEMEDIAITEM, TargetTypeEnum.SINGLEMEDIASEGMENT]; // TODO iterate automatically?
  componentTypes = [TaskType.ComponentsEnum.EXTERNALIMAGE, TaskType.ComponentsEnum.EXTERNALVIDEO, TaskType.ComponentsEnum.IMAGEITEM, TaskType.ComponentsEnum.TEXT, TaskType.ComponentsEnum.VIDEOITEMSEGMENT];
  scoreTypes = [TaskType.ScoreEnum.AVS, TaskType.ScoreEnum.KIS];
  filterTypes = [TaskType.FilterEnum.NODUPLICATES, TaskType.FilterEnum.ONECORRECTPERTEAM, TaskType.FilterEnum.TEMPORALSUBMISSION];
  options = [TaskType.OptionsEnum.HIDDENRESULTS, TaskType.OptionsEnum.MAPTOSEGMENT];

  constructor(public dialogRef: MatDialogRef<CompetitionBuilderTaskTypeDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: TaskType) {

    this.form = new FormGroup({
      name: new FormControl(data?.name, [Validators.required, Validators.minLength(3)]),
      defaultTaskDuration: new FormControl(data?.taskDuration, [Validators.required, Validators.min(1)])
    });

  }

  ngOnInit(): void {
  }

  public save(): void {
    if (this.form.valid) {
      // TODO
    }
  }

  public close(): void {
    this.dialogRef.close(null);
  }

}
