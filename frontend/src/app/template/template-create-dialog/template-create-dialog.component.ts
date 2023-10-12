import { Component } from '@angular/core';
import { FormControl, FormGroup, UntypedFormControl, UntypedFormGroup } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { ApiCreateEvaluation } from "../../../../openapi";

@Component({
  selector: 'app-template-create-dialog',
  templateUrl: './template-create-dialog.component.html',
  styleUrls: ['./template-create-dialog.component.scss']
})
export class TemplateCreateDialogComponent {
  form: FormGroup = new FormGroup({
  name: new FormControl('', {nonNullable: true}),
  description: new FormControl('', {nonNullable: true}),
});

  participantsCanView = true;

  constructor(public dialogRef: MatDialogRef<TemplateCreateDialogComponent>) {
  }

  public create(){
    if(this.form.valid){
      this.dialogRef.close({
        name: this.form.get('name').value,
        description: this.form.get('description').value,
      } as ApiCreateEvaluation);
    }
  }

  public close(){
    this.dialogRef.close(null);
  }

}
