import {AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
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
     * Default= 2s
     */
    @Input() padding = 2;
    /**
     * Too short submission duration threshold (if shorten than this, the padding is added).
     * Default: 3s
     */
    @Input() tooShortThreshold = 3;
    @ViewChild('videoPlayer', {static: false}) video: ElementRef;
    videoUrl: Observable<string>;
    videoUrlDebug: Observable<string>;
    private startInSeconds: number;
    private endInSeconds: number;
    private requestSub: Subscription;
    private offset = 5;

    constructor(private config: AppConfig) {
    }

    private static log(msg: string) {
        console.log(`[JudgeMedia] ${msg}`);
    }

    ngAfterViewInit(): void {
        /* Custom loop handler */
        this.video.nativeElement.addEventListener('timeupdate', () => {
            if (this.endInSeconds) {
                if (this.video.nativeElement.currentTime >= this.endInSeconds) {
                    JudgementMediaViewerComponent.log('Rewind video');
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

        /* Handling request */
        this.requestSub = this.req.subscribe(req => {
            if (req != null) {
                JudgementMediaViewerComponent.log(`Request=${JSON.stringify(req)}`);
                this.calculateTime(req);
                const url = this.resolvePath(req);
                this.videoUrl = new Observable<string>(sub => sub.next(url));
                this.videoUrlDebug = new Observable<string>(sub => sub.next(url)); // TODO Debug only
                JudgementMediaViewerComponent.log(`Handled request: src=${this.video.nativeElement.src}, start=${this.startInSeconds}, end=${this.endInSeconds}`);
            }
        });
    }

    stop() {
        this.videoUrl = undefined;
        this.videoUrlDebug = undefined;
        this.startInSeconds = undefined;
        this.endInSeconds = undefined;
        this.video.nativeElement.pause();
    }

    ngOnDestroy(): void {
        this.stop();
        this.requestSub.unsubscribe();
    }

    private calculateTime(req: JudgementRequest) {
        this.startInSeconds = 0;
        /* Parse start time, given in millis */
        if (req.startTime) {
            this.startInSeconds = Math.floor(Number.parseInt(req.startTime, 10) / 1000);
        }
        /* Parse end time, given in millis */
        if (req.endTime) {
            this.endInSeconds = Math.ceil(Number.parseInt(req.endTime, 10) / 1000);
        }
        /* If only a frame is given OR too short is shown, add padding */
        if (this.endInSeconds - this.startInSeconds < this.tooShortThreshold) {
            this.startInSeconds = this.startInSeconds - this.padding < 0 ? 0 : this.startInSeconds - this.padding;
            this.endInSeconds = this.endInSeconds + this.padding;
        }
        JudgementMediaViewerComponent.log(`time=[${this.startInSeconds},${this.endInSeconds}`);
    }

    private resolvePath(req: JudgementRequest, time = true): string {
        const timeSuffix = time ? `#t=${this.startInSeconds},${this.endInSeconds}` : '';
        return this.config.resolveApiUrl(`/media/${req.collection}/${req.item}${timeSuffix}`);
    }

}
