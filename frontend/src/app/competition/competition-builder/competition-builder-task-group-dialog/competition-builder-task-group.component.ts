import {Component, Inject} from '@angular/core';
import {TaskGroup, TaskType} from '../../../../../openapi';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormControl, FormGroup, Validators} from '@angular/forms';

export interface CompetitionBuilderTaskGroupDialogData {
    types: TaskType[];
    group?: TaskGroup;
}

@Component({
    selector: 'app-competition-builder-task-group-dialog',
    templateUrl: './competition-builder-task-group.component.html'
})
export class CompetitionBuilderTaskGroupDialogComponent {

    /** List of task types currently supported by the UI. */
    readonly supportedTaskTypes: TaskType[] = [];

    /** FromGroup for this dialog. */
    form: FormGroup;

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTaskGroupDialogComponent>,
                @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderTaskGroupDialogData) {
        this.supportedTaskTypes = data?.types;

        this.form = new FormGroup({
            name: new FormControl(data?.group?.name, [Validators.required, Validators.minLength(3)]),
            type: new FormControl(this.typeFromName(data?.group?.type), [Validators.required])
        });
    }

    private typeFromName(name){
        return this.supportedTaskTypes.find(t => t.name === name);
    }

    public save(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.fetchFormData());
        }
    }

    fetchFormData() {
        return {
            name: this.form.get('name').value,
            type: (this.form.get('type').value as TaskType).name
        } as TaskGroup;
    }

    export(){
        console.log(JSON.stringify(this.fetchFormData()));
    }

    public close(): void {
        this.dialogRef.close(null);
    }
}
