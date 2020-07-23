import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {TaskType} from '../../../../openapi';
import {FormArray, FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import TargetTypeEnum = TaskType.TargetTypeEnum;
import ComponentsEnum = TaskType.ComponentsEnum;
import ScoreEnum = TaskType.ScoreEnum;
import FilterEnum = TaskType.FilterEnum;
import OptionsEnum = TaskType.OptionsEnum;
import {MatCheckboxChange} from '@angular/material/checkbox';

@Component({
    selector: 'app-competition-builder-task-type',
    templateUrl: './competition-builder-task-type-dialog.component.html',
    styleUrls: ['./competition-builder-task-type-dialog.component.scss']
})
export class CompetitionBuilderTaskTypeDialogComponent implements OnInit {

    /** FromGroup for this dialog. */
    form: FormGroup;
    targetTypes = Object.keys(TargetTypeEnum).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
    componentTypes = Object.keys(ComponentsEnum).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
    scoreTypes = Object.keys(ScoreEnum).sort((a, b) => a.localeCompare(b));
    filterTypes = Object.keys(FilterEnum).sort((a, b) => a.localeCompare(b));
    options = Object.keys(OptionsEnum).sort((a, b) => a.localeCompare(b));

    constructor(
        public dialogRef: MatDialogRef<CompetitionBuilderTaskTypeDialogComponent>,
        private formBuilder: FormBuilder,
        @Inject(MAT_DIALOG_DATA) public data: TaskType) {

        this.form = new FormGroup({
            /* Name. Required */
            name: new FormControl(data?.name, [Validators.required, Validators.minLength(3)]),
            /* Default Duration. Required */
            defaultTaskDuration: new FormControl(data?.taskDuration, [Validators.required, Validators.min(1)]),
            /* Target Type. Required */
            target: new FormControl(data?.targetType, [Validators.required]),
            /* Components. Required, at least one */
            components: formBuilder.array([]), // TODO deserialization: how to check it?
            /* Scoring. Required */
            scoring: new FormControl(data?.score, [Validators.required]),
            /* Submission Filters. Optional */
            filters: formBuilder.array([]), // TODO deserialization: how to check it?
            /* Options. Optional */
            options: formBuilder.array([]) // TODO deserialization: how to check it?
        });

    }

    onCheckboxChange(e: MatCheckboxChange, name: string) {
        const arr: FormArray = this.form.get(name) as FormArray;

        if (e.checked) {
            arr.push(new FormControl(e.source.value));
        } else {
            let i = 0;
            arr.controls.forEach((item: FormControl) => {
                if (item.value === e.source.value) {
                    arr.removeAt(i);
                    return;
                }
                i++;
            });
        }

        console.log(this.form);
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
