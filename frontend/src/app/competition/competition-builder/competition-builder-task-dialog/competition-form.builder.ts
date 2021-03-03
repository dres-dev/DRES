import {
    CollectionService, ConfiguredOptionQueryComponentType, ConfiguredOptionTargetType,
    RestMediaItem,
    RestTaskDescription,
    RestTaskDescriptionComponent,
    RestTaskDescriptionTarget,
    RestTaskDescriptionTargetItem,
    TaskGroup,
    TaskType,
    TemporalPoint,
    TemporalRange
} from '../../../../../openapi';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {filter, first, switchMap} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {RequireMatch} from './competition-builder-task-dialog.component';

export class CompetitionFormBuilder {

    /** List of data sources managed by this CompetitionFormBuilder. */
    private dataSources = new Map<string, Observable<RestMediaItem[] | string[]>>();

    /** The {@link FormGroup} held by this {@link CompetitionFormBuilder}. */
    public form: FormGroup;


    /**
     * Constructor for CompetitionFormBuilder.
     *
     * @param taskGroup The {@link TaskGroup} to create this {@link CompetitionFormBuilder} for.
     * @param taskType The {@link TaskType} to create this {@link CompetitionFormBuilder} for.
     * @param collectionService The {@link CollectionService} reference used to fetch data through the DRES API.
     * @param data The {@link RestTaskDescription} to initialize the form with.
     */
    constructor(private taskGroup: TaskGroup, private taskType: TaskType,
                private collectionService: CollectionService, private data?: RestTaskDescription) {
        this.initializeForm();
    }

    /**
     * Returns the {@link Observable<MediaItem[]>} for the given key.
     *
     * @param key Key to fetch the data source for.
     */
    public dataSource(key: string): Observable<RestMediaItem[] | string[]> {
        return this.dataSources.get(key);
    }

    /**
     * Adds a new {@link FormGroup} for the given {@link ConfiguredOptionQueryComponentType.OptionEnum}.
     *
     * @param type The {@link ConfiguredOptionQueryComponentType.OptionEnum} to add a {@link FormGroup} for.
     */
    public addComponentForm(type: ConfiguredOptionQueryComponentType.OptionEnum) {
        const array = this.form.get('components') as FormArray;
        const newIndex = array.length;
        switch (type) {
            case 'IMAGE_ITEM':
                array.push(this.imageItemComponentForm(newIndex));
                break;
            case 'VIDEO_ITEM_SEGMENT':
                array.push(this.videoItemComponentForm(newIndex));
                break;
            case 'TEXT':
                array.push(this.textItemComponentForm(newIndex));
                break;
            case 'EXTERNAL_IMAGE':
                array.push(this.externalImageItemComponentForm(newIndex));
                break;
            case 'EXTERNAL_VIDEO':
                array.push(this.externalVideoItemComponentForm(newIndex));
                break;
        }
    }

