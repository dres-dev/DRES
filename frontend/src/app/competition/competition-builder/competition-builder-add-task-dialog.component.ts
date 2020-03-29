import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Competition, TaskDescription, Team} from '../../../../openapi';
import TaskTypeEnum = TaskDescription.TaskTypeEnum;
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';


export interface CompetitionBuilderAddTaskDialogData {
    type: TaskTypeEnum;
    competition: Competition;
}

@Component({
    selector: 'app-competition-builder-add-task-dialog',
    templateUrl: './competition-builder-add-task-dialog.component.html'
})
export class CompetitionBuilderAddTaskDialogComponent implements OnInit {

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderAddTaskDialogComponent>,
                @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderAddTaskDialogData) {


        this.type = data.type;
        this.form = new FormGroup({
            name: new FormControl('', Validators.required),
            taskGroup: new FormControl('', Validators.required)
        });

        switch (this.type) {
            case 'KIS_VISUAL':
                this.form.addControl('mediaItemId', new FormControl('', [Validators.required, Validators.min(1)]));
                this.form.addControl('start', CompetitionBuilderAddTaskDialogComponent.taskTimePointFormControl());
                this.form.addControl('end', CompetitionBuilderAddTaskDialogComponent.taskTimePointFormControl());
                break;
            case 'KIS_TEXTUAL':
                this.form.addControl('mediaItemId', new FormControl('', [Validators.required, Validators.min(1)]));
                this.form.addControl('start', CompetitionBuilderAddTaskDialogComponent.taskTimePointFormControl());
                this.form.addControl('end', CompetitionBuilderAddTaskDialogComponent.taskTimePointFormControl());
                this.form.addControl('descriptions', new FormArray([CompetitionBuilderAddTaskDialogComponent.taskDescriptionFormControl()], Validators.required));
                this.form.addControl('delay', new FormControl('', [Validators.required, Validators.min(0)]));
                break;
            case 'AVS':
                this.form.addControl('description', CompetitionBuilderAddTaskDialogComponent.taskDescriptionFormControl());
                break;
        }
    }

    form: FormGroup;
    type: TaskTypeEnum;
    units = ['FRAME_NUMBER', 'SECONDS', 'MILLISECONDS', 'TIMECODE'];


    public static taskDescriptionFormControl() {
        return new FormControl('', Validators.minLength(1));
    }

    public static taskTimePointFormControl() {
        return new FormGroup({
            value: new FormControl('', [Validators.required, Validators.min(0)]),
            unit: new FormControl('', [Validators.required, Validators.min(0)])
        }, Validators.required);
    }

    ngOnInit(): void {
    }

    /**
     *
     * @param index
     */
    public addDescription(index: number) {
        (this.form.get('descriptions') as FormArray).insert(index, CompetitionBuilderAddTaskDialogComponent.taskDescriptionFormControl());
    }

    /**
     *
     * @param index
     */
    public removeDescription(index: number) {
        (this.form.get('descriptions') as FormArray).removeAt(index);
    }

    public add(): void {
        if (this.form.valid) {
        }
    }


    public close(): void {
        this.dialogRef.close(null);
    }

}
