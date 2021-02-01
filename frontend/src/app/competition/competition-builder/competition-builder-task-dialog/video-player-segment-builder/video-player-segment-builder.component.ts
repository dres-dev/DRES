import {AfterViewInit, Component, ElementRef, EventEmitter, Inject, Input, OnDestroy, Output, ViewChild} from '@angular/core';
import {Observable, of, Subscription} from 'rxjs';
import {RestMediaItem, TemporalRange} from '../../../../../../openapi';
import {AppConfig} from '../../../../app.config';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Options} from 'ng5-slider';

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


    @Input() data: VideoPlayerSegmentBuilderData;
    @Output() rangeChange = new EventEmitter<TemporalRange>();

    @ViewChild('videoPlayer', {static: false}) video: ElementRef;
    videoUrl: Observable<string>;
    playtimeRelative: Observable<number>;
    /* Apparently, some arbitrary default values are required */
    startInSeconds = 0;
    endInSeconds = 100;
    durationInSeconds = 100;


    options = {
        floor: 0,
        ceil: 100,
        showTicks: false
    } as Options;

    doLoop = true;

    private requestSub: Subscription;

    constructor(public config: AppConfig/*,
                public dialogRef: MatDialogRef<VideoPlayerSegmentBuilderData>,
                @Inject(MAT_DIALOG_DATA) public data: VideoPlayerSegmentBuilderData*/) {

    }

    ngAfterViewInit(): void {
        setTimeout(() => {
            /*
            * timeout because of value changed after checking thingy
            * https://blog.angular-university.io/angular-debugging/
            */
            if (this.data.mediaItem) {
                this.videoUrl = of(this.config.resolveApiUrl(`/media/${this.data?.mediaItem?.collectionId}/${this.data?.mediaItem?.id}`));
                this.durationInSeconds = this.data.mediaItem.durationMs / 1000;
                this.setNewRange(0, this.durationInSeconds);
            }
            if (this.data.segmentStart) {
                this.startInSeconds = this.data.segmentStart === -1 ? 0 : this.data.segmentStart;
            }
            if (this.data.segmentEnd) {
                this.endInSeconds = this.data.segmentEnd === -1 ? this.data.mediaItem.durationMs / 1000 : this.data.segmentEnd;
            }

            /* Custom loop handler */
            this.video.nativeElement.addEventListener('timeupdate', () => {
                const playtime = ((this.video.nativeElement.currentTime - this.startInSeconds) / (this.endInSeconds - this.startInSeconds)) * 100;
                this.playtimeRelative = new Observable<number>(subscriber => subscriber.next(playtime));
                if (this.endInSeconds) {
                    if (this.video.nativeElement.currentTime >= this.endInSeconds) {
                        if (this.doLoop) {
                            console.log('Rewind video');
                            this.video.nativeElement.currentTime = this.startInSeconds;
                            this.video.nativeElement.play().then(r => {
                            });
                        } else {
                            console.log('Finished. Stopping now');
                            this.video.nativeElement.currentTime = this.startInSeconds;
                            this.video.nativeElement.pause();
                        }
                    }
                }
            });

            /* custom handler to force-start when loaded. */
            this.video.nativeElement.addEventListener('loadeddata', () => {
                console.log('Event loadeddata fired.');
                this.video.nativeElement.currentTime = this.startInSeconds;
                this.video.nativeElement.play().then(r => console.log('Playing video after event fired'));
            });
        });
    }

    setNewRange(start: number, end: number) {
        const newOptions: Options = Object.assign({}, this.options);
        newOptions.ceil = start;
        newOptions.floor = end;
        this.options = newOptions;
    }

    stop() {
        this.videoUrl = undefined;
        this.startInSeconds = undefined;
        this.endInSeconds = undefined;
        this.video.nativeElement.pause();
    }

    ngOnDestroy(): void {
        this.stop();
        if (this.requestSub) {
            this.requestSub.unsubscribe();
        }
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
     * Currently only logs the formdata as json
     * @deprecated
     */
    export(): void {
        console.log(this.asJson());
    }

    /**
     * @deprecated
     */
    asJson(): string {
        return JSON.stringify(this.fetchData());
    }

    recalcVideoTime($event: Event) {
        console.log(`Change: ${this.startInSeconds} - ${this.endInSeconds}`);
        this.video.nativeElement.currentTime = this.startInSeconds;
        this.rangeChange.emit(this.fetchData());
    }

    setStart(){
        this.startInSeconds = this.video.nativeElement.currentTime;
        this.recalcVideoTime(null);
    }

    setEnd(){
        this.endInSeconds = this.video.nativeElement.currentTime;
        this.recalcVideoTime(null);
    }

    public fetchData() {
        const out = {
            start: {value: this.startInSeconds, unit: 'SECONDS'},
            end: {value: this.endInSeconds, unit: 'SECONDS'}
        } as TemporalRange;
        console.log(`Fetched: ${out}`);
        return out;
    }
}
