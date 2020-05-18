import {AfterViewInit, Component, Input, ViewChild} from '@angular/core';
import {Observable, of} from 'rxjs';
import {AppConfig} from '../app.config';
import {JudgementRequest} from '../../../openapi';
import {MatVideoComponent} from 'mat-video/lib/video.component';

@Component({
    selector: 'app-judgement-media-viewer',
    templateUrl: './judgement-media-viewer.component.html',
    styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent implements AfterViewInit {

    @ViewChild('video') videoPlayer: MatVideoComponent;
    @Input() req: Observable<JudgementRequest>;

    videoUrl: Observable<string>;
    private offset = 5;

    private videoTag: HTMLVideoElement;

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
        const url = this.config.resolveApiUrl(path);
        this.videoUrl = new Observable<string>(subscriber => subscriber.next(url));
        this.videoPlayer.time = startTime;
        this.videoPlayer.lastTime = endTime;
        this.videoPlayer.src = url;
        this.videoPlayer.load();
        this.videoPlayer.playing = true;
        if (endTime > 0) {
            this.videoTag.addEventListener('timeupdate', () => {
                console.log(`[JudgeMedia] Playing@${this.videoTag.currentTime}s`);
                if (this.videoTag.currentTime > endTime) {
                    console.log('[JudgeMedia] Restarting video');
                    this.videoTag.currentTime = startTime;
                }
            });
        }
        console.log(`[JudgeMedia] src=${JSON.stringify(this.videoPlayer.src)}, start=${startTime}, end=${endTime}`);
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
        this.videoTag.pause();
        this.videoTag.src = '';
        this.videoUrl = of(null);
    }

    ngAfterViewInit(): void {
        this.videoTag = this.videoPlayer.getVideoTag();
    }
}
