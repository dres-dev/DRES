import {AfterViewInit, Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {RestMediaItem, TemporalRange} from '../../../../../../openapi';
import {AppConfig} from '../../../../app.config';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

export interface VideoPlayerSegmentBuilderData {
    mediaItem: RestMediaItem;
    segmentStart: number;
    segmentEnd: number;
}

@Component({
    selector: 'app-video-player-segment-builder',
    templateUrl: './video-player-segment-builder.component.html',
    styleUrls: ['./video-player-segment-builder.component.scss']
})
export class VideoPlayerSegmentBuilderComponent implements AfterViewInit, OnDestroy {


    @ViewChild('videoPlayer', {static: false}) video: ElementRef;
    videoUrl: Observable<string>;
    playtimeRelative: Observable<number>;
    private start: number;
    private end: number;
    private startInSeconds: number;
    private endInSeconds: number;
    private requestSub: Subscription;

    constructor(public config: AppConfig, public dialogRef: MatDialogRef<VideoPlayerSegmentBuilderData>,
                @Inject(MAT_DIALOG_DATA) public data: VideoPlayerSegmentBuilderData) {
        // TODO setup numbers
    }

    ngAfterViewInit(): void {
        /* Custom loop handler */
        this.video.nativeElement.addEventListener('timeupdate', () => {
            const playtime = ((this.video.nativeElement.currentTime - this.startInSeconds) / (this.endInSeconds - this.startInSeconds)) * 100;
            this.playtimeRelative = new Observable<number>(subscriber => subscriber.next(playtime));
            if (this.endInSeconds) {
                if (this.video.nativeElement.currentTime >= this.endInSeconds) {
                    console.log('Rewind video');
                    this.video.nativeElement.currentTime = this.startInSeconds;
                    this.video.nativeElement.play().then(r => {
                    });
                }
            }
        });

        /* custom handler to force-start when loaded. */
        this.video.nativeElement.addEventListener('loadeddata', () => {
            console.log('Event loadeddata fired.');
            this.video.nativeElement.currentTime = this.startInSeconds;
            this.video.nativeElement.play().then(r => console.log('Playing video after event fired'));
        });
    }

    stop() {
        this.videoUrl = undefined;
        this.startInSeconds = undefined;
        this.endInSeconds = undefined;
        this.video.nativeElement.pause();
    }

    ngOnDestroy(): void {
        this.stop();
        this.requestSub.unsubscribe();
    }

    togglePlaying() {
        if (this.video && this.video.nativeElement) {
            if (this.video.nativeElement.paused) {
                this.video.nativeElement.play();
            } else {
                this.video.nativeElement.pause();
            }
        }
    }

    play() {
        if (this.videoUrl && this.video && this.video.nativeElement) {
            this.video.nativeElement.play();
        }
    }

    /**
     * Fetches the data from the form, returns it to the dialog openeer and cloeses this dialog
     */
    save(): void {
        this.dialogRef.close(this.fetchData());
    }

    /**
     * Closes this dialog without saving
     */
    close(): void {
        this.dialogRef.close(null);
    }

    /**
     * Currently only logs the formdata as json
     */
    export(): void {
        console.log(this.asJson());
    }

    asJson(): string {
        return JSON.stringify(this.fetchData());
    }

    private fetchData() {
        return {
            start: {value: this.start, unit: 'SECONDS'},
            end: {value: this.end, unit: 'SECONDS'}
        } as TemporalRange;
    }


}
