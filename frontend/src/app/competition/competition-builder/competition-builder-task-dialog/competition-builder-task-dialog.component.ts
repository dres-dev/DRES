import {Component, ElementRef, Inject, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogConfig, MatDialogRef} from '@angular/material/dialog';
import {
    CollectionService,
    ConfiguredOptionQueryComponentType,
    ConfiguredOptionTargetType,
    RestMediaCollection,
    RestMediaItem,
    RestTaskDescription,
    TaskGroup,
    TaskType,
    TemporalPoint,
    TemporalRange
} from '../../../../../openapi';
import {AbstractControl, FormControl, FormGroup} from '@angular/forms';
import {Observable} from 'rxjs';
import {filter, first} from 'rxjs/operators';
import {AppConfig} from '../../../app.config';
import {CompetitionFormBuilder} from './competition-form.builder';
import {
    VideoPlayerSegmentBuilderComponent,
    VideoPlayerSegmentBuilderData
} from './video-player-segment-builder/video-player-segment-builder.component';
import {AdvancedBuilderDialogComponent, AdvancedBuilderDialogData} from './advanced-builder-dialog/advanced-builder-dialog.component';


/**
 * Its expected that the taskGroup and taskType properties are correctly given
 * even in the case this is 'edit'!
 */
export interface CompetitionBuilderTaskDialogData {
    taskGroup: TaskGroup;
    taskType: TaskType;
    task?: RestTaskDescription;
}

/**
 * https://onthecode.co.uk/force-selection-angular-material-autocomplete/
 * @param control
 * @constructor
 */
export function RequireMatch(control: AbstractControl) {
    const selection: any = control.value;
    if (typeof selection === 'string') {
        return {incorrect: true};
    }
    return null;
}


@Component({
    selector: 'app-competition-builder-task-dialog',
    templateUrl: './competition-builder-task-dialog.component.html'
})
export class CompetitionBuilderTaskDialogComponent {

