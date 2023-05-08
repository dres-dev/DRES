import { Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import {ApiEvaluationType} from '../../../../openapi';

export interface CompetitionStartDialogResult {
  name: string;
  type: ApiEvaluationType;
  participantCanView: boolean;
  allowRepeatedTasks: boolean;
  shuffleTasks: boolean;
  limit: Number;
}

/**
 * @deprecated Replaced by TemplateStart
 */
@Component({
  selector: 'app-competition-start-dialog',
  templateUrl: 'competition-start-dialog.component.html',
})
export class CompetitionStartDialogComponent {
  form: UntypedFormGroup = new UntypedFormGroup({
    name: new UntypedFormControl(''),
    type: new UntypedFormControl(''),
    participantsCanView: new UntypedFormControl(true),
    shuffleTasks: new UntypedFormControl(false),
    allowRepeatedTasks: new UntypedFormControl(false),
    limit: new UntypedFormControl(0)
  });
  runTypes: ApiEvaluationType[] = ['SYNCHRONOUS', 'ASYNCHRONOUS'];

  typeObservable = this.form.get('type').valueChanges;

  constructor(public dialogRef: MatDialogRef<CompetitionStartDialogComponent>) {}

  public create(): void {
    if (this.form.valid) {
      this.dialogRef.close({
        name: this.form.get('name').value,
        type: this.form.get('type').value,
        participantCanView: this.form.get('participantsCanView').value,
        allowRepeatedTasks: this.form.get('allowRepeatedTasks').value,
        shuffleTasks: this.form.get('shuffleTasks').value,
        limit: this.form.get('limit').value
      } as CompetitionStartDialogResult);
    }
  }

  public close(): void {
    this.dialogRef.close(null);
  }
}
