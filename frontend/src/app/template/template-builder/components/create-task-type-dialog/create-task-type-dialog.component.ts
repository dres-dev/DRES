import { AfterViewInit, Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AbstractControl, UntypedFormArray, UntypedFormControl, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatCheckboxChange } from '@angular/material/checkbox';
import {
  ApiHintOption,
  ApiScoreOption,
  ApiSubmissionOption,
  ApiTargetOption,
  ApiTaskOption,
  ApiTaskType,
} from '../../../../../../openapi';

/**
 * Wrapper to be able to have an enum value boolean tuple
 */
interface ActivatedType<T> {
  type: T;
  activated: boolean;
}

/**
 * Data that can be passed to the [CreateTaskTypeDialogComponent]. For backwards compatibility, a plain [ApiTaskType] (or null) is accepted as well.
 */
export interface CreateTaskTypeDialogData {
  /** The task type to start from (preset, or the task type to edit). */
  taskType?: ApiTaskType;
  /** True, if an existing task type is edited (instead of a new one being created). */
  edit?: boolean;
  /** Names of the task types that already exist in the template (used to prevent duplicate names). */
  existingNames?: string[];
}

@Component({
  selector: 'app-create-task-type',
  standalone: false,
  templateUrl: './create-task-type-dialog.component.html',
  styleUrls: ['./create-task-type-dialog.component.scss'],
})
export class CreateTaskTypeDialogComponent implements OnInit, AfterViewInit {
  /** FromGroup for this dialog. */
  form: UntypedFormGroup;

  /**
   * Dynamically generated list of all target types. Since TargetType is an enum, values is required as this is the "underscore sensitive"
   * version. Object.keys() strips the underscores from the names.
   */
  targetTypes = Object.values(ApiTargetOption).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
  componentTypes = Object.values(ApiHintOption)
    .sort((a, b) => a.localeCompare(b))
    .map((v) => {
      return { type: v, activated: false } as ActivatedType<ApiHintOption>;
    });
  scoreTypes = Object.values(ApiScoreOption).sort((a, b) => a.localeCompare(b));
  filterTypes = Object.values(ApiSubmissionOption)
    .sort((a, b) => a.localeCompare(b))
    .map((v) => {
      return { type: v, activated: false } as ActivatedType<ApiSubmissionOption>;
    });
  options = Object.values(ApiTaskOption)
    .sort((a, b) => a.localeCompare(b))
    .map((v) => {
      return { type: v, activated: false } as ActivatedType<ApiTaskOption>;
    });

  /** The task type this dialog was opened with. */
  data: ApiTaskType;

  /** True, if an existing task type is edited. */
  editMode = false;

  /** Names that must not be used for this task type (other task types in the template). */
  private reservedNames: string[] = [];

  constructor(
    public dialogRef: MatDialogRef<CreateTaskTypeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiTaskType | CreateTaskTypeDialogData
  ) {
    if (dialogData && ('taskType' in dialogData || 'edit' in dialogData || 'existingNames' in dialogData)) {
      const d = dialogData as CreateTaskTypeDialogData;
      this.data = d.taskType ?? null;
      this.editMode = d.edit ?? false;
      this.reservedNames = (d.existingNames ?? []).filter((n) => !(this.editMode && n === this.data?.name));
    } else {
      this.data = (dialogData as ApiTaskType) ?? null;
    }
    this.init();
  }

  /**
   * Listens for changes on a checkbox and reflects this change in the form group
   * @param e
   * @param name
   */
  onCheckboxChange(e: MatCheckboxChange, name: string) {
    const arr: UntypedFormArray = this.form.get(name) as UntypedFormArray;

    const idx = arr.controls.findIndex((item) => item.value === e.source.value);
    if (e.checked) {
      if (idx === -1) {
        arr.push(new UntypedFormControl(e.source.value));
      }
    } else if (idx > -1) {
      arr.removeAt(idx);
    }
  }

  uploaded = (data: string | ApiTaskType) => {
    const parsed = (typeof data === 'string' ? JSON.parse(data) : data) as ApiTaskType;
    this.data = parsed;
    this.init();
  };

  ngOnInit(): void {
    // Loop over all enums
    this.componentTypes.forEach((ct) => {
      // if its in data, set to true to render it as checked
      if (this.data?.hintOptions.find((p) => p === ct.type)) {
        ct.activated = true;
      }
    });

    this.filterTypes.forEach((t) => {
      if (this.data?.submissionOptions?.find((p) => p === t.type)) {
        t.activated = true;
      }
    });

    this.options.forEach((t) => {
      if (this.data?.taskOptions?.find((p) => p === t.type)) {
        t.activated = true;
      }
    });
  }

  /**
   * Adds a new parameter entry to the list of parameter entries.
   */
  public addParameter() {
    (this.form.get('parameters') as UntypedFormArray).push(
      new UntypedFormArray([new UntypedFormControl(null), new UntypedFormControl(null), new UntypedFormControl(null)])
    );
  }

