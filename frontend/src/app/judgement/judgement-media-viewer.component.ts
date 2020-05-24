import {AfterViewInit, Component, ElementRef, Input, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {AppConfig} from '../app.config';
import {JudgementRequest} from '../../../openapi';
import {MatVideoComponent} from 'mat-video/lib/video.component';

@Component({
    selector: 'app-judgement-media-viewer',
    templateUrl: './judgement-media-viewer.component.html',
    styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent implements AfterViewInit {

    @Input() req: Observable<JudgementRequest>;

    @ViewChild('videoPlayer', {static: false}) video: HTMLVideoElement;

    videoUrl: Observable<string>;
    private offset = 5;

    constructor(private config: AppConfig) {
    }

    public judge(req: JudgementRequest) {
        console.log('[JudgeMedia] Judging: ' + JSON.stringify(req));
        let startTime = 0;
        if (req.startTime) {
            startTime = Number.parseInt(req.startTime, 10) / 1000; // ms?
        }
        let endTime = -1;
        if (req.endTime) {
            endTime = Number.parseInt(req.endTime, 10) / 1000;
        }
        if (startTime === endTime) {
            startTime = startTime - this.offset < 0 ? 0 : startTime - this.offset;
            endTime = endTime + this.offset;
        }
        // TODO How to know here what type this media item has?
        const path = `/media/${req.collection}/${req.item}#t=${startTime},${endTime}`; // Should work (in chorme, directly this works)
        // const path = `/media/${req.collection}/${req.item}`; // Should work (in chorme, directly this works)
        const url = this.config.resolveApiUrl(path);
        this.videoUrl = new Observable<string>(subscriber => {
            subscriber.next(url);
            if (this.video) {
                // This code is not called, as videoPlayer does not exist -- yet
                this.video.src = url;

                this.video.currentTime = startTime;
                this.video.loop = true;
                if (endTime > 0) {
                    this.video.addEventListener('timeupdate', () => {
                        console.log(`[JudgeMedia] Playing@${this.video.currentTime}s`);
                        if (this.video.currentTime >= endTime) {
                            console.log('[JudgeMedia] Restarting video');
                            this.video.currentTime = startTime;
                            this.video.play().then(r => console.log('Video playing...'));
                        }
                    });
                }
                console.log(`[JudgeMedia] src=${JSON.stringify(this.video.src)}, start=${startTime}, end=${endTime}`);
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
            this.video.pause();
        }
        this.videoUrl = undefined;
    }

    ngAfterViewInit(): void {
    }
}
