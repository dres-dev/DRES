import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {
    AvsTaskDescription,
    CollectionService,
    KisTextualTaskDescription,
    KisVisualTaskDescription, MediaCollection,
    MediaItem,
    Task,
    TaskDescription,
    TemporalPoint,
    TemporalRange,
    VideoItem
} from '../../../../openapi';
import TaskTypeEnum = TaskDescription.TaskTypeEnum;
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {Observable} from 'rxjs';
import {filter, flatMap} from 'rxjs/operators';


export interface CompetitionBuilderTaskDialogData {
    taskType: TaskTypeEnum;
    task?: Task;
}

@Component({
    selector: 'app-competition-builder-add-task-dialog',
    templateUrl: './competition-builder-task-dialog.component.html'
})
export class CompetitionBuilderTaskDialogComponent {


    form: FormGroup;
    units = ['FRAME_NUMBER', 'SECONDS', 'MILLISECONDS', 'TIMECODE'];
    mediaCollectionSource: Observable<MediaCollection[]>;
    mediaItemSource: Observable<MediaItem[]>;

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTaskDialogComponent>,
                public collectionService: CollectionService,
                @Inject(MAT_DIALOG_DATA) private data: CompetitionBuilderTaskDialogData) {



        switch (data.taskType) {
            case 'KIS_VISUAL':
                this.form = CompetitionBuilderTaskDialogComponent.KisVisualFormControl(data.task);
                this.mediaCollectionSource = this.collectionService.getApiCollection();
                this.mediaItemSource = this.form.get('mediaItemId').valueChanges.pipe(
                    filter((value: string) => value.length >= 3),
                    flatMap(value => {
                        return this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, value);
                    })
                );
                break;
            case 'KIS_TEXTUAL':
                this.form = CompetitionBuilderTaskDialogComponent.KisTextualFormControl(data.task);
                this.mediaCollectionSource = this.collectionService.getApiCollection();
                this.mediaItemSource = this.form.get('mediaItemId').valueChanges.pipe(
                    filter((value: string) => value.length >= 3),
                    flatMap((value) => {
                        return this.collectionService.getApiCollectionWithCollectionidWithStartswith(this.form.get('mediaCollection').value, value);
                    })
                );
                break;
            case 'AVS':
                this.form = CompetitionBuilderTaskDialogComponent.AvsFormControl(data.task);
                break;
        }
    }

    public static BasicFormControl(task?: Task) {
        if (task) {
            return new FormGroup({
                name: new FormControl(task.name, Validators.required),
                taskGroup: new FormControl(task.taskGroup, Validators.required)
            });
        } else {
            return new FormGroup({
                name: new FormControl('', Validators.required),
                taskGroup: new FormControl('', Validators.required)
            });
        }
    }

    /**
     * Prepares and initializes the FormControl for an KIS Textual Task Description.
     *
     * @param task The task item (optional)
     */
    public static KisVisualFormControl(task?: Task) {
        const addTo = this.BasicFormControl(task);
        if (task) {
            const desc = task.description as KisVisualTaskDescription;
            addTo.addControl('mediaCollection', new FormControl(0));
            addTo.addControl('mediaItemId', new FormControl(desc.item, [Validators.required, Validators.min(1)]));
            addTo.addControl('start', new FormControl(desc.temporalRange.start.value, [Validators.required, Validators.min(0)]));
            addTo.addControl('end', new FormControl(desc.temporalRange.end.value, [Validators.required, Validators.min(0)]));
            addTo.addControl('time_unit', new FormControl(desc.temporalRange.start.unit, [Validators.required, Validators.min(0)]));
        } else {
            addTo.addControl('mediaCollection', new FormControl(''));
            addTo.addControl('mediaItemId', new FormControl('', [Validators.required, Validators.min(1)]));
            addTo.addControl('start', new FormControl(0, [Validators.required, Validators.min(0)]));
            addTo.addControl('end', new FormControl(0, [Validators.required, Validators.min(0)]));
            addTo.addControl('time_unit', new FormControl(TemporalPoint.UnitEnum.FRAMENUMBER, [Validators.required, Validators.min(0)]));
        }
        return addTo;
    }

    /**
     * Prepares and initializes the FormControl for an KIS Textual Task Description.
     *
     * @param addTo The form control to initialize.
     * @param task The task item (optional)
     */
    public static KisTextualFormControl(task?: Task) {
        const addTo = this.BasicFormControl(task);
        if (task) {
            const desc = task.description as KisTextualTaskDescription;
            addTo.addControl('mediaCollection', new FormControl(0));
            addTo.addControl('mediaItemId', new FormControl(desc.item, [Validators.required, Validators.min(1)]));
            addTo.addControl('start', new FormControl(desc.temporalRange.start.value, [Validators.required, Validators.min(0)]));
            addTo.addControl('end', new FormControl(desc.temporalRange.end.value, [Validators.required, Validators.min(0)]));
            addTo.addControl('time_unit', new FormControl(desc.temporalRange.start.unit, [Validators.required, Validators.min(0)]));
            addTo.addControl('descriptions', new FormArray(desc.descriptions.map(
                (v) => new FormControl(v, Validators.minLength(1)), Validators.required))
            );
            addTo.addControl('delay', new FormControl(desc.delay, [Validators.required, Validators.min(0)]));
        } else {
            addTo.addControl('mediaCollection', new FormControl(''));
            addTo.addControl('mediaItemId', new FormControl('', [Validators.required, Validators.min(1)]));
            addTo.addControl('start', new FormControl(0, [Validators.required, Validators.min(0)]));
            addTo.addControl('end', new FormControl(0, [Validators.required, Validators.min(0)]));
            addTo.addControl('time_unit', new FormControl(TemporalPoint.UnitEnum.FRAMENUMBER, [Validators.required, Validators.min(0)]));
            addTo.addControl('descriptions', new FormArray([new FormControl('', Validators.minLength(1))], Validators.required));
            addTo.addControl('delay', new FormControl('30', [Validators.required, Validators.min(0)]));
        }
        return addTo;
    }

    /**
     * Prepares and initializes the FormControl for an AVS Task Description.
     *
     * @param addTo The form control to initialize.
     * @param task The task item (optional)
     */
    public static AvsFormControl(task?: Task) {
        const addTo = this.BasicFormControl(task);
        if (task) {
            const desc = task.description as AvsTaskDescription;
            addTo.addControl('description', new FormControl(desc.description, Validators.minLength(1)));
        } else {
            addTo.addControl('description', new FormControl('', Validators.minLength(1)));
        }
        return addTo;
    }

    /**
     * Handler for + button for task descriptions (KIS_TEXTUAL tasks only). Adds a description.
     *
     * @param index The index to add description at.
     */
    public addDescription(index: number) {
        (this.form.get('descriptions') as FormArray).insert(index, CompetitionBuilderTaskDialogComponent.taskDescriptionFormControl());
    }

    /**
     * Handler for (-) button for task descriptions (KIS_TEXTUAL tasks only). Removes a description.
     *
     * @param index The index to remove description at.
     */
    public removeDescription(index: number) {
        (this.form.get('descriptions') as FormArray).removeAt(index);
    }

    /**
     * Converts a MediaItem to its display value for the autocomplete field.
     *
     * @param value MediaItem to convert
     */
    public mediaItemToDisplay(value: MediaItem) {
        if (value) {
            return `${value.name} (${value.id})`;
        } else {
           return '';
        }
    }

    /**
     * Handler for 'save' button.
     */
    public save() {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.get('name'). value,
                taskGroup: this.form.get('taskGroup').value,
                description: this.getTaskDescription()
            } as Task);
        }
    }

    /**
     * Handler for 'close' button.
     */
    public close(): void {
        this.dialogRef.close(null);
    }

    private getTaskDescription(): TaskDescription {
        switch (this.data.taskType) {
            case 'AVS':
                return {taskType: 'AVS', description: this.form.get('description').value} as AvsTaskDescription;
            case 'KIS_TEXTUAL':
                return {
                    taskType: 'KIS_TEXTUAL',
                    item: (this.form.get('mediaItemId').value) as VideoItem,
                    temporalRange: {
                        start: {
                            value: this.form.get('start').value,
                            unit: this.form.get('time_unit').value
                        } as TemporalPoint,
                        end: {
                            value: this.form.get('end').value,
                            unit: this.form.get('time_unit').value
                        } as TemporalPoint
                    } as TemporalRange,
                    descriptions: (this.form.get('descriptions') as FormArray).value,
                    delay: this.form.get('delay').value
                } as KisTextualTaskDescription;
            case 'KIS_VISUAL':
                return {
                    taskType: 'KIS_VISUAL',
                    item: (this.form.get('mediaItemId').value) as VideoItem,
                    temporalRange: {
                        start: {
                            value: this.form.get('start').value,
                            unit: this.form.get('time_unit').value
                        } as TemporalPoint,
                        end: {
                            value: this.form.get('end').value,
                            unit: this.form.get('time_unit').value
                        } as TemporalPoint
                    } as TemporalRange,
                } as KisVisualTaskDescription;
        }
    }
}
