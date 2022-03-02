import {AfterViewInit, Component, Input, OnInit} from '@angular/core';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {
  ConfiguredOptionQueryComponentOption,
  ConfiguredOptionScoringOption, ConfiguredOptionSimpleOption, ConfiguredOptionSubmissionFilterOption,
  ConfiguredOptionTargetOption,
  TaskType
} from '../../../../../../openapi';
import {ActivatedType} from '../../../../utilities/data.utilities';

@Component({
  selector: 'app-task-types',
  templateUrl: './task-types.component.html',
  styleUrls: ['./task-types.component.scss']
})
export class TaskTypesComponent implements OnInit, AfterViewInit {

  @Input() data: TaskType;


  /** FromGroup for this dialog. */
  form: FormGroup;

  /**
   * Dynamically generated list of all target types. Since TargetType is an enum, values is required as this is the "underscore sensitive"
   * version. Object.keys() strips the underscores from the names.
   */
  targetTypes = Object.values(ConfiguredOptionTargetOption.OptionEnum).sort((a, b) => a.localeCompare(b)); // sorted alphabetically
  componentTypes = Object.values(ConfiguredOptionQueryComponentOption.OptionEnum)
      .sort((a, b) => a.localeCompare(b))
      .map((v) => {
        return {type: v, activated: false} as ActivatedType<ConfiguredOptionQueryComponentOption.OptionEnum>;
      });
  scoreTypes = Object.values(ConfiguredOptionScoringOption.OptionEnum).sort((a, b) => a.localeCompare(b));
  filterTypes = Object.values(ConfiguredOptionSubmissionFilterOption.OptionEnum)
      .sort((a, b) => a.localeCompare(b))
      .map((v) => {
        return {type: v, activated: false} as ActivatedType<ConfiguredOptionSubmissionFilterOption.OptionEnum>;
      });
  options = Object.values(ConfiguredOptionSimpleOption.OptionEnum)
      .sort((a, b) => a.localeCompare(b))
      .map((v) => {
        return {type: v, activated: false} as ActivatedType<ConfiguredOptionSimpleOption.OptionEnum>;
      });

  constructor() {
    this.init();
  }

  public valid(): boolean {
    return this.form.valid;
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
  public fetchFromForm(): TaskType {
    return {
      name: this.form.get('name').value,
      taskDuration: this.form.get('defaultTaskDuration').value,
      targetType: {
        option: this.form.get('target').value,
        parameters: this.fetchConfigurationParameters(this.form.get('scoring').value)
      } as ConfiguredOptionTargetOption,
      components: (this.form.get('components') as FormArray).controls.map(c => {
        return {option: c.value, parameters: this.fetchConfigurationParameters(c.value)};
      }) as Array<ConfiguredOptionQueryComponentOption>,
      score: {
        option: this.form.get('scoring').value,
        parameters: this.fetchConfigurationParameters(this.form.get('scoring').value)
      } as ConfiguredOptionScoringOption,
      filter: (this.form.get('filters') as FormArray).controls.map(c => {
        return {option: c.value, parameters: this.fetchConfigurationParameters(c.value)};
      }) as Array<ConfiguredOptionSubmissionFilterOption>,
      options: (this.form.get('options') as FormArray).controls.map(c => {
        return {option: c.value, parameters: this.fetchConfigurationParameters(c.value)};
      }) as Array<ConfiguredOptionSimpleOption>
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