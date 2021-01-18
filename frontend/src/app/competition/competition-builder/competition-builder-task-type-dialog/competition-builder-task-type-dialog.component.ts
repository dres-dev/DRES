import {AfterViewInit, Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {TaskType} from '../../../../../openapi';
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
    /**
     * Dynamically generated list of all target types. Since TargetType is an enum, values is required as this is the "underscore sensitive" version.
     * Object.keys() strips the underscores from the names.
     */
    targetTypes = Object.values(TargetTypeEnum).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
    componentTypes = Object.values(ComponentsEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<ComponentsEnum>;
        });
    scoreTypes = Object.values(ScoreEnum).sort((a, b) => a.localeCompare(b));
    filterTypes = Object.values(FilterEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<FilterEnum>;
        });
    options = Object.values(OptionsEnum)
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
            components: this.data?.components ? new FormArray(this.data?.components?.map((v) => new FormControl(v)), [Validators.minLength(1)]) : new FormArray([]),
            /* Scoring. Required */
            scoring: new FormControl(this.data?.score, [Validators.required]),
            /* Submission Filters. Optional */
            filters: this.data?.filter ? new FormArray(this.data?.filter?.map((v) => new FormControl(v))) : new FormArray([]),
            /* Options. Optional */
            options: this.data?.options ? new FormArray(this.data?.options?.map((v) => new FormControl(v))) : new FormArray([])
        });
    }

    /**
     * Listens for changes on a checkbox and reflects this change in the form group
     * @param e
     * @param name
     */
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
            if (this.data?.filter?.find(p => p === t.type)) {
                t.activated = true;
            }
        });

        this.options.forEach(t => {
            if (this.data?.options?.find(p => p === t.type)) {
                t.activated = true;
            }
        });
    }

    ngAfterViewInit(): void {
    }

    save(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.fetchFromForm());
        }
    }

    close(): void {
        this.dialogRef.close(null);
    }

    fileProvider = () => this.fetchFromForm()?.name ? this.fetchFromForm().name : 'tasktype-download.json';
    downloadProvider = () => JSON.stringify(this.fetchFromForm());

    import(): void {
        // TODO
    }


    /**
     * Fetches the resulting [TaskType] from the form data
     */
    private fetchFromForm(): TaskType {
        return {
            name: this.form.get('name').value,
            taskDuration: this.form.get('defaultTaskDuration').value,
            targetType: this.form.get('target').value,
            components: this.form.get('components').value,
            score: this.form.get('scoring').value,
            filter: this.form.get('filters').value,
            options: this.form.get('options').value
        } as TaskType;
    }
}
