import {AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {AppConfig} from '../app.config';
import {JudgementRequest} from '../../../openapi';

@Component({
    selector: 'app-judgement-media-viewer',
    templateUrl: './judgement-media-viewer.component.html',
    styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent implements AfterViewInit, OnDestroy {

    /**
     * The observable holding the currently judged request (i.e. the submission to judge)
     */
    @Input() req: Observable<JudgementRequest>;
    /**
     * Padding to add, if the submission is too short
     * Will be added to the start and end, i.e. the actual played length will be
     * submission.duration + 2 * padding.
     * Default= 3s
     */
    @Input() padding = 3;
    /**
     * Too short submission duration threshold (if shorten than this, the padding is added).
     * Default: 3s
     */
    @Input() tooShortThreshold = 10;
    @ViewChild('videoPlayer', {static: false}) video: ElementRef;
    mediaUrl: Observable<string>;
    videoUrlDebug: Observable<string>;
    playtimeRelative: Observable<number>;
    activeType: BehaviorSubject<string> = new BehaviorSubject<string>('undefined');
    /** Current text to display. */
    currentText: Observable<string>;
    /** Font size in em. TODO: Make configurable. */
    fontSize = 2.5;
    private startInSeconds: number;
    private endInSeconds: number;
    private requestSub: Subscription;
    private offset = 5;
    private paddingEnabled: boolean;
    private startPaddingApplied: boolean;
    private relativePlaytimeSeconds = 0;
    private originalLengthInSeconds: number;

    constructor(public config: AppConfig) {
    }

    private static log(msg: string) {
        console.log(`[JudgeMedia] ${msg}`);
    }

    private static detectType(req: JudgementRequest) {
        if (req?.startTime) {
            return 'segment';
        } else {
            return req.mediaType.toLowerCase();
        }
    }

    ngAfterViewInit(): void {
        /* Custom loop handler */
        if (this.video) {
            this.video.nativeElement.addEventListener('timeupdate', () => {
                const playtime = ((this.video.nativeElement.currentTime - this.startInSeconds) / (this.endInSeconds - this.startInSeconds)) * 100;
                this.playtimeRelative = new Observable<number>(subscriber => subscriber.next(playtime));
                this.relativePlaytimeSeconds = Math.round(this.video.nativeElement.currentTime) - this.startInSeconds;
                JudgementMediaViewerComponent.log(`t=${this.relativePlaytimeSeconds}, ol=${this.originalLengthInSeconds}, ct=${this.video.nativeElement.currentTime}`);
                if (this.paddingEnabled && this.startPaddingApplied && this.video.nativeElement.currentTime < (this.startInSeconds + this.padding)) {
                    /* Start padding */
                    JudgementMediaViewerComponent.log('Start padding');
                    this.addTemporalContextClass();
                } else if (this.paddingEnabled && this.video.nativeElement.currentTime > this.startInSeconds + (this.startPaddingApplied ? this.padding : 0) + this.originalLengthInSeconds) {
                    /* End padding */
                    JudgementMediaViewerComponent.log('End padding');
                    this.addTemporalContextClass();
                } else {
                    /* no padding */
                    this.removeTemporalContextClass();
                }
                if (this.endInSeconds) {
                    if (this.video.nativeElement.currentTime >= this.endInSeconds) {
                        JudgementMediaViewerComponent.log('Rewind video');
                        this.relativePlaytimeSeconds = 0;
                        this.video.nativeElement.currentTime = this.startInSeconds;
                        this.video.nativeElement.play().then(r => {
                        });
                    }
                }
            });

            /* custom handler to force-start when loaded. */
            this.video.nativeElement.addEventListener('loadeddata', () => {
                JudgementMediaViewerComponent.log('Event loadeddata fired.');
                this.video.nativeElement.currentTime = this.startInSeconds;
                this.video.nativeElement.play().then(r => JudgementMediaViewerComponent.log('Playing video after event fired'));
            });
        }


        /* Handling request */
        this.requestSub = this.req.subscribe(req => {
            if (req != null) {
                JudgementMediaViewerComponent.log(`Request=${JSON.stringify(req)}`);
                this.activeType.next(JudgementMediaViewerComponent.detectType(req));
                switch (this.activeType.value) {
                    case 'text':
                        this.initText(req);
                        break;
                    case 'segment':
                        this.initSegment(req);
                        break;
                    case 'video':
                    case 'image':
                        this.initItem(req);
                        break;
                }
            }
        });
    }

    stop() {
        this.mediaUrl = undefined;
        this.activeType.next(undefined);
        this.videoUrlDebug = undefined;
        this.startInSeconds = undefined;
        this.endInSeconds = undefined;
        this.currentText = undefined;
        if (this.video) {
            this.video.nativeElement.pause();
        }
        this.removeTemporalContextClass();
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
        if (this.mediaUrl && this.video && this.video.nativeElement) {
            this.video.nativeElement.play();
        }
    }

    private initSegment(req: JudgementRequest) {
        this.calculateTime(req);
        const url = this.resolvePath(req);
        this.mediaUrl = new Observable<string>(sub => sub.next(url));
        this.videoUrlDebug = new Observable<string>(sub => sub.next(url)); // TODO Debug only
        JudgementMediaViewerComponent.log(`Handled request: src=${this?.video?.nativeElement?.src}, start=${this.startInSeconds}, end=${this.endInSeconds}`);
    }

    private initItem(req: JudgementRequest) {
        const url = this.resolvePath(req, false);
        this.mediaUrl = new Observable<string>(sub => sub.next(url));
        this.videoUrlDebug = new Observable<string>(sub => sub.next(url)); // TODO Debug only
    }

    private initText(req: JudgementRequest) {
        this.currentText = new Observable<string>(sub => sub.next(req.item));
    }

    private addTemporalContextClass() {
        if (!this.video?.nativeElement?.classList.contains('temporalContext')) {
            this.video.nativeElement.classList.add('temporalContext');
        }
    }

    private removeTemporalContextClass() {
        if (this.video?.nativeElement?.classList.contains('temporalContext')) {
            this.video.nativeElement.classList.remove('temporalContext');
        }
    }

    private calculateTime(req: JudgementRequest) {
        JudgementMediaViewerComponent.log('Calculating time');
        this.startInSeconds = 0;
        /* Parse start time, given in millis */
        if (req.startTime) {
            this.startInSeconds = Math.floor(Number.parseInt(req.startTime, 10) / 1000);
        }
        /* Parse end time, given in millis */
        if (req.endTime) {
            this.endInSeconds = Math.ceil(Number.parseInt(req.endTime, 10) / 1000);
        }
        this.originalLengthInSeconds = this.endInSeconds - this.startInSeconds;
        /* If only a frame is given OR too short is shown, add padding */
        if (this.originalLengthInSeconds < this.tooShortThreshold) {
            if (this.startInSeconds - this.padding < 0) {
                this.startInSeconds = 0;
            } else {
                this.startInSeconds = this.startInSeconds - this.padding;
                this.startPaddingApplied = true;
            }
            this.endInSeconds = this.endInSeconds + this.padding;
            this.paddingEnabled = true;
            JudgementMediaViewerComponent.log(`Padding: ${this.paddingEnabled}`);
        }
        JudgementMediaViewerComponent.log(`time=[${this.startInSeconds},${this.endInSeconds}] - original=${this.originalLengthInSeconds}`);
    }

    private resolvePath(req: JudgementRequest, time = true): string {
        const timeSuffix = time ? `#t=${this.startInSeconds},${this.endInSeconds}` : '';
        return this.config.resolveApiUrl(`/media/${req.collection}/${req.item}${timeSuffix}`);
    }

}
