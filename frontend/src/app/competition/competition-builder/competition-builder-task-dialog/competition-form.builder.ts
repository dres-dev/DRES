import { AbstractControl, FormArray, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { filter, first, switchMap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { RequireMatch } from './require-match';
import { TimeUtilities } from '../../../utilities/time.utilities';
import {
  ApiHint,
  ApiHintOption, ApiHintType,
  ApiMediaItem, ApiTarget,
  ApiTargetOption, ApiTargetType,
  ApiTaskGroup,
  ApiTaskTemplate,
  ApiTaskType, ApiTemporalPoint, ApiTemporalRange,
  CollectionService
} from '../../../../../openapi';

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
   * @param data The {@link ApiTaskTemplate} to initialize the form with.
   */
  constructor(
    private taskGroup: ApiTaskGroup,
    private taskType: ApiTaskType,
    private collectionService: CollectionService,
    private data?: ApiTaskTemplate
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
      return this.taskType.duration;
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
  public addComponentForm(type: ApiHintType, afterIndex: number = null, external : boolean = false) {
    const array = this.form.get('components') as FormArray;
    const newIndex = afterIndex ? afterIndex + 1 : array.length;
    let component = null;
    switch (type) {
      case 'EMPTY':
        break;
      case 'TEXT':
        component = this.textItemComponentForm(newIndex);
        break;
      case 'VIDEO':
        component = external ?  this.externalVideoItemComponentForm(newIndex) : this.videoItemComponentForm(newIndex);
        break;
      case 'IMAGE':
        component = external ? this.externalImageItemComponentForm(newIndex) : this.imageItemComponentForm(newIndex);
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
      component.get('start').setValue(0, {emitEvent: false});
    } else if (previousItem.get('end').value) {
      component.get('start').setValue(previousItem.get('end').value, {emitEvent: false});
    } else {
      previousItem.get('end').setValue(previousItem.get('start').value + CompetitionFormBuilder.DEFAULT_HINT_DURATION, {emitEvent: false});
      component.get('start').setValue(previousItem.get('end').value, {emitEvent: false});
    }

    /* Append component. */
    array.insert(newIndex, component);
  }

  /**
   * Adds a new {@link FormGroup} for the given {@link TaskType.ComponentsEnum}.
   *
   * @param type The {@link TaskType.TargetTypeEnum} to add a {@link FormGroup} for.
   */
  public addTargetForm(type: ApiTargetOption | 'MULTI') {
    const array = this.form.get('target') as FormArray;
    const newIndex = array.length;
    switch (type) {
      // FIXME to make compiler happy. obviously this is semantically not appropriate
      case 'MULTI':
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
   * Assembles form data and returns a {@link ApiTaskTemplate}.
   */
  public fetchFormData(): ApiTaskTemplate {
    // FIXME semantic check for the entire fetching
    const data = {
      name: this.form.get('name').value,
      taskGroup: this.taskGroup.name /* Cannot be edited! */,
      taskType: this.taskGroup.type /* Cannot be edited! */,
      duration: this.form.get('duration').value,
      collectionId: this.form.get('mediaCollection').value,
      hints: (this.form.get('components') as FormArray).controls.map((c) => {
        return {
          type: c.get('type').value,
          start: c.get('start').value,
          end: c.get('end').value,
          mediaItem: c.get('mediaItem')?.value?.mediaItemId ?? null,
          range:
            c.get('segment_start') && c.get('segment_end')
              ? ({
                  start: { value: c.get('segment_start').value, unit: c.get('segment_time_unit').value } as ApiTemporalPoint,
                  end: { value: c.get('segment_end').value, unit: c.get('segment_time_unit').value } as ApiTemporalPoint,
                } as ApiTemporalRange)
              : null,
          description: c.get('description') ? c.get('description').value : null,
          path: c.get('path') ? c.get('path').value : null,
        } as ApiHint;
      }),
      targets:  (this.form.get('target') as FormArray).controls.map((t) => {
          return {
            type: t.get('type').value,
            target: t.get('mediaItem')?.value?.mediaItemId ?? null,
            range:
              t.get('segment_start') && t.get('segment_start')
                ? ({
                    start: { value: t.get('segment_start').value, unit: t.get('segment_time_unit').value } as ApiTemporalPoint,
                    end: { value: t.get('segment_end').value, unit: t.get('segment_time_unit').value } as ApiTemporalPoint,
                  } as ApiTemporalRange)
                : null,
          } as ApiTarget;
        })as Array<ApiTarget>,
    } as ApiTaskTemplate;

    /* Set ID of set. */
    data.id = this.form.get('id')?.value ?? null;
    console.log("FETCH", data);
    return data;
  }

  public storeFormData(){
      this.data.name = this.form.get('name').value;
      this.data.taskGroup = this.taskGroup.name /* Cannot be edited! */;
      this.data.taskType = this.taskGroup.type /* Cannot be edited! */;
      this.data.duration= this.form.get('duration').value;
      this.data.collectionId = this.form.get('mediaCollection').value;
      this.data.hints = (this.form.get('components') as FormArray).controls.map((c) => {
        return {
          type: c.get('type').value,
          start: c.get('start').value,
          end: c.get('end').value,
          mediaItem: c.get('mediaItem')?.value?.mediaItemId ?? null,
          range:
            c.get('segment_start') && c.get('segment_end')
              ? ({
                start: { value: c.get('segment_start').value, unit: c.get('segment_time_unit').value } as ApiTemporalPoint,
                end: { value: c.get('segment_end').value, unit: c.get('segment_time_unit').value } as ApiTemporalPoint,
              } as ApiTemporalRange)
              : null,
          description: c.get('description') ? c.get('description').value : null,
          path: c.get('path') ? c.get('path').value : null,
        } as ApiHint;
      });
      this.data.targets=  (this.form.get('target') as FormArray).controls.map((t) => {
        return {
          type: t.get('type').value,
          /** Either its the mediaItem's ID or its text that is stored in 'mediaItem' form control */
          target: t.get('mediaItem')?.value?.mediaItemId ?? t.get('mediaItem')?.value,
          range:
            t.get('segment_start') && t.get('segment_start')
              ? ({
                start: { value: t.get('segment_start').value, unit: t.get('segment_time_unit').value } as ApiTemporalPoint,
                end: { value: t.get('segment_end').value, unit: t.get('segment_time_unit').value } as ApiTemporalPoint,
              } as ApiTemporalRange)
              : null,
        } as ApiTarget;
      })as Array<ApiTarget>;

    /* Reset ID if set. */
    this.data.id = this.form.get('id')?.value ?? null;
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
      mediaCollection: new FormControl(this.data?.collectionId, [Validators.required]),
    });
    this.form.addControl('target', this.formForTarget());
    this.form.addControl('components', this.formForQueryComponents());
  }

  /**
   * Returns the target form for the given {TaskType}
   */
  private formForTarget() {
    switch (this.taskType.targetOption) {
      case 'SINGLE_MEDIA_ITEM':
        return new FormArray([this.singleMediaItemTargetForm(0, this.data?.targets[0] ?? null)]);
      case 'SINGLE_MEDIA_SEGMENT':
        return new FormArray([this.singleMediaSegmentTargetForm(this.data?.targets[0] ?? null)]);
      case 'JUDGEMENT':
        return new FormArray([new FormGroup({type: new FormControl(ApiTargetType.JUDGEMENT)})]);
      case 'VOTE':
        return new FormArray([new FormGroup({type: new FormControl(ApiTargetType.JUDGEMENT_WITH_VOTE)})]);
      case 'TEXT':
        const text: FormGroup[] = [];
        if (this.data?.targets) {
          this.data?.targets?.forEach((d, i) => text.push(this.singleTextTargetForm(d)));
        } else {
          console.log('no target');
          text.push(this.singleTextTargetForm());
        }
        return new FormArray(text);
      default:
        // Handling multiple here, since it's the default.
        const content: FormGroup[] = [];
        if (this.data?.targets) {
          this.data?.targets?.forEach((t, i) => content.push(this.singleMediaItemTargetForm(i, t)));
        } else {
          content.push(this.singleMediaItemTargetForm(0));
        }
        return new FormArray(content);
    }
  }

  /**
   * Returns FormGroup for a single Media Item Target.
   *
   * @param index Index of the FormControl
   * @param initialize The optional {RestTaskDescriptionTargetItem} containing the data to initialize the form with.
   */
  private singleMediaItemTargetForm(index: number, initialize?: ApiTarget): FormGroup {
    /* Prepare auto complete field. */
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    const typeFormControl = new FormControl(ApiTargetType.MEDIA_ITEM);

    this.dataSources.set(
      `target.${index}.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.getApiV2CollectionByCollectionIdByStartsWith(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.target && this.data?.collectionId) {
      this.collectionService
        .getApiV2MediaItemByMediaItemId(initialize?.target)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s, {emitEvent: false});
        });
    }

    return new FormGroup({ type: typeFormControl, mediaItem: mediaItemFormControl }, [RequireMatch]);
  }

  /**
   * Returns FormGroup for a single Media Segment Target.
   *
   * @param initialize The optional {RestTaskDescriptionTargetItem} to initialize the form with.
   */
  private singleMediaSegmentTargetForm(initialize?: ApiTarget) {
    /* Prepare auto complete field. */
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    const typeFormControl = new FormControl(ApiTargetType.MEDIA_ITEM_TEMPORAL_RANGE);

    this.dataSources.set(
      `target.0.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.getApiV2CollectionByCollectionIdByStartsWith(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.target && this.data.collectionId) {
      this.collectionService
        .getApiV2MediaItemByMediaItemId(initialize.target)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s, {emitEvent: false});
        });
    }

    const formGroup = new FormGroup({
      type: typeFormControl,
      mediaItem: mediaItemFormControl,
      segment_start: new FormControl(initialize?.range.start.value, [Validators.required]),
      segment_end: new FormControl(initialize?.range.end.value, [Validators.required]),
      segment_time_unit: new FormControl(
        initialize?.range.start.unit ? initialize?.range.start.unit : 'SECONDS',
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

  private singleTextTargetForm(initialize?: ApiTarget) {
    const textFormControl = new FormControl(null, [Validators.required]);
    const typeFormControl = new FormControl(ApiTargetType.TEXT);

    console.log(initialize?.target);

    textFormControl.setValue(initialize?.target, {emitEvent: false});

    return new FormGroup({
      type: typeFormControl,
      mediaItem: textFormControl,
    });
  }

  /**
   * Returns the component form for the given {TaskType}
   */
  private formForQueryComponents() {
    const array = [];
    if (this.data) {
      for (const component of this.data.hints) {
        const index = this.data.hints.indexOf(component);
        switch (component.type) {
          case 'IMAGE':
            array.push(this.imageItemComponentForm(index, component));
            break;
          case 'VIDEO':
            array.push(this.videoItemComponentForm(index, component));
            break;
          case 'TEXT':
            array.push(this.textItemComponentForm(index, component));
            break;
          /*case 'EXTERNAL_IMAGE':
            array.push(this.externalImageItemComponentForm(index, component));
            break;
          case 'EXTERNAL_VIDEO':
            array.push(this.externalVideoItemComponentForm(index, component));
            break;*/
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
  private imageItemComponentForm(index: number, initialize?: ApiHint): FormGroup {
    if(initialize?.path && !initialize?.mediaItem){
      /* Handle external image */
      return this.externalImageItemComponentForm(index, initialize);
    }
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    if (
      !initialize?.mediaItem &&
      (this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetOption === 'SINGLE_MEDIA_ITEM')
    ) {
      mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value, {emitEvent: false});
    }

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.getApiV2CollectionByCollectionIdByStartsWith(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.mediaItem && this.data?.collectionId) {
      this.collectionService
        .getApiV2MediaItemByMediaItemId(initialize?.mediaItem)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s, {emitEvent: false});
        });
    }



    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.duration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.duration)]),
      type: new FormControl('IMAGE', [Validators.required]),
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
  private videoItemComponentForm(index: number, initialize?: ApiHint): FormGroup {
    if(initialize?.path && !initialize?.mediaItem){
      /* handle external video */
      return this.externalVideoItemComponentForm(index, initialize);
    }
    /* Initialize media item based on target. */
    const mediaItemFormControl = new FormControl(null, [Validators.required, RequireMatch]);
    if (
      !initialize?.mediaItem &&
      (this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetOption === 'SINGLE_MEDIA_ITEM')
    ) {
      mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value, {emitEvent: false});
    }

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.mediaItem`,
      mediaItemFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) =>
          this.collectionService.getApiV2CollectionByCollectionIdByStartsWith(this.form.get('mediaCollection').value, s)
        )
      )
    );

    /* Load media item from API. */
    if (initialize?.mediaItem && this.data?.collectionId) {
      this.collectionService
        .getApiV2MediaItemByMediaItemId(initialize.mediaItem)
        .pipe(first())
        .subscribe((s) => {
          mediaItemFormControl.setValue(s, {emitEvent: false});
        });
    }

    /* Prepare FormGroup. */
    const group = new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.duration),
      ]),
      end: new FormControl(initialize?.end, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.duration),
      ]),
      type: new FormControl('VIDEO', [Validators.required]),
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
    if (targetTimeUnit && this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_time_unit').setValue(targetTimeUnit, {emitEvent: false});
    }

    if (!group.get('segment_start').value && this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_start').setValue((this.form.get('target') as FormArray).controls[0].get('segment_start').value, {emitEvent: false});
    }

    if (!group.get('segment_end').value && this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_end').setValue((this.form.get('target') as FormArray).controls[0].get('segment_end').value, {emitEvent: false});
    }

    /* Manually setting the duration of the hint equal to the duration of the task, this way the validators are happy */
    group.get('end').setValue(this.taskType.duration, {emitEvent: false});

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
  private textItemComponentForm(index: number, initialize?: ApiHint): FormGroup {
    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.duration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.duration)]),
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
  private externalImageItemComponentForm(index: number, initialize?: ApiHint): FormGroup {
    /* Prepare form control. */
    const pathFormControl = new FormControl(initialize?.path, [Validators.required]);

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.path`,
      pathFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) => this.collectionService.getApiV2ExternalByStartsWith(s))
      )
    );

    return new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.duration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.duration)]),
      type: new FormControl('IMAGE', [Validators.required]),
      external: new FormControl(true),
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
  private externalVideoItemComponentForm(index: number, initialize?: ApiHint): FormGroup {
    /* Prepare form control. */
    const pathFormControl = new FormControl(initialize?.path, [Validators.required]);

    /* Prepare data source. */
    this.dataSources.set(
      `components.${index}.path`,
      pathFormControl.valueChanges.pipe(
        filter((s) => s.length >= 1),
        switchMap((s) => this.collectionService.getApiV2ExternalByStartsWith(s))
      )
    );

    const group = new FormGroup({
      start: new FormControl(initialize?.start, [
        Validators.required,
        Validators.min(0),
        Validators.max(this.taskType.duration),
      ]),
      end: new FormControl(initialize?.end, [Validators.min(0), Validators.max(this.taskType.duration)]),
      type: new FormControl('VIDEO', [Validators.required]),
      external: new FormControl(true),
      path: pathFormControl,
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
    if (targetTimeUnit && this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_time_unit').setValue(targetTimeUnit, {emitEvent: false});
    }

    if (!group.get('segment_start').value && this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_start').setValue((this.form.get('target') as FormArray).controls[0].get('segment_start').value, {emitEvent: false});
    }

    if (!group.get('segment_end').value && this.taskType.targetOption === 'SINGLE_MEDIA_SEGMENT') {
      group.get('segment_end').setValue((this.form.get('target') as FormArray).controls[0].get('segment_end').value, {emitEvent: false});
    }

    /* Manually setting the duration of the hint equal to the duration of the task, this way the validators are happy */
    group.get('end').setValue(this.taskType.duration, {emitEvent: false});

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
}
