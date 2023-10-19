import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import {ApiTaskGroup, ApiTaskType} from '../../../../../openapi';

export interface CompetitionBuilderTaskGroupDialogData {
  types: ApiTaskType[];
  group?: ApiTaskGroup;
}

@Component({
  selector: 'app-competition-builder-task-group-dialog',
  templateUrl: './competition-builder-task-group.component.html',
})
export class CompetitionBuilderTaskGroupDialogComponent {
  /** List of task types currently supported by the UI. */
  readonly supportedTaskTypes: ApiTaskType[] = [];

  /** FromGroup for this dialog. */
  form: UntypedFormGroup;

  constructor(
    public dialogRef: MatDialogRef<CompetitionBuilderTaskGroupDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderTaskGroupDialogData
  ) {
    this.supportedTaskTypes = this.data?.types;
    this.init();
  }

  public save(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.fetchFormData());
    }
  }

  fetchFormData() {
    return {
      name: this.form.get('name').value,
      type: (this.form.get('type').value as ApiTaskType).name,
    } as ApiTaskGroup;
  }

  fileProvider = () => (this.fetchFormData()?.name ? this.fetchFormData().name : 'task-group-download.json');

  downloadProvider = () => JSON.stringify(this.fetchFormData());

  uploaded = (data: string) => {
    const parsed = JSON.parse(data) as ApiTaskGroup;
    this.data.group = parsed;
    this.init();
    console.log('Loaded task group: ' + JSON.stringify(parsed));
  };

  public close(): void {
    this.dialogRef.close(null);
  }

  private init() {
    this.form = new UntypedFormGroup({
      name: new UntypedFormControl(this.data?.group?.name, [Validators.required, Validators.minLength(3)]),
      type: new UntypedFormControl(this.typeFromName(this.data?.group?.type), [Validators.required]),
    });
  }

  private typeFromName(name) {
    return this.supportedTaskTypes.find((t) => t.name === name);
  }
}
