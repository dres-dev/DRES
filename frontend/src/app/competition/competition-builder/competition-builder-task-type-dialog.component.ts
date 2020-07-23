import {AfterViewInit, Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {TaskType} from '../../../../openapi';
import {FormArray, FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {MatCheckboxChange} from '@angular/material/checkbox';
import TargetTypeEnum = TaskType.TargetTypeEnum;
import ComponentsEnum = TaskType.ComponentsEnum;
import ScoreEnum = TaskType.ScoreEnum;
import FilterEnum = TaskType.FilterEnum;
import OptionsEnum = TaskType.OptionsEnum;

/**
 * Wrapper to be able to have an enum value boolean tuple
 */
interface ActivatedType<T> {
    type: T;
    activated: boolean;
}

@Component({
    selector: 'app-competition-builder-task-type',
    templateUrl: './competition-builder-task-type-dialog.component.html',
    styleUrls: ['./competition-builder-task-type-dialog.component.scss']
})
export class CompetitionBuilderTaskTypeDialogComponent implements OnInit, AfterViewInit {

    /** FromGroup for this dialog. */
    form: FormGroup;
    targetTypes = Object.keys(TargetTypeEnum).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
    componentTypes = Object.keys(ComponentsEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<ComponentsEnum>;
        });
    scoreTypes = Object.keys(ScoreEnum).sort((a, b) => a.localeCompare(b));
    filterTypes = Object.keys(FilterEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<FilterEnum>;
        });
    options = Object.keys(OptionsEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<OptionsEnum>;
        });

    constructor(
        public dialogRef: MatDialogRef<CompetitionBuilderTaskTypeDialogComponent>,
        private formBuilder: FormBuilder,
        @Inject(MAT_DIALOG_DATA) public data: TaskType) {
        this.form = new FormGroup({
            /* Name. Required */
            name: new FormControl(this.data?.name, [Validators.required, Validators.minLength(3)]),
            /* Default Duration. Required */
            defaultTaskDuration: new FormControl(this.data?.taskDuration, [Validators.required, Validators.min(1)]),
            /* Target Type. Required */
            target: new FormControl(this.data?.targetType, [Validators.required]),
            /* Components. Required, at least one */
            components: new FormArray(this.data?.components.map((v) => new FormControl(v))),
            /* Scoring. Required */
            scoring: new FormControl(this.data?.score, [Validators.required]),
            /* Submission Filters. Optional */
            filters: this.formBuilder.array([]), // TODO deserialization: how to check it?
            /* Options. Optional */
            options: this.formBuilder.array([]) // TODO deserialization: how to check it?
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
    }

    ngOnInit(): void {
      // Loop over all enums
        this.componentTypes.forEach(ct => {
          // if its in data, set to true to render it as checked
            if (this.data?.components.find(p => p === ct.type)) {
                ct.activated = true;
            }
        });

        this.filterTypes.forEach(t => {
            if (this.data?.filter.find(p => p === t.type)) {
                t.activated = true;
            }
        });

        this.options.forEach(t => {
          if(this.data?.options.find(p => p === t.type)){
            t.activated = true;
          }
        });
    }

    ngAfterViewInit(): void {
    }

    public save(): void {
        if (this.form.valid) {
            // TODO
        }
    }

    public close(): void {
        this.dialogRef.close(null);
    }

    public export(): void {
        // Currently on ly debug
        console.log(JSON.stringify(this.form.value));
    }

    public import(): void {
        // TODO
    }
}