    form: FormGroup;
    units = ['FRAME_NUMBER', 'SECONDS', 'MILLISECONDS', 'TIMECODE'];
    /** Data source for list of {@link MediaCollection}. Loaded upon construction of the dialog. */
    mediaCollectionSource: Observable<RestMediaCollection[]>;
    /** The {@link CompetitionFormBuilder} used by this dialogue. */
    builder: CompetitionFormBuilder;
    @ViewChild('videoPlayer', {static: false}) video: ElementRef;
    viewLayout = 'list';
    private imagePreviewMap = new Set<number>();

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTaskDialogComponent>,
                public collectionService: CollectionService,
                @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderTaskDialogData,
                private dialog: MatDialog,
                public config: AppConfig) {

        this.builder = new CompetitionFormBuilder(this.data.taskGroup, this.data.taskType, this.collectionService, this.data.task);
        this.form = this.builder.form;
        this.mediaCollectionSource = this.collectionService.getApiCollectionList();
    }

    private static randInt(min: number, max: number): number {
        min = Math.floor(min);
        max = Math.ceil(max);
        return Math.round(Math.random() * (max - min + 1) + min);
    }

    /**
     * Handler for (+) button for query target form component.
     */
    public addQueryTarget(targetType: ConfiguredOptionTargetType.OptionEnum) {
        this.builder.addTargetForm(targetType);
    }

    /**
     * Handler for (-) button for query target form component.
     *
     * @param index The index of the query target to remove.
     */
    public removeQueryTarget(index: number) {
        this.builder.removeTargetForm(index);
    }

    /**
     * Handler for (+) button for query hint form component.
     */
    public addQueryComponent(componentType: ConfiguredOptionQueryComponentType.OptionEnum) {
        this.builder.addComponentForm(componentType);
    }

    /**
     * Handler for (-) button for query hint form components.
     *
     * @param index The index of the query component to remove.
     */
    public removeQueryComponent(index: number) {
        this.builder.removeComponentForm(index);
    }

    /**
     * Converts a MediaItem to its display value for the autocomplete field.
     *
     * @param value MediaItem to convert
     */
    public mediaItemToDisplay(value: RestMediaItem) {
        if (value) {
            return `${value.name} (${value.type})`;
        } else {
            return '';
        }
    }

    /**
     * Handler for 'save' button.
     */
    public save() {
        if (this.form.valid) {
            this.dialogRef.close(this.builder.fetchFormData());
        }
    }

    /**
     * The form data as json
     */
    asJson(): string {
        return JSON.stringify(this.builder.fetchFormData());
    }

    /**
     * Prints the JSONified form data to console
     */
    export() {
        console.log(this.asJson());
    }

    /**
     * Picks a ranomd {@link MediaItem} from the list.
     *
     * @param collectionId The ID of the collection to pick a {@link MediaItem} from.
     * @param target The target {@link FormControl} to apply the value to.
     */
    public pickRandomMediaItem(collectionId: string, target: FormControl) {
        this.collectionService.getApiCollectionWithCollectionidRandom(collectionId).pipe(first()).subscribe(value => {
            target.setValue(value);
        });
    }

    /**
     * Picks a random segment within the given {@link MediaItem} .
     *
     * @param item The {@link VideoItem} to pick the segment for.
     * @param startControl The target {@link FormControl} to apply the value to.
     * @param endControl The target {@link FormControl} to apply the value to.
     * @param unitControl The target {@link FormControl} to apply the value to.
     */
    public pickRandomSegment(item: RestMediaItem, startControl: FormControl, endControl: FormControl, unitControl: FormControl) {
        const start = CompetitionBuilderTaskDialogComponent.randInt(1, (item.durationMs / 1000) / 2); // always in first half
        let end = 1;
        do {
            end = start + CompetitionBuilderTaskDialogComponent.randInt(5, (item.durationMs / 1000)); // Arbitrary 5 seconds minimal length
        } while (end > (item.durationMs / 1000));
        startControl.setValue(start);
        endControl.setValue(end);
        unitControl.setValue('SECONDS');
    }

    toggleVideoPlayer(mediaItem: RestMediaItem, startControl?: FormControl, endControl?: FormControl, unitControl?: FormControl) {
        /* Add to toggleVideoPlayer button if
        [disabled]="!target.get('mediaItem').value && !target.get('segment_start').value && !target.get('segment_end').value"
         */
        /*
        convert segmentStart / end based on unit to seconds
        pass everything to dialog. let dialog handle and take result as temporal range
         */
        let start = -1;
        let end = -1;
        if (startControl && startControl.value) {
            start = Number.parseInt(startControl.value, 10);
        }
        if (endControl && endControl.value) {
            end = Number.parseInt(endControl.value, 10);
        }
        const config = {
            width: '800px', data: {mediaItem, segmentStart: start, segmentEnd: end}
        } as MatDialogConfig<VideoPlayerSegmentBuilderData>;
        const dialogRef = this.dialog.open(VideoPlayerSegmentBuilderComponent, config);
        dialogRef.afterClosed().pipe(
            filter(r => r != null))
            .subscribe((r: TemporalRange) => {
                console.log(`Finished: ${r}`);
                startControl.setValue(r.start.value);
                endControl.setValue(r.end.value);
                unitControl.setValue(TemporalPoint.UnitEnum.SECONDS);
            });

    }

    isImageMediaItem(mi: RestMediaItem): boolean {
        if (mi) {
            return mi.type === 'IMAGE';
        } else {
            return false;
        }
    }

    /**
     * Check whether the given index is currently listed as active preview
     *
     * @param index
     */
    isPreviewActive(index: number): boolean {
        return this.imagePreviewMap.has(index);
    }

    togglePreview(index: number) {
        if (this.imagePreviewMap.has(index)) {
            this.imagePreviewMap.delete(index);
        } else {
            this.imagePreviewMap.add(index);
        }
    }

    getImageUrl(mi: RestMediaItem) {
        if (mi && mi.type === 'IMAGE') {
            return this.config.resolveApiUrl(`/media/${mi.collectionId}/${mi.id}`);
        }
        return '';
    }

    /**
     * Handler for 'close' button.
     */
    public close(): void {
        this.dialogRef.close(null);
    }

    batchAddTargets() {
        const config = {
            width: '400px', height: '600px', data: {builder: this.builder}
        } as MatDialogConfig<AdvancedBuilderDialogData>;
        const dialogRef = this.dialog.open(AdvancedBuilderDialogComponent, config);
        dialogRef.afterClosed().pipe(
            filter(r => r != null))
            .subscribe((r: Array<string>) => {
                this.builder.removeTargetForm(0);
                const mediaCollectionId = this.builder.form.get('mediaCollection').value;
                r.forEach((name, idx) => {
                    const form = this.builder.addTargetForm(ConfiguredOptionTargetType.OptionEnum.MULTIPLEMEDIAITEMS);
                    console.log(`${mediaCollectionId} ? ${name}`);
                    const nameNoExt = name.substring(0, name.lastIndexOf('.'));
                    this.collectionService.getApiCollectionWithCollectionidWithStartswith(mediaCollectionId, nameNoExt)
                        .subscribe(item =>
                        form.get('mediaItem').setValue(item[0]));
                });
            });
    }

    /**
     * Handler for 'close' button.
     */
    private pathForItem(item: RestMediaItem): string {
        // units = ['FRAME_NUMBER', 'SECONDS', 'MILLISECONDS', 'TIMECODE'];
        let timeSuffix = '';
        switch (this.form.get('time_unit').value) {
            case 'FRAME_NUMBER':
                const start = Math.round(this.form.get('start').value / item.fps);
                const end = Math.round(this.form.get('end').value / item.fps);
                timeSuffix = `#t=${start},${end}`;
                break;
            case 'SECONDS':
                timeSuffix = `#t=${this.form.get('start').value},${this.form.get('end').value}`;
                break;
            case 'MILLISECONDS':
                timeSuffix = `#t=${Math.round(this.form.get('start').value / 1000)},${Math.round(this.form.get('end').value / 1000)}`;
                break;
            case 'TIMECODE':
                console.log('Not yet supported'); // TODO make it!
                break;
            default:
                console.error(`The time unit ${this.form.get('time_unit').value} is not supported`);
        }
        return '';
    }
}
