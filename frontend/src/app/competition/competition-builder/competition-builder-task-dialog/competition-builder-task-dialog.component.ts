import {Component, ElementRef, Inject, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogConfig, MatDialogRef} from '@angular/material/dialog';
import {
    CollectionService,
    ConfiguredOptionQueryComponentOption,
    ConfiguredOptionTargetOption,
    RestMediaCollection,
    RestMediaItem,
    RestTaskDescription,
    RestTemporalPoint,
    RestTemporalRange,
    TaskGroup,
    TaskType
} from '../../../../../openapi';
import {FormControl, FormGroup} from '@angular/forms';
import {Observable} from 'rxjs';
import {filter, first} from 'rxjs/operators';
import {AppConfig} from '../../../app.config';
import {CompetitionFormBuilder} from './competition-form.builder';
import {VideoPlayerSegmentBuilderData} from './video-player-segment-builder/video-player-segment-builder.component';
import {AdvancedBuilderDialogComponent, AdvancedBuilderDialogData} from './advanced-builder-dialog/advanced-builder-dialog.component';
import {TimeUtilities} from '../../../utilities/time.utilities';


/**
 * Its expected that the taskGroup and taskType properties are correctly given
 * even in the case this is 'edit'!
 */
export interface CompetitionBuilderTaskDialogData {
    taskGroup: TaskGroup;
    taskType: TaskType;
    task?: RestTaskDescription;
}

@Component({
    selector: 'app-competition-builder-task-dialog',
    templateUrl: './competition-builder-task-dialog.component.html',
    styleUrls: ['./competition-builder-task-dialog.component.scss']
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
    showVideo = false;
    videoSegmentData: VideoPlayerSegmentBuilderData;
    private imagePreviewMap = new Set<number>();

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTaskDialogComponent>,
                public collectionService: CollectionService,
                @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderTaskDialogData,
                private dialog: MatDialog,
                public config: AppConfig) {

        this.builder = new CompetitionFormBuilder(this.data.taskGroup, this.data.taskType, this.collectionService, this.data.task);
        this.form = this.builder.form;
        this.mediaCollectionSource = this.collectionService.getApiV1CollectionList();
    }

    private static randInt(min: number, max: number): number {
        min = Math.floor(min);
        max = Math.ceil(max);
        return Math.round(Math.random() * (max - min + 1) + min);
    }

    uploaded = (taskData: string) => {
        const task = JSON.parse(taskData) as RestTaskDescription;
        this.builder = new CompetitionFormBuilder(this.data.taskGroup, this.data.taskType, this.collectionService, task);
        this.form = this.builder.form;
        console.log('Loaded task: ' + JSON.stringify(task));
    };

    /**
     * Handler for (+) button for query target form component.
     */
    public addQueryTarget(targetType: ConfiguredOptionTargetOption.OptionEnum) {
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
    public addQueryComponent(componentType: ConfiguredOptionQueryComponentOption.OptionEnum, previous: number = null) {
        this.builder.addComponentForm(componentType, previous);
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

    fileProvider = () => this.builder.fetchFormData()?.name ? this.builder.fetchFormData().name : 'task-download.json';

    downloadProvider = () => this.asJson();

    /**
     * Picks a ranomd {@link MediaItem} from the list.
     *
     * @param collectionId The ID of the collection to pick a {@link MediaItem} from.
     * @param target The target {@link FormControl} to apply the value to.
     */
    public pickRandomMediaItem(collectionId: string, target: FormControl) {
        this.collectionService.getApiV1CollectionWithCollectionidRandom(collectionId).pipe(first()).subscribe(value => {
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
        const unit = unitControl?.value ? (unitControl.value as RestTemporalPoint.UnitEnum) : RestTemporalPoint.UnitEnum.SECONDS;
        if (startControl && startControl.value) {
            if (unitControl.value === 'TIMECODE') {
                start = TimeUtilities.timeCode2Milliseconds(startControl.value, mediaItem.fps) / 1000;
            } else {
                start = TimeUtilities.point2Milliseconds({value: startControl.value, unit} as RestTemporalPoint, mediaItem.fps) / 1000;
            }
            // start = Number.parseInt(startControl.value, 10);
        }
        if (endControl && endControl.value) {
            if (unitControl.value === 'TIMECODE') {
                end = TimeUtilities.timeCode2Milliseconds(endControl.value, mediaItem.fps) / 1000;
            } else {
                end = TimeUtilities.point2Milliseconds({value: endControl.value, unit} as RestTemporalPoint, mediaItem.fps) / 1000;
            }
        }

        console.log('Start=' + start + ', End=' + end);
        // const config = {
        //     width: '800px', data: {mediaItem, segmentStart: start, segmentEnd: end}
        // } as MatDialogConfig<VideoPlayerSegmentBuilderData>;
        // const dialogRef = this.dialog.open(VideoPlayerSegmentBuilderDialogComponent, config);
        /*dialogRef.afterClosed().pipe(
            filter(r => r != null))
            .subscribe((r: TemporalRange) => {
                console.log(`Finished: ${r}`);
                startControl.setValue(r.start.value);
                endControl.setValue(r.end.value);
                unitControl.setValue(TemporalPoint.UnitEnum.SECONDS);
            });*/
        this.videoSegmentData = {mediaItem, segmentStart: start, segmentEnd: end} as VideoPlayerSegmentBuilderData;
        this.showVideo = !this.showVideo;
    }

    onRangeChange(range: RestTemporalRange, startControl?: FormControl, endControl?: FormControl, unitControl?: FormControl) {
        startControl?.setValue(range.start.value);
        endControl?.setValue(range.end.value);
        unitControl?.setValue(RestTemporalPoint.UnitEnum.SECONDS);
        console.log('Range updated');
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
                this.collectionService.postApiV1CollectionWithCollectionidResolve(mediaCollectionId, r).subscribe(items => {
                    items.forEach(item => {
                       const form = this.builder.addTargetForm(ConfiguredOptionTargetOption.OptionEnum.MULTIPLE_MEDIA_ITEMS);
                       console.log(`Adding new mediaItem as target ${mediaCollectionId}/${item.name}`);
                       form.get('mediaItem').setValue(item);
                    });
                });
                /*r.forEach((name, idx) => {
                    const form = this.builder.addTargetForm(ConfiguredOptionTargetOption.OptionEnum.MULTIPLE_MEDIA_ITEMS);
                    console.log(`${mediaCollectionId} ? ${name}`);
                    const nameNoExt = name.substring(0, name.lastIndexOf('.'));
                    this.collectionService.getApiV1CollectionWithCollectionidWithStartswith(mediaCollectionId, nameNoExt)
                        .subscribe(item => {
                                console.log(`Added ${item[0]}`);
                                form.get('mediaItem').setValue(item[0]);
                            }
                        );
                });*/
            });
    }

    timeUnitChanged($event, startElementRef: HTMLInputElement, endElementRef: HTMLInputElement) {
        console.log($event);
        const type = $event.value === 'TIMECODE' ? 'text' : 'number';
        console.log('New type: ' + type);
        if (startElementRef) {
            startElementRef.type = type;
        }
        if (endElementRef) {
            endElementRef.type = type;
        }
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

    public renderTextTargetTooltip() {
        return `The textual task target.
        Regex are allowed and have to be enclosed with single backslashes (\\).
        Java Regex matching is used.`;
    }
}
