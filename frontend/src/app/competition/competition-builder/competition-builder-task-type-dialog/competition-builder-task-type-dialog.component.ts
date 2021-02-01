import {AfterViewInit, Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {
    ConfiguredOptionOptions,
    ConfiguredOptionQueryComponentType,
    ConfiguredOptionScoringType,
    ConfiguredOptionSubmissionFilterType,
    ConfiguredOptionTargetType,
    TaskGroup,
    TaskType
} from '../../../../../openapi';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {MatCheckboxChange} from '@angular/material/checkbox';


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
     * Dynamically generated list of all target types. Since TargetType is an enum, values is required as this is the "underscore sensitive"
     * version. Object.keys() strips the underscores from the names.
     */
    targetTypes = Object.values(ConfiguredOptionTargetType.OptionEnum).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
    componentTypes = Object.values(ConfiguredOptionQueryComponentType.OptionEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<ConfiguredOptionQueryComponentType.OptionEnum>;
        });
    scoreTypes = Object.values(ConfiguredOptionScoringType.OptionEnum).sort((a, b) => a.localeCompare(b));
    filterTypes = Object.values(ConfiguredOptionSubmissionFilterType.OptionEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<ConfiguredOptionSubmissionFilterType.OptionEnum>;
        });
    options = Object.values(ConfiguredOptionOptions.OptionEnum)
        .sort((a, b) => a.localeCompare(b))
        .map((v) => {
            return {type: v, activated: false} as ActivatedType<ConfiguredOptionOptions.OptionEnum>;
        });

    constructor(
        public dialogRef: MatDialogRef<CompetitionBuilderTaskTypeDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: TaskType) {
        this.init();
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

    uploaded = (data: string) => {
        const parsed = JSON.parse(data) as TaskType;
        this.data = parsed;
        this.init();
        console.log('Loaded task group: ' + JSON.stringify(parsed));
    }

    ngOnInit(): void {
        // Loop over all enums
        this.componentTypes.forEach(ct => {
            // if its in data, set to true to render it as checked
            if (this.data?.components.find(p => p.option === ct.type)) {
                ct.activated = true;
            }
        });

        this.filterTypes.forEach(t => {
            if (this.data?.filter?.find(p => p.option === t.type)) {
                t.activated = true;
            }
        });

        this.options.forEach(t => {
            if (this.data?.options?.find(p => p.option === t.type)) {
                t.activated = true;
            }
        });
    }

    /**
     * Adds a new parameter entry to the list of parameter entries.
     */
    public addParameter() {
        (this.form.get('parameters') as FormArray).controls.push(
            new FormArray([
                new FormControl(null),
                new FormControl(null),
                new FormControl(null)
            ])
        );
    }

    /**
     * Removes a parameter entry from the list of parameter entries.
     */
    public removeParameter(entry: FormArray) {
        const array = (this.form.get('parameters') as FormArray).controls;
        const index = array.indexOf(entry);
        if (index >= 0) {
            array.splice(index, 1);
        }
    }

    /**
     * Returns a list of all available domains.
     */
    public availableDomains(): string[] {
        const array = [];
        array.push(this.form.get('target').value);
        array.push(this.form.get('scoring').value);
        (this.form.get('components') as FormArray).controls.forEach(c => array.push(c.value));
        (this.form.get('filters') as FormArray).controls.forEach(c => array.push(c.value));
        (this.form.get('options') as FormArray).controls.forEach(c => array.push(c.value));
        return array;
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

    private init() {
        /* Load all configuration parameters. */
        const parameters: Array<[string, string, string]> = [];
        if (this.data?.targetType?.parameters) {
            Object.keys(this.data?.targetType?.parameters).forEach(key => {
                parameters.push([this.data.score.option, key, this.data.score.parameters[key]]);
            });
        }

        if (this.data?.score?.parameters) {
            Object.keys(this.data?.score?.parameters).forEach(key => {
                parameters.push([this.data.score.option, key, this.data.score.parameters[key]]);
            });
        }

        this.data?.components?.forEach(domain => {
            Object.keys(domain.parameters).forEach(key => {
                parameters.push([domain.option, key, domain.parameters[key]]);
            });
        });

        this.data?.filter?.forEach(domain => {
            Object.keys(domain.parameters).forEach(key => {
                parameters.push([domain.option, key, domain.parameters[key]]);
            });
        });

        this.data?.options?.forEach(domain => {
            Object.keys(domain.parameters).forEach(key => {
                parameters.push([domain.option, key, domain.parameters[key]]);
            });
        });

        /* Prepare empty FormControl. */
        this.form = new FormGroup({
            /* Name. Required */
            name: new FormControl(this.data?.name, [Validators.required, Validators.minLength(3)]),

            /* Default Duration. Required */
            defaultTaskDuration: new FormControl(this.data?.taskDuration, [Validators.required, Validators.min(1)]),

            /* Target Type. Required */
            target: new FormControl(this.data?.targetType?.option, [Validators.required]),

            /* Components: Required, at least one */
            components: this.data?.components ? new FormArray(this.data?.components?.map(
                (v) => new FormControl(v.option)
            ), [Validators.minLength(1)]) : new FormArray([]),

            /* Scoring: Required */
            scoring: new FormControl(this.data?.score?.option, [Validators.required]),

            /* Submission Filters: Optional*/
            filters: this.data?.filter ? new FormArray(this.data.filter.map((v) => new FormControl(v.option))) : new FormArray([]),

            /* Options: Optional */
            options: this.data?.options ? new FormArray(this.data.options.map((v) =>
                new FormControl(v.option))) : new FormArray([]),

            /* Parameters: Optional */
            parameters: new FormArray(parameters.map((v) =>
                new FormArray([new FormControl(v[0]), new FormControl(v[1]), new FormControl(v[2])])))
        });
    }

    /**
     * Creates the [TaskType] object from the form data and returns it.
     */
    private fetchFromForm(): TaskType {
        return {
            name: this.form.get('name').value,
            taskDuration: this.form.get('defaultTaskDuration').value,
            targetType: {
                option: this.form.get('target').value,
                parameters: this.fetchConfigurationParameters(this.form.get('scoring').value)
            } as ConfiguredOptionTargetType,
            components: (this.form.get('components') as FormArray).controls.map(c => {
                return {option: c.value, parameters: this.fetchConfigurationParameters(c.value)};
            }) as Array<ConfiguredOptionQueryComponentType>,
            score: {
                option: this.form.get('scoring').value,
                parameters: this.fetchConfigurationParameters(this.form.get('scoring').value)
            } as ConfiguredOptionScoringType,
            filter: (this.form.get('filters') as FormArray).controls.map(c => {
                return {option: c.value, parameters: this.fetchConfigurationParameters(c.value)};
            }) as Array<ConfiguredOptionSubmissionFilterType>,
            options: (this.form.get('options') as FormArray).controls.map(c => {
                return {option: c.value, parameters: this.fetchConfigurationParameters(c.value)};
            }) as Array<ConfiguredOptionOptions>
        } as TaskType;
    }

    /**
     * Fetches the named configuration parameters for the given domain.
     *
     * @param domain The domain to fetch the parameters for.
     * @private The object encoding the named paramters.
     */
    private fetchConfigurationParameters(domain: string): any {
        const obj = {};
        (this.form.get('parameters') as FormArray).controls.forEach(c => {
            const cast = (c as FormArray).controls;
            if (cast[0].value === domain) {
                obj[cast[1].value] = cast[2].value;
            }
        });
        return obj;
    }
}
