import {RestTaskDescription, RestTaskDescriptionTarget, RestTaskDescriptionTargetItem, TaskType} from '../../../../../openapi';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';

export class CompetitionFormBuilder {

    constructor(private taskType: TaskType, private data?: RestTaskDescription) {
    }

    /**
     *
     * @param taskType
     * @param data
     */
    public formForData() {
        return new FormGroup({
            uid:  new FormControl(this.data?.id),
            name: new FormControl(this.data?.name, [Validators.required]),
            duration: new FormControl(this.durationInitValue, [Validators.required, Validators.min(1)]),
            components: new FormArray(this.data?.components ? this.data?.components?.map(v => new FormControl(v)) : [], [Validators.minLength(1)]),
            target: this.formForTarget()
        });
    }

    /**
     * Returns the duration value to init with:
     * either the set task duration (if this is for editing)
     * otherwise the default value based on the tasktype default
     */
    private get durationInitValue(){
        if(this?.data?.duration){
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
                return this.singleMediaItemTargetForm(this.data?.target?.mediaItems[0]);
            case 'SINGLE_MEDIA_SEGMENT':
                return this.singleMediaSegmentTargetForm(this.data?.target?.mediaItems[0]);
            case 'MULTIPLE_MEDIA_ITEMS':
                return this.multipleMediaSegmentTargetForm(this.data?.target);
            case 'JUDGEMENT':
                return new FormArray([]);
        }
    }

    /**
     * Returns FormGroup for a single Media Item Target.
     *
     * @param data The optional {RestTaskDescriptionTargetItem} containing the data.
     */
    private singleMediaItemTargetForm(data?: RestTaskDescriptionTargetItem) {
        return new FormArray([new FormGroup({
            mediaCollection: new FormControl(null),
            mediaItem: new FormControl(data?.mediaItem, Validators.required),
        })]);
    }

    /**
     * Returns FormGroup for a single Media Segment Target.
     *
     * @param data The optional {RestTaskDescriptionTargetItem} containing the data.
     */
    private singleMediaSegmentTargetForm(data?: RestTaskDescriptionTargetItem) {
        return new FormArray([new FormGroup({
            mediaCollection: new FormControl(null),
            mediaItem: new FormControl(data?.mediaItem, Validators.required),
            start: new FormControl(data?.temporalRange.start.value, Validators.required),
            end: new FormControl(data?.temporalRange.end.value, Validators.required),
            time_unit: new FormControl(data?.temporalRange.start.unit ? data?.temporalRange.start.unit  : 'FRAME_NUMBER', Validators.required)
        })]);
    }

    /**
     * Returns FormGroup for a multiple Media Segment Targets.
     *
     * @param data The optional {RestTaskDescriptionTarget} containing the data.
     */
    private multipleMediaSegmentTargetForm(data?: RestTaskDescriptionTarget) {
        const content = [];
        if (data != null) {
            content.push(data?.mediaItems.map(i => this.singleMediaSegmentTargetForm(i)));
        } else {
            content.push(this.singleMediaSegmentTargetForm());
        }
        return new FormArray(content);
    }
}
