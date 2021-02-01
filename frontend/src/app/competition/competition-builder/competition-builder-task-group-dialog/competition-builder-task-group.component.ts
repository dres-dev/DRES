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
            type: (this.form.get('type').value as TaskType).name
        } as TaskGroup;
    }

    fileProvider = () => this.fetchFormData()?.name ? this.fetchFormData().name : 'task-group-download.json';

    downloadProvider = () => JSON.stringify(this.fetchFormData());

    uploaded = (data: string) => {
        const parsed = JSON.parse(data) as TaskGroup;
        this.data.group = parsed;
        this.init();
        console.log('Loaded task group: ' + JSON.stringify(parsed));
    }

    public close(): void {
        this.dialogRef.close(null);
    }

    private init() {
        this.form = new FormGroup({
            name: new FormControl(this.data?.group?.name, [Validators.required, Validators.minLength(3)]),
            type: new FormControl(this.typeFromName(this.data?.group?.type), [Validators.required])
        });
    }

    private typeFromName(name) {
        return this.supportedTaskTypes.find(t => t.name === name);
    }
}
