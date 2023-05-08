import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import {ApiCreateEvaluation} from '../../../../openapi';

/**
 * @deprecated Replaced by TemplateCreateDialog
 */
@Component({
  selector: 'app-competition-create-dialog',
  templateUrl: 'competition-create-dialog.component.html',
})
export class CompetitionCreateDialogComponent {
  form: UntypedFormGroup = new UntypedFormGroup({
    name: new UntypedFormControl(''),
    description: new UntypedFormControl(''),
  });

  participantsCanView = true;

  constructor(public dialogRef: MatDialogRef<CompetitionCreateDialogComponent>) {}

  public create(): void {
    if (this.form.valid) {
      this.dialogRef.close({
        name: this.form.get('name').value,
        description: this.form.get('description').value,
      } as ApiCreateEvaluation);
    }
  }

  public close(): void {
    this.dialogRef.close(null);
  }
}
