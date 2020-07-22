import {Component, Inject} from '@angular/core';
import {TaskGroup} from '../../../../openapi';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormControl, FormGroup, Validators} from '@angular/forms';


@Component({
    selector: 'app-competition-builder-task-group-dialog',
    templateUrl: './competition-builder-task-group.component.html'
})
export class CompetitionBuilderTaskGroupDialogComponent {

    /** List of task types currently supported by the UI. */
    readonly supportedTaskTypes = []; // TODO read from created task groups

    /** FromGroup for this dialog. */
    form: FormGroup;

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTaskGroupDialogComponent>,
                @Inject(MAT_DIALOG_DATA) public data: TaskGroup) {

        this.form = new FormGroup({
            name: new FormControl(data?.name, [Validators.required, Validators.minLength(3)]),
            type: new FormControl(data?.type, [Validators.required])// ,
            // defaultTaskDuration: new FormControl(data?.defaultTaskDuration, [Validators.required, Validators.min(1)])
        });
    }

    public save(): void {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.get('name').value,
                type: this.form.get('type').value,
                defaultTaskDuration: this.form.get('defaultTaskDuration').value,
            } as TaskGroup);
        }
    }

    public close(): void {
        this.dialogRef.close(null);
    }
}
