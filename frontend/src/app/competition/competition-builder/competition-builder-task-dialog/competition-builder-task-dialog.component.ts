import {Component, ElementRef, Inject, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogConfig, MatDialogRef} from '@angular/material/dialog';
import {
    CollectionService,
    RestMediaCollection,
    RestMediaItem,
    RestTaskDescription,
    TaskGroup,
    TaskType,
    TemporalRange
} from '../../../../../openapi';
import {FormControl, FormGroup} from '@angular/forms';
import {Observable} from 'rxjs';
import {filter, first, flatMap, tap} from 'rxjs/operators';
import {AppConfig} from '../../../app.config';
import {CompetitionFormBuilder} from './competition-form.builder';
import {CollectionBuilderDialogComponent} from '../../../collection/collection-builder/collection-builder-dialog/collection-builder-dialog.component';
import {
    VideoPlayerSegmentBuilderComponent,
    VideoPlayerSegmentBuilderData
} from './video-player-segment-builder/video-player-segment-builder.component';


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
    templateUrl: './competition-builder-task-dialog.component.html'
})
export class CompetitionBuilderTaskDialogComponent {

    form: FormGroup;
    units = ['FRAME_NUMBER', 'SECONDS', 'MILLISECONDS', 'TIMECODE'];
    /** Data source for list of {@link MediaCollection}. Loaded upon construction of the dialog. */
    mediaCollectionSource: Observable<RestMediaCollection[]>;
    /** The {@link CompetitionFormBuilder} used by this dialogue. */
    builder: CompetitionFormBuilder;
    showPlayer = false;
    videoUrl: Observable<string>;
    @ViewChild('videoPlayer', {static: false}) video: ElementRef;

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
     *
     */
    public addQueryTarget(targetType: TaskType.TargetTypeEnum) {

    }

    /**
     * Handler for + button for query hint form component.
     */
    public addQueryComponent(componentType: TaskType.ComponentsEnum) {
        this.builder.addComponentForm(componentType);
    }

    /**
     * Handler for (-) button for query hint form components.
     *
     * @param index The index to remove the component at
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

    toggleVideoPlayer(mediaItem: RestMediaItem, segmentStart: string, segmentEnd: string) {
        /* Add to toggleVideoPlayer button if
        [disabled]="!target.get('mediaItem').value && !target.get('segment_start').value && !target.get('segment_end').value"
         */
        /*
        convert segmentStart / end based on unit to seconds
        pass everything to dialog. let dialog handle and take result as temporal range
         */
        const start = Number.parseInt(segmentStart, 10); // TODO sensitive conversion
        const end = Number.parseInt(segmentEnd, 10); // TODO sensitive conversion
        const config = {
            width: '500px', data: {mediaItem, segmentStart: start, segmentEnd: end}
        } as MatDialogConfig<VideoPlayerSegmentBuilderData>;
        const dialogRef = this.dialog.open(VideoPlayerSegmentBuilderComponent, config);
        dialogRef.afterClosed().pipe(
            filter(r => r != null),
            tap((r: TemporalRange) => {
                // TODO fill form accordingly. Howto know which one was called? (i.e. target or description)
            })
        );
    }

    /**
     * Handler for 'close' button.
     */
    public close(): void {
        this.dialogRef.close(null);
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