  /**
   * Removes a parameter entry from the list of parameter entries.
   */
  public removeParameter(entry: UntypedFormArray) {
    const array = this.form.get('parameters') as UntypedFormArray;
    const index = array.controls.indexOf(entry);
    if (index >= 0) {
      array.removeAt(index);
    }
  }

  /**
   * Returns a list of all available domains.
   */
  public availableDomains(): string[] {
    const array = [];
    array.push(this.form.get('target').value);
    array.push(this.form.get('scoring').value);
    (this.form.get('components') as UntypedFormArray).controls.forEach((c) => array.push(c.value));
    (this.form.get('filters') as UntypedFormArray).controls.forEach((c) => array.push(c.value));
    (this.form.get('options') as UntypedFormArray).controls.forEach((c) => array.push(c.value));
    return array;
  }

  ngAfterViewInit(): void {}

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.valid) {
      this.dialogRef.close(this.fetchFromForm());
    }
  }

  close(): void {
    this.dialogRef.close(null);
  }

  fileProvider = () => (this.fetchFromForm()?.name ? this.fetchFromForm().name : 'tasktype-download.json');

  downloadProvider = () => JSON.stringify(this.fetchFromForm());

  import(): void {
    // TODO
  }

  private init() {
    const parameters: Array<[string, string, string]> = [];
    /* Load all configuration parameters. */
    for (let configurationKey in this.data?.configuration) {
      const keyParts = configurationKey.split('.');
      const param: [string, string, string] = [
        keyParts[0] as ApiSubmissionOption,
        keyParts[1],
        this?.data.configuration[configurationKey],
      ];
      parameters.push(param);
    }

    /* Prepare empty FormControl. */
    this.form = new UntypedFormGroup({
      /* Name. Required */
      name: new UntypedFormControl(this.data?.name, [Validators.required, Validators.minLength(3), this.uniqueNameValidator]),

      /* Default Duration. Required */
      defaultTaskDuration: new UntypedFormControl(this.data?.duration, [Validators.min(1), Validators.max(999999)]),

      /* Target Type. Required */
      target: new UntypedFormControl(this.data?.targetOption, [Validators.required]),

      /* Components: Required, at least one */
      components: this.data?.hintOptions
        ? new UntypedFormArray(
            this.data?.hintOptions?.map((v) => new UntypedFormControl(v)),
            [Validators.minLength(1)]
          )
        : new UntypedFormArray([]),

      /* Scoring: Required */
      scoring: new UntypedFormControl(this.data?.scoreOption, [Validators.required]),

      /* Submission Filters: Optional*/
      filters: this.data?.submissionOptions
        ? new UntypedFormArray(this.data.submissionOptions.map((v) => new UntypedFormControl(v)))
        : new UntypedFormArray([]),

      /* Options: Optional */
      options: this.data?.taskOptions
        ? new UntypedFormArray(this.data.taskOptions.map((v) => new UntypedFormControl(v)))
        : new UntypedFormArray([]),

      /* Parameters: Optional */
      parameters: new UntypedFormArray(
        parameters.map(
          (v) =>
            new UntypedFormArray([new UntypedFormControl(v[0]), new UntypedFormControl(v[1]), new UntypedFormControl(v[2])])
        )
      ),
    });
  }

  /** Validator that prevents the use of a name that is already used by another task type of the template. */
  private uniqueNameValidator = (control: AbstractControl): ValidationErrors | null => {
    const value = (control.value ?? '').toString().trim();
    return this.reservedNames?.some((n) => n.trim() === value) ? { duplicate: true } : null;
  };

  /**
   * Creates the [TaskType] object from the form data and returns it.
   */
  private fetchFromForm(): ApiTaskType {
    return {
      name: (this.form.get('name').value ?? '').trim(),
      duration: this.form.get('defaultTaskDuration').value ?? null,
      targetOption: this.form.get('target').value as ApiTargetOption,
      hintOptions: (this.form.get('components') as UntypedFormArray).controls.map((c) => {
        return c.value as ApiHintOption;
      }) as Array<ApiHintOption>,
      scoreOption: this.form.get('scoring').value as ApiScoreOption,
      submissionOptions: (this.form.get('filters') as UntypedFormArray).controls.map((c) => {
        return c.value as ApiSubmissionOption;
      }) as Array<ApiSubmissionOption>,
      taskOptions: (this.form.get('options') as UntypedFormArray).controls.map((c) => {
        return c.value as ApiTaskOption;
      }) as Array<ApiTaskOption>,
      configuration: this.fetchConfigurationParameters(),
    } as ApiTaskType;
  }

  /**
   * Fetches the named configuration parameters for the given domain.
   *
   * @param domain The domain to fetch the parameters for.
   * @private The object encoding the named paramters.
   */
  private fetchConfigurationParameters(): any {
    const obj = {};
    (this.form.get('parameters') as UntypedFormArray).controls.forEach((c) => {
      const cast = (c as UntypedFormArray).controls;
      obj[`${cast[0].value}.${cast[1].value}`] = cast[2].value;
    });
    return obj;
  }
}
