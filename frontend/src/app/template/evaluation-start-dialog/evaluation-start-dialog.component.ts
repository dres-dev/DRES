import { Component } from '@angular/core';
import { FormControl, FormGroup } from "@angular/forms";
import { ApiEvaluationType } from "../../../../openapi";
import { MatDialogRef } from "@angular/material/dialog";

export interface EvaluationStartDialogResult {
  name: string;
  type: ApiEvaluationType;
  participantsCanView: boolean;
  allowRepeatedTasks: boolean;
  shuffleTasks: boolean;
  limit: number;
}

@Component({
  selector: 'app-evaluation-start-dialog',
  templateUrl: './evaluation-start-dialog.component.html',
  styleUrls: ['./evaluation-start-dialog.component.scss']
})
export class EvaluationStartDialogComponent {

  form: FormGroup = new FormGroup({
    name: new FormControl('', {nonNullable: true}),
    type: new FormControl('', {nonNullable: true}),
    participantsCanView: new FormControl(true),
    shuffleTasks: new FormControl(false),
    allowRepeatedTasks: new FormControl(false),
    limit: new FormControl(0, {nonNullable: true})
  })

  evaluationTypes: ApiEvaluationType[] = [ApiEvaluationType.SYNCHRONOUS, ApiEvaluationType.ASYNCHRONOUS];

  typeObservable = this.form.get('type').valueChanges;

  constructor(public dialogRef: MatDialogRef<EvaluationStartDialogComponent>) {
  }

  public create(){
    if(this.form.valid){
      this.dialogRef.close({
        name: this.form.get('name').value,
        type: this.form.get('type').value,
        participantsCanView: this.form.get('participantsCanView').value,
        allowRepeatedTasks: this.form.get('allowRepeatedTasks').value,
        shuffleTasks: this.form.get('shuffleTasks').value,
        limit: this.form.get('limit').value,
      } as EvaluationStartDialogResult);
    }
  }

  public close(){
    this.dialogRef.close(null);
  }

}
