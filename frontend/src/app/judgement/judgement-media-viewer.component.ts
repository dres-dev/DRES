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

    videoUrl: Observable<string> = of('http://localhost:8080/api/media/mini/00001');
    private offset = 5;

    private videoTag: HTMLVideoElement;

    constructor(private config: AppConfig) {
    }

    public judge(req: JudgementRequest) {
        console.log('[JudgeMedia] Judging: ' + JSON.stringify(req));
        const timeRange = '';
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
        const path = `/media/${req.collection}/${req.item}${timeRange}`;
        const url = this.config.resolveApiUrl(path);
        this.videoUrl = new Observable<string>(subscriber => subscriber.next(url));
        this.videoPlayer.time = startTime;
        if (endTime > 0) {
            this.videoTag.addEventListener('timeupdate', () => {
                if(this.videoTag.currentTime > endTime){
                    this.videoPlayer.time = startTime;
                }
            });
        }
        /*this.videoTag.play().then(r => {
            this.videoTag.muted = false;
        });*/
    }

    stop() {
        this.videoUrl = of(null);
    }

    ngAfterViewInit(): void {
        this.videoTag = this.videoPlayer.getVideoTag();
    }
}