    /**
     * Adds a new {@link FormGroup} for the given {@link TaskType.ComponentsEnum}.
     *
     * @param type The {@link TaskType.TargetTypeEnum} to add a {@link FormGroup} for.
     */
    public addTargetForm(type: ConfiguredOptionTargetType.OptionEnum) {
        const array = this.form.get('target') as FormArray;
        const newIndex = array.length;
        switch (type) {
            case 'MULTIPLE_MEDIA_ITEMS':
                const targetForm = this.singleMediaItemTargetForm(newIndex);
                array.push(targetForm);
                return targetForm;
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
            taskGroup: this.taskGroup.name, /* Cannot be edited! */
            taskType: this.taskGroup.type, /* Cannot be edited! */
            duration: this.form.get('duration').value,
            mediaCollectionId: this.form.get('mediaCollection').value,
            components: (this.form.get('components') as FormArray).controls.map(c => {
                return {
                    type: c.get('type').value,
                    start: c.get('start').value,
                    end: c.get('end').value,
                    mediaItem: c.get('mediaItem') ? c.get('mediaItem').value.id : null,
                    range: c.get('segment_start') && c.get('segment_end') ? {
                        start: { value: c.get('segment_start').value, unit: c.get('segment_time_unit').value }  as TemporalPoint,
                        end: { value: c.get('segment_end').value, unit: c.get('segment_time_unit').value }  as TemporalPoint,
                    } as TemporalRange : null,
                    description: c.get('description') ? c.get('description').value : null,
                    path: c.get('path') ? c.get('path').value : null
                } as RestTaskDescriptionComponent;
            }),
            target: {
                type: this.taskType.targetType.option,
                mediaItems: (this.form.get('target') as FormArray).controls.map(t => {
                    return {
                        mediaItem: t.get('mediaItem').value.id,
                        temporalRange: t.get('segment_start') && t.get('segment_start') ? {
                            start: {value: t.get('segment_start').value, unit: t.get('segment_time_unit').value} as TemporalPoint,
                            end: {value: t.get('segment_end').value, unit: t.get('segment_time_unit').value} as TemporalPoint
                        } as TemporalRange : null
                    } as RestTaskDescriptionTargetItem;
                })
            } as RestTaskDescriptionTarget
        } as RestTaskDescription;

        /* Set ID of set. */
        if (this.form.get('id').value) {
            data.id = this.form.get('id').value;
        }

        return data;
    }

    /**
     * Initializes the {@link FormGroup}.
     */
    private initializeForm() {
        this.form = new FormGroup({
            id: new FormControl(this.data?.id),
            name: new FormControl(this.data?.name, [Validators.required]),
            duration: new FormControl(this.durationInitValue, [Validators.required, Validators.min(1)]),
            mediaCollection: new FormControl(this.data?.mediaCollectionId, [Validators.required])
        });
        this.form.addControl('target', this.formForTarget());
        this.form.addControl('components', this.formForQueryComponents());
    }

    /**
     * Returns the duration value to init with:
     * either the set task duration (if this is for editing)
     * otherwise the default value based on the tasktype default
     */
    private get durationInitValue(){
        if (this?.data?.duration) {
            return this.data.duration;
        }else{
            return this.taskType.taskDuration;
        }
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
        this.dataSources.set(`target.${index}.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data?.mediaCollectionId) {
            this.collectionService.getApiMediaitemWithMediaid(initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                    mediaItemFormControl.setValue(s);
                });
        }

        return new FormGroup({mediaItem: mediaItemFormControl}, [RequireMatch]);
    }

    /**
     * Returns FormGroup for a single Media Segment Target.
     *
     * @param initialize The optional {RestTaskDescriptionTargetItem} to initialize the form with.
     */
    private singleMediaSegmentTargetForm(initialize?: RestTaskDescriptionTargetItem) {
        /* Prepare auto complete field. */
        const mediaItemFormControl =  new FormControl(null, [Validators.required, RequireMatch]);

        this.dataSources.set(`target.0.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data.mediaCollectionId) {
            this.collectionService.getApiMediaitemWithMediaid( initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                mediaItemFormControl.setValue(s);
            });
        }

