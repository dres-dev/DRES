import {
    CollectionService,
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

export class CompetitionFormBuilder {

    /** List of data sources managed by this CompetitionFormBuilder. */
    private dataSources = new Map<string, Observable<RestMediaItem[]>>();

    /** The {@link FormGroup} held by this {@link CompetitionFormBuilder}. */
    public form: FormGroup;


    /**
     * Constructor for CompetitionFormBuilder.
     *
     * @param taskGroup
     * @param taskType
     * @param collectionService
     * @param data
     */
    constructor(private taskGroup: TaskGroup, private taskType: TaskType, private collectionService: CollectionService, private data?: RestTaskDescription) {
        this.initializeForm();
    }

    /**
     * Returns the {@link Observable<MediaItem[]>} for the given key.
     *
     * @param key Key to fetch the data source for.
     */
    public dataSource(key: string): Observable<RestMediaItem[]> {
        return this.dataSources.get(key);
    }

    /**
     * Adds a new {@link FormGroup} for the given {@link TaskType.ComponentsEnum}.
     *
     * @param type {@link TaskType.ComponentsEnum} to add a {@linl FormGroup} for.
     */
    public addComponentForm(type: TaskType.ComponentsEnum) {
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
                    mediaItem: c.get('mediaItem') ? c.get('mediaItem').value.id : null,
                    range: c.get('start') && c.get('end') ? {
                        start: { value: c.get('start').value, unit: c.get('time_unit').value }  as TemporalPoint,
                        end: { value: c.get('end').value, unit: c.get('time_unit').value }  as TemporalPoint,
                    } as TemporalRange : null,
                    description: c.get('description') ? c.get('description').value : null,
                    payload: c.get('payload') ? c.get('payload').value : null,
                    dataType: c.get('dataType') ? c.get('dataType').value : null
                } as RestTaskDescriptionComponent;
            }),
            target: {
                type: this.taskType.targetType,
                mediaItems: (this.form.get('target') as FormArray).controls.map(t => {
                    return {
                        mediaItem: t.get('mediaItem').value.id,
                        temporalRange: t.get('start') && t.get('end') ? {
                            start: {value: t.get('start').value, unit: t.get('time_unit').value} as TemporalPoint,
                            end: {value: t.get('end').value, unit: t.get('time_unit').value} as TemporalPoint
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
            mediaCollection: new FormControl(this.data?.mediaCollectionId, [Validators.required]),
            target: this.formForTarget(),
            components: this.formForQueryComponents()
        });
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
        switch (this.taskType.targetType) {
            case 'SINGLE_MEDIA_ITEM':
                return this.singleMediaItemTargetForm(0, this.data?.target?.mediaItems[0]);
            case 'MULTIPLE_MEDIA_ITEMS':
                return this.multipleMediaItemTargetForm(this.data?.target);
            case 'SINGLE_MEDIA_SEGMENT':
                return this.singleMediaSegmentTargetForm( this.data?.target?.mediaItems[0]);
            case 'JUDGEMENT':
                return new FormArray([]);
        }
    }

    /**
     * Returns FormGroup for a single Media Item Target.
     *
     * @param index Index of the FormControl
     * @param initialize The optional {RestTaskDescriptionTargetItem} containing the data to initialize the form with.
     */
    private singleMediaItemTargetForm(index: number, initialize?: RestTaskDescriptionTargetItem) {
        /* Prepare auto complete field. */
        const mediaItemFormControl =  new FormControl(null, Validators.required);
        this.dataSources.set('target.0.mediaItem', mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data?.mediaCollectionId) {
            this.collectionService.getApiCollectionWithCollectionidMediaWithMediaid(this.data?.mediaCollectionId, initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                    mediaItemFormControl.setValue(s);
                });
        }

        return new FormArray([new FormGroup({mediaItem: mediaItemFormControl})]);
    }

    /**
     * Returns FormGroup for a multiple Media Item Targets.
     *
     * @param initialize The optional {RestTaskDescriptionTarget} to initialize the form with.
     */
    private multipleMediaItemTargetForm(initialize?: RestTaskDescriptionTarget) {
        const content = [];
        if (initialize) {
            content.push(initialize?.mediaItems.map((d, i) => this.singleMediaItemTargetForm(i, d)));
        } else {
            content.push(this.singleMediaItemTargetForm(0));
        }
        return new FormArray(content);
    }

    /**
     * Returns FormGroup for a single Media Segment Target.
     *
     * @param initialize The optional {RestTaskDescriptionTargetItem} to initialize the form with.
     */
    private singleMediaSegmentTargetForm(initialize?: RestTaskDescriptionTargetItem) {
        /* Prepare auto complete field. */
        const mediaItemFormControl =  new FormControl(null, Validators.required);

        this.dataSources.set(`target.0.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data.mediaCollectionId) {
            this.collectionService.getApiCollectionWithCollectionidMediaWithMediaid(this.data.mediaCollectionId, initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                mediaItemFormControl.setValue(s);
            });
        }

        return new FormArray([new FormGroup({
            mediaItem: mediaItemFormControl,
            start: new FormControl(initialize?.temporalRange.start.value, [Validators.required, Validators.min(0)]),
            end: new FormControl(initialize?.temporalRange.end.value, [Validators.required, Validators.min(0)]),
            time_unit: new FormControl(initialize?.temporalRange.start.unit ?
                initialize?.temporalRange.start.unit  : 'SECONDS', Validators.required)
        })]);
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
                        break;
                    case 'EXTERNAL_VIDEO':
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
        const mediaItemFormControl =  new FormControl(null, Validators.required);
        if (!initialize.mediaItem && (this.taskType.targetType === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetType === 'SINGLE_MEDIA_ITEM')) {
            mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value);
        }

        /* Prepare data source. */
        this.dataSources.set(`components.${index}.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data?.mediaCollectionId) {
            this.collectionService.getApiCollectionWithCollectionidMediaWithMediaid(this.data?.mediaCollectionId, initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                mediaItemFormControl.setValue(s);
            });
        }

        return new FormGroup({
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
        const mediaItemFormControl =  new FormControl(null, Validators.required);
        if (!initialize?.mediaItem && (this.taskType.targetType === 'SINGLE_MEDIA_SEGMENT' || this.taskType.targetType === 'SINGLE_MEDIA_ITEM')) {
            mediaItemFormControl.setValue((this.form.get('target') as FormArray).controls[0].get('mediaItem').value);
        }

        /* Prepare data source. */
        this.dataSources.set(`components.${index}.mediaItem`, mediaItemFormControl.valueChanges.pipe(
            filter(s => s.length >= 1),
            switchMap(s => this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, s))
        ));

        /* Load media item from API. */
        if (initialize?.mediaItem && this.data?.mediaCollectionId) {
            this.collectionService.getApiCollectionWithCollectionidMediaWithMediaid(this.data?.mediaCollectionId, initialize.mediaItem)
                .pipe(first()).subscribe(s => {
                mediaItemFormControl.setValue(s);
            });
        }

        /* Prepare FormGroup. */
        const group = new FormGroup({
            type: new FormControl('VIDEO_ITEM_SEGMENT', [Validators.required]),
            mediaItem: mediaItemFormControl,
            start: new FormControl(initialize?.range.start.value, [Validators.required, Validators.min(0)]),
            end: new FormControl(initialize?.range.end.value, [Validators.required, Validators.min(0)]),
            time_unit: new FormControl(initialize?.range.start.unit ?
                initialize?.range.start.unit  : 'SECONDS', Validators.required)
        });

        /* Initialize start, end and time unit based on target. */
        if (!group.get('start').value && this.taskType.targetType === 'SINGLE_MEDIA_SEGMENT') {
            group.get('start').setValue((this.form.get('target') as FormArray).controls[0].get('start').value);
        }

        if (!group.get('end').value && this.taskType.targetType === 'SINGLE_MEDIA_SEGMENT') {
            group.get('end').setValue((this.form.get('target') as FormArray).controls[0].get('end').value);
        }

        if (!group.get('time_unit').value && this.taskType.targetType === 'SINGLE_MEDIA_SEGMENT') {
            group.get('time_unit').setValue((this.form.get('target') as FormArray).controls[0].get('time_unit').value);
        }

        return group;
    }

    /**
     * Returns a new external image item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param component The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private textItemComponentForm(index: number, component?: RestTaskDescriptionComponent) {
        return new FormGroup({
            type: new FormControl('TEXT', [Validators.required]),
            description: new FormControl(component?.description, [Validators.required])
        });
    }

    /**
     * Returns a new external image item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param component The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private externalImageItemComponentForm(index: number, component?: RestTaskDescriptionComponent) {
        return new FormGroup({
            type: new FormControl('EXTERNAL_IMAGE', [Validators.required]),
            payload: new FormControl(component?.payload, [Validators.required]),
            dataType: new FormControl(component?.dataType, [Validators.required]),
        });
    }

    /**
     * Returns a new external video item component {@link FormGroup}.
     *
     * @param index The position of the new {@link FormGroup} (for data source).
     * @param component The {@link RestTaskDescriptionComponent} to populate data from.
     */
    private externalVideoItemComponentForm(index: number, component?: RestTaskDescriptionComponent) {
        return new FormGroup({
            type: new FormControl('EXTERNAL_VIDEO', [Validators.required]),
            payload: new FormControl(component?.payload, [Validators.required]),
            dataType: new FormControl(component?.dataType, [Validators.required]),
            start: new FormControl(component?.range.start.value, [Validators.required, Validators.min(0)]),
            end: new FormControl(component?.range.end.value, [Validators.required, Validators.min(0)]),
            time_unit: new FormControl(component?.range.start.unit ?
                component?.range.start.unit  : 'SECONDS', Validators.required)
        });
    }
}

