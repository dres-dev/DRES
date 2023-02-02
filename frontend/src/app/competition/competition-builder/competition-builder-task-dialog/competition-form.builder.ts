import {
  ApiMediaItem,
  ApiTaskGroup, ApiTaskType,
  CollectionService,
} from '../../../../../openapi';
import { AbstractControl, FormArray, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { filter, first, switchMap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { RequireMatch } from './require-match';
import { TimeUtilities } from '../../../utilities/time.utilities';

export class CompetitionFormBuilder {
  /** The default duration of a query hint. This is currently a hard-coded constant. */
  private static DEFAULT_HINT_DURATION = 30;

  /** The {@link FormGroup} held by this {@link CompetitionFormBuilder}. */
  public form: FormGroup;

  /** List of data sources managed by this CompetitionFormBuilder. */
  private dataSources = new Map<string, Observable<ApiMediaItem[] | string[]>>();

  /**
   * Constructor for CompetitionFormBuilder.
   *
   * @param taskGroup The {@link TaskGroup} to create this {@link CompetitionFormBuilder} for.
   * @param taskType The {@link TaskType} to create this {@link CompetitionFormBuilder} for.
   * @param collectionService The {@link CollectionService} reference used to fetch data through the DRES API.
   * @param data The {@link RestTaskDescription} to initialize the form with.
   */
  constructor(
    private taskGroup: ApiTaskGroup,
    private taskType: ApiTaskType,
    private collectionService: CollectionService,
    private data?: RestTaskDescription
  ) {
    this.initializeForm();
  }

  /**
   * Returns the duration value to init with:
   * either the set task duration (if this is for editing)
   * otherwise the default value based on the tasktype default
   */
  private get durationInitValue() {
    if (this?.data?.duration) {
      return this.data.duration;
    } else {
      return this.taskType.taskDuration;
    }
  }

  /**
   * Returns the {@link Observable<MediaItem[]>} for the given key.
   *
   * @param key Key to fetch the data source for.
   */
  public dataSource(key: string): Observable<ApiMediaItem[] | string[]> {
    return this.dataSources.get(key);
  }

  /**
   * Adds a new {@link FormGroup} for the given {@link ConfiguredOptionQueryComponentType.OptionEnum}.
   *
   * @param type The {@link ConfiguredOptionQueryComponentType.OptionEnum} to add a {@link FormGroup} for.
   * @param afterIndex The {@link FormControl} to insert the new {@link FormControl} after.
   */
  public addComponentForm(type: ConfiguredOptionQueryComponentOption.OptionEnum, afterIndex: number = null) {
    const array = this.form.get('components') as FormArray;
    const newIndex = afterIndex ? afterIndex + 1 : array.length;
    let component = null;
    switch (type) {
      case 'IMAGE_ITEM':
        component = this.imageItemComponentForm(newIndex);
        break;
      case 'VIDEO_ITEM_SEGMENT':
        component = this.videoItemComponentForm(newIndex);
        break;
      case 'TEXT':
        component = this.textItemComponentForm(newIndex);
        break;
      case 'EXTERNAL_IMAGE':
        component = this.externalImageItemComponentForm(newIndex);
        break;
      case 'EXTERNAL_VIDEO':
        component = this.externalVideoItemComponentForm(newIndex);
        break;
      default:
        console.error(`Failed to add query hint: Unsupported component type '${type}.`);
        return;
    }

    /* Find previous item in the same channel. */
    let previousItem = null;
    for (let i = newIndex - 1; i >= 0; i--) {
      if (array.get([i]).get('type').value === component.get('type').value) {
        previousItem = array.get([i]); /* Find last item in channel. */
        break;
      }
    }

    /* Initialize new and previous component in channel with default values. */
    if (previousItem == null) {
      component.get('start').setValue(0);
    } else if (previousItem.get('end').value) {
      component.get('start').setValue(previousItem.get('end').value);
    } else {
      previousItem.get('end').setValue(previousItem.get('start').value + CompetitionFormBuilder.DEFAULT_HINT_DURATION);
      component.get('start').setValue(previousItem.get('end').value);
    }

    /* Append component. */
    array.insert(newIndex, component);
  }

  /**
   * Adds a new {@link FormGroup} for the given {@link TaskType.ComponentsEnum}.
   *
   * @param type The {@link TaskType.TargetTypeEnum} to add a {@link FormGroup} for.
   */
  public addTargetForm(type: ConfiguredOptionTargetOption.OptionEnum) {
    const array = this.form.get('target') as FormArray;
    const newIndex = array.length;
    switch (type) {
      case 'MULTIPLE_MEDIA_ITEMS':
        const targetForm = this.singleMediaItemTargetForm(newIndex);
        array.push(targetForm);
        return targetForm;
      case 'TEXT':
        const form = this.singleTextTargetForm();
        array.push(form);
        return form;
      default:
        break;
    }
  }

  /**
   * Removes the {@link FormGroup} at the given index.
   *
   * @param index Index to remove.
   */
  public removeComponentForm(index: number) {
    const array = this.form.get('components') as FormArray;
    if (array.length > index) {
      array.removeAt(index);
    }
  }

  /**
   * Removes the {@link FormGroup} at the given index.
   *
   * @param index Index to remove.
   */
  public removeTargetForm(index: number) {
    const array = this.form.get('target') as FormArray;
    if (array.length > index) {
      array.removeAt(index);
    }
  }

  /**
   * Assembles form data and returns a {@link RestTaskDescription}.
   */
  public fetchFormData(): RestTaskDescription {
    const data = {
      name: this.form.get('name').value,
      taskGroup: this.taskGroup.name /* Cannot be edited! */,
      taskType: this.taskGroup.type /* Cannot be edited! */,
      duration: this.form.get('duration').value,
      mediaCollectionId: this.form.get('mediaCollection').value,
      components: (this.form.get('components') as FormArray).controls.map((c) => {
        return {
          type: c.get('type').value,
          start: c.get('start').value,
          end: c.get('end').value,
          mediaItem: c.get('mediaItem') ? c.get('mediaItem').value.id : null,
          range:
            c.get('segment_start') && c.get('segment_end')
              ? ({
                  start: { value: c.get('segment_start').value, unit: c.get('segment_time_unit').value } as RestTemporalPoint,
                  end: { value: c.get('segment_end').value, unit: c.get('segment_time_unit').value } as RestTemporalPoint,
                } as RestTemporalRange)
              : null,
          description: c.get('description') ? c.get('description').value : null,
          path: c.get('path') ? c.get('path').value : null,
        } as RestTaskDescriptionComponent;
      }),
      target: {
        type: this.taskType.targetType.option,
        mediaItems: (this.form.get('target') as FormArray).controls.map((t) => {
          return {
            mediaItem: t.get('mediaItem').value?.id || t.get('mediaItem').value,
            temporalRange:
              t.get('segment_start') && t.get('segment_start')
                ? ({
                    start: { value: t.get('segment_start').value, unit: t.get('segment_time_unit').value } as RestTemporalPoint,
                    end: { value: t.get('segment_end').value, unit: t.get('segment_time_unit').value } as RestTemporalPoint,
                  } as RestTemporalRange)
                : null,
          } as RestTaskDescriptionTargetItem;
        }),
      } as RestTaskDescriptionTarget,
    } as RestTaskDescription;

    /* Set ID of set. */
    if (this.form.get('id').value) {
      data.id = this.form.get('id').value;
    }

    return data;
  }

  private orValidator(validator1: ValidatorFn, validator2: ValidatorFn): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      return validator1(control) || validator2(control);
    };
  }

  private temporalPointValidator(unitControl: FormControl): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      if (unitControl.value === 'TIMECODE') {
        if (TimeUtilities.timeCodeRegex.test(`${control.value}`)) {
          return null;
        } else {
          return { error: `${control.value} does not conform [[[HH:]MM:]SS:]fff format` };
        }
      } else {
        return Validators.min(0);
      }
    };
  }

  /**
   * Initializes the {@link FormGroup}.
   */
  private initializeForm() {
    this.form = new FormGroup({
      id: new FormControl(this.data?.id),
      name: new FormControl(this.data?.name, [Validators.required]),
      duration: new FormControl(this.durationInitValue, [Validators.required, Validators.min(1)]),
      mediaCollection: new FormControl(this.data?.mediaCollectionId, [Validators.required]),
    });
    this.form.addControl('target', this.formForTarget());
    this.form.addControl('components', this.formForQueryComponents());
  }

  /**
   * Returns the target form for the given {TaskType}
   */
  private formForTarget() {
    switch (this.taskType.targetType.option) {
      case 'SINGLE_MEDIA_ITEM':
        return new FormArray([this.singleMediaItemTargetForm(0, this.data?.target?.mediaItems[0])]);
      case 'MULTIPLE_MEDIA_ITEMS':
        const content: FormGroup[] = [];
        if (this.data?.target) {
          this.data?.target?.mediaItems.forEach((d, i) => content.push(this.singleMediaItemTargetForm(i, d)));
        } else {
          content.push(this.singleMediaItemTargetForm(0));
        }
        return new FormArray(content);
      case 'SINGLE_MEDIA_SEGMENT':
        return new FormArray([this.singleMediaSegmentTargetForm(this.data?.target?.mediaItems[0])]);
      case 'JUDGEMENT':
        return new FormArray([]);
      case 'VOTE':
        return new FormArray([]);
      case 'TEXT':
        const text: FormGroup[] = [];
        if (this.data?.target) {
          console.log(this.data?.target);
          this.data?.target?.mediaItems.forEach((d, i) => text.push(this.singleTextTargetForm(d)));
        } else {
          console.log('no target');
          text.push(this.singleTextTargetForm());
        }
        return new FormArray(text);
    }
  }

  /**
   * Returns FormGroup for a single Media Item Target.
   *
   * @param index Index of the FormControl
   * @param initialize The optional {RestTaskDescriptionTargetItem} containing the data to initialize the form with.
   */
  private singleMediaItemTargetForm(index: number, initialize?: RestTaskDescriptionTargetItem): FormGroup {
    /* Prepare auto complete field. */
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    this.dataSources.set(
      `target.${index}.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.apiV2CollectionCollectionIdStartsWithGet(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.mediaItem && this.data?.mediaCollectionId) {
      this.collectionService
        .apiV2MediaItemMediaIdGet(initialize.mediaItem)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s);
        });
    }

    return new FormGroup({ mediaItem: mediaItemFormControl }, [RequireMatch]);
  }

  /**
   * Returns FormGroup for a single Media Segment Target.
   *
   * @param initialize The optional {RestTaskDescriptionTargetItem} to initialize the form with.
   */
  private singleMediaSegmentTargetForm(initialize?: RestTaskDescriptionTargetItem) {
    /* Prepare auto complete field. */
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);

    this.dataSources.set(
      `target.0.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.apiV2CollectionCollectionIdStartsWithGet(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.mediaItem && this.data.mediaCollectionId) {
      this.collectionService
        .apiV2MediaItemMediaIdGet(initialize.mediaItem)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s);
        });
    }

    const formGroup = new FormGroup({
      mediaItem: mediaItemFormControl,
      segment_start: new FormControl(initialize?.temporalRange.start.value, [Validators.required]),
      segment_end: new FormControl(initialize?.temporalRange.end.value, [Validators.required]),
      segment_time_unit: new FormControl(
        initialize?.temporalRange.start.unit ? initialize?.temporalRange.start.unit : 'SECONDS',
        [Validators.required]
      ),
    });

    formGroup
      .get('segment_start')
      .setValidators([Validators.required, this.temporalPointValidator(formGroup.get('segment_time_unit') as FormControl)]);
    formGroup
      .get('segment_end')
      .setValidators([Validators.required, this.temporalPointValidator(formGroup.get('segment_time_unit') as FormControl)]);
    formGroup.get('segment_start').updateValueAndValidity();
    formGroup.get('segment_end').updateValueAndValidity();

    return formGroup;
  }

  private singleTextTargetForm(initialize?: RestTaskDescriptionTargetItem) {
    const textFormControl = new FormControl(null, [Validators.required]);

    console.log(initialize?.mediaItem);

    textFormControl.setValue(initialize?.mediaItem);

    return new FormGroup({
      mediaItem: textFormControl,
    });
  }

  /**
   * Returns the component form for the given {TaskType}
   */
  private formForQueryComponents() {
    const array = [];
    if (this.data) {
      for (const component of this.data.components) {
        const index = this.data.components.indexOf(component);
        switch (component.type) {
          case 'IMAGE_ITEM':
            array.push(this.imageItemComponentForm(index, component));
            break;
          case 'VIDEO_ITEM_SEGMENT':
            array.push(this.videoItemComponentForm(index, component));
            break;
          case 'TEXT':
            array.push(this.textItemComponentForm(index, component));
            break;
          case 'EXTERNAL_IMAGE':
            array.push(this.externalImageItemComponentForm(index, component));
            break;
          case 'EXTERNAL_VIDEO':
            array.push(this.externalVideoItemComponentForm(index, component));
            break;
        }
      }
    }
    return new FormArray(array, [Validators.required]);
  }

  /**
   * Returns a new image item component {@link FormGroup}.
   *
   * @param index The position of the new {@link FormGroup} (for data source).
   * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
   * @return The new {@link FormGroup}
   */
  private imageItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent): FormGroup {
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    if (
      !initialize?.mediaItem &&
      (this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetType.option === 'SINGLE_MEDIA_ITEM')
    ) {
      mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value);
    }

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.apiV2CollectionCollectionIdStartsWithGet(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.mediaItem && this.data?.mediaCollectionId) {
      this.collectionService
        .apiV2MediaItemMediaIdGet(initialize?.mediaItem)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s);
        });
    }

    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.taskDuration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.taskDuration)]),
      type: new FormControl('IMAGE_ITEM', [Validators.required]),
      mediaItem: mediaItemFormControl,
    });
  }

  /**
   * Returns a new video item component {@link FormGroup}.
   *
   * @param index The position of the new {@link FormGroup} (for data source).
   * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
   * @return The new {@link FormGroup}
   */
  private videoItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent): FormGroup {
    /* Initialize media item based on target. */
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    if (
      !initialize?.mediaItem &&
      (this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetType.option === 'SINGLE_MEDIA_ITEM')
    ) {
      mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value);
    }

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.apiV2CollectionCollectionIdStartsWithGet(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.mediaItem && this.data?.mediaCollectionId) {
      this.collectionService
        .apiV2MediaItemMediaIdGet(initialize.mediaItem)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s);
        });
    }

    /* Prepare FormGroup. */
    const group = new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.taskDuration),
      ]),
      end: new FormControl(initialize?.end, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.taskDuration),
      ]),
      type: new FormControl('VIDEO_ITEM_SEGMENT', [Validators.required]),
      mediaItem: mediaItemFormControl,
      segment_start: new FormControl(initialize?.range.start.value, [Validators.required]),
      segment_end: new FormControl(initialize?.range.end.value, [Validators.required]),
      segment_time_unit: new FormControl(
        initialize?.range.start.unit ? initialize?.range.start.unit : 'SECONDS',
        Validators.required
      ),
    });

    /* Initialize start, end and time unit based on target. */
    // fetch target time unit
    const targetTimeUnit = (this.form.get('target') as FormArray).controls[0].get('segment_time_unit').value;
    if (targetTimeUnit && this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_time_unit').setValue(targetTimeUnit);
    }

    if (!group.get('segment_start').value && this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_start').setValue((this.form.get('target') as FormArray).controls[0].get('segment_start').value);
    }

    if (!group.get('segment_end').value && this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_end').setValue((this.form.get('target') as FormArray).controls[0].get('segment_end').value);
    }

    /* Manually setting the duration of the hint equal to the duration of the task, this way the validators are happy */
    group.get('end').setValue(this.taskType.taskDuration);

    group
      .get('segment_start')
      .setValidators([Validators.required, this.temporalPointValidator(group.get('segment_time_unit') as FormControl)]);
    group
      .get('segment_end')
      .setValidators([Validators.required, this.temporalPointValidator(group.get('segment_time_unit') as FormControl)]);
    group.get('segment_start').updateValueAndValidity();
    group.get('segment_end').updateValueAndValidity();

    return group;
  }

  /**
   * Returns a new external image item component {@link FormGroup}.
   *
   * @param index The position of the new {@link FormGroup} (for data source).
   * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
   * @return The new {@link FormGroup}
   */
  private textItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent): FormGroup {
    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.taskDuration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.taskDuration)]),
      type: new FormControl('TEXT', [Validators.required]),
      description: new FormControl(initialize?.description, [Validators.required]),
    });
  }

  /**
   * Returns a new external image item component {@link FormGroup}.
   *
   * @param index The position of the new {@link FormGroup} (for data source).
   * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
   */
  private externalImageItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent): FormGroup {
    /* Prepare form control. */
    const pathFormControl = new FormControl(initialize?.path, [Validators.required]);

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.path`,
      pathFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) => this.collectionService.apiV2ExternalStartsWithGet(s))
      )
    );

    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.taskDuration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.taskDuration)]),
      type: new FormControl('EXTERNAL_IMAGE', [Validators.required]),
      path: pathFormControl,
    });
  }

  /**
   * Returns a new external video item component {@link FormGroup}.
   *
   * @param index The position of the new {@link FormGroup} (for data source).
   * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
   * @return The new {@link FormGroup}
   */
  private externalVideoItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent): FormGroup {
    /* Prepare form control. */
    const pathFormControl = new FormControl(initialize?.path, [Validators.required]);

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.path`,
      pathFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) => this.collectionService.apiV2ExternalStartsWithGet(s))
      )
    );

    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.taskDuration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.taskDuration)]),
      type: new FormControl('EXTERNAL_VIDEO', [Validators.required]),
      path: pathFormControl,
    });
  }
}