        return new FormGroup({
            mediaItem: mediaItemFormControl,
            segment_start: new FormControl(initialize?.temporalRange.start.value, [Validators.required, Validators.min(0)]),
            segment_end: new FormControl(initialize?.temporalRange.end.value, [Validators.required, Validators.min(0)]),
            segment_time_unit: new FormControl(initialize?.temporalRange.start.unit ?
                initialize?.temporalRange.start.unit  : 'SECONDS', [Validators.required])
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
        return new FormArray(array);
    }

    /**
     * Returns a new image item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private imageItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent) {
        const mediaItemFormControl =  new FormControl(null, [Validators.required, RequireMatch]);
        if (!initialize?.mediaItem && (this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetType.option === 'SINGLE_MEDIA_ITEM')) {
            mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value);
        }

        /* Prepare data source. */
        this.dataSources.set(`components.${index}.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data?.mediaCollectionId) {
            this.collectionService.getApiMediaitemWithMediaid(initialize?.mediaItem)
                .pipe(first()).subscribe(s => {
                mediaItemFormControl.setValue(s);
            });
        }

        return new FormGroup({
            start: new FormControl(initialize?.start),
            end: new FormControl(initialize?.end),
            type: new FormControl('IMAGE_ITEM', [Validators.required]),
            mediaItem: mediaItemFormControl
        });
    }

    /**
     * Returns a new video item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private videoItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent) {
        /* Initialize media item based on target. */
        const mediaItemFormControl =  new FormControl(null, [Validators.required, RequireMatch]);
        if (!initialize?.mediaItem && (this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetType.option === 'SINGLE_MEDIA_ITEM')) {
            mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value);
        }

        /* Prepare data source. */
        this.dataSources.set(`components.${index}.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data?.mediaCollectionId) {
            this.collectionService.getApiMediaitemWithMediaid(initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                mediaItemFormControl.setValue(s);
            });
        }

        /* Prepare FormGroup. */
        const group = new FormGroup({
            start: new FormControl(initialize?.start),
            end: new FormControl(initialize?.end),
            type: new FormControl('VIDEO_ITEM_SEGMENT', [Validators.required]),
            mediaItem: mediaItemFormControl,
            segment_start: new FormControl(initialize?.range.start.value, [Validators.required, Validators.min(0)]),
            segment_end: new FormControl(initialize?.range.end.value, [Validators.required, Validators.min(0)]),
            segment_time_unit: new FormControl(initialize?.range.start.unit ?
                initialize?.range.start.unit  : 'SECONDS', Validators.required)
        });

        /* Initialize start, end and time unit based on target. */
        if (!group.get('segment_start').value && this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT') {
            group.get('segment_start').setValue((this.form.get('target') as FormArray).controls[0].get('segment_start').value);
        }

        if (!group.get('segment_end').value && this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT') {
            group.get('segment_end').setValue((this.form.get('target') as FormArray).controls[0].get('segment_end').value);
        }

        if (!group.get('segment_time_unit').value && this.taskType.targetType.option === 'SINGLE_MEDIA_SEGMENT') {
            group.get('segment_time_unit').setValue((this.form.get('target') as FormArray).controls[0].get('segment_time_unit').value);
        }

        return group;
    }

    /**
     * Returns a new external image item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private textItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent) {
        return new FormGroup({
            start: new FormControl(initialize?.start),
            end: new FormControl(initialize?.end),
            type: new FormControl('TEXT', [Validators.required]),
            description: new FormControl(initialize?.description, [Validators.required])
        });
    }

    /**
     * Returns a new external image item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private externalImageItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent) {
        /* Prepare form control. */
        const pathFormControl = new FormControl(initialize?.path, [Validators.required]);

        /* Prepare data source. */
        this.dataSources.set(`components.${index}.path`, pathFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiExternalWithStartswith(s))
        ));

        return new FormGroup({
            start: new FormControl(initialize?.start),
            end: new FormControl(initialize?.end),
            type: new FormControl('EXTERNAL_IMAGE', [Validators.required]),
            path: pathFormControl
        });
    }

    /**
     * Returns a new external video item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param initialize The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private externalVideoItemComponentForm(index: number, initialize?: RestTaskDescriptionComponent) {
        /* Prepare form control. */
        const pathFormControl = new FormControl(initialize?.path, [Validators.required]);

        /* Prepare data source. */
        this.dataSources.set(`components.${index}.path`, pathFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiExternalWithStartswith(s))
        ));

        return new FormGroup({
            start: new FormControl(initialize?.start),
            end: new FormControl(initialize?.end),
            type: new FormControl('EXTERNAL_VIDEO', [Validators.required]),
            path: pathFormControl
        });
    }
}


