import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {AppConfig} from '../app.config';
import {JudgementRequest} from '../../../openapi';

@Component({
    selector: 'app-judgement-media-viewer',
    templateUrl: './judgement-media-viewer.component.html',
    styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent implements OnInit, OnDestroy{

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

    private startInSeconds: Observable<number>;
    private endInSeconds: Observable<number>;
    private requestSub: Subscription;

    videoUrl: Observable<string>;
    videoUrlDebug: Observable<string>;
    private offset = 5;

    constructor(private config: AppConfig) {
    }

    ngOnInit(): void {
        this.requestSub = this.req.subscribe(req => {

        });
    }


    public judge(req: JudgementRequest) {
        console.log('[JudgeMedia] Judging: ' + JSON.stringify(req));
        let startTime = 0;
        if (req.startTime) {
            startTime = Math.floor(Number.parseInt(req.startTime, 10) / 1000); // ms?
        }
        let endTime = -1;
        if (req.endTime) {
            endTime = Math.ceil(Number.parseInt(req.endTime, 10) / 1000);
        }
        if (startTime === endTime) {
            startTime = startTime - this.offset < 0 ? 0 : startTime - this.offset;
            endTime = endTime + this.offset;
        }
        // TODO How to know here what type this media item has?
        const path1 = `/media/${req.collection}/${req.item}#t=${startTime},${endTime}`; // Should work (in chorme, directly this works)
        const path = `/media/${req.collection}/${req.item}`; // Should work (in chorme, directly this works)
        const url = this.config.resolveApiUrl(path);
        const debugUrl = this.config.resolveApiUrl(path1);
        /* Debug */
        this.videoUrlDebug = new Observable<string>(sub => {
            sub.next(debugUrl);
        });
        console.log('[JudgeMedia] Url=' + url);
        this.videoUrl = new Observable<string>(subscriber => {
            subscriber.next(debugUrl);
            if (this.video) {
                // This code is not called, as videoPlayer does not exist -- yet
                // this.video.src = url;
                console.log('[JudgeMedia] Adding loop-hook');
                this.video.nativeElement.currentTime = startTime;
                // this.video.loop = true;
                if (endTime > 0) {
                    this.video.nativeElement.addEventListener('timeupdate', () => { // TODO TypeError this.video.addEventListener is not a function
                        console.log(`[JudgeMedia] Playing@${this.video.nativeElement.currentTime}s`);
                        if (this.video.nativeElement.currentTime >= endTime) {
                            console.log('[JudgeMedia] Restarting video');
                            this.video.nativeElement.currentTime = startTime;
                            this.video.nativeElement.play().then(r => console.log('Video playing...'));
                        }
                    });
                }
                console.log(`[JudgeMedia] src=${JSON.stringify(this.video.nativeElement.src)}, start=${startTime}, end=${endTime}`);
                this.video.nativeElement.addEventListener('loadeddata', () => { // TODO TypeError this.video.addEventListener is not a function
                    console.log('[JudgeMedia] Loaded complete');
                    this.video.nativeElement.currentTime = startTime;
                    this.video.nativeElement.play().then(r => console.log('[Judgem]'));
                });
            }

        });
        // https://developers.google.com/web/updates/2017/06/play-request-was-interrupted
        // Not working due to 401
        /* fetch(url)
            .then(response => response.blob())
            .then(blob => {
                this.videoTag.srcObject = blob;
                return this.videoTag.play();
            })
            .then(_ => {
            })
            .catch(e => {
                console.log('error on playback');
                console.log(e);
            }); */
    }

    stop() {
        if (this.video) {
            this.video.nativeElement.pause();
        }
        this.videoUrl = undefined;
    }

    ngOnDestroy(): void {
    }

}
