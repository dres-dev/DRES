import {Component, Input} from '@angular/core';
import {Observable, of} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {AppConfig} from '../app.config';
import {JudgementRequest} from '../../../openapi';

@Component({
    selector: 'app-judgement-media-viewer',
    templateUrl: './judgement-media-viewer.component.html',
    styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent {

    @Input() req: Observable<JudgementRequest>;

    videoUrl: Observable<SafeUrl>;

    constructor(private sanitizer: DomSanitizer, private config: AppConfig) {
    }

  private offset = 4;

  public judge(req: JudgementRequest) {
        let timeRange = '';
        if (req.startTime && req.endTime) {
            let start = Number.parseInt(req.startTime, 10);
            let end = Number.parseInt(req.endTime, 10);
            if (start === end) {
                start = start - this.offset < 0 ? 0 : start - this.offset;
                end = end + this.offset;
            }
            timeRange = `#t=${start},${end}`;
        }
        // TODO How to know here what type this media item has?
        const path = `/media/${req.collection}/${req.item}${timeRange}`;
        const url = this.config.resolveApiUrl(path);
        this.videoUrl = new Observable<SafeUrl>(subscriber => subscriber.next(url));
    }

    stop() {
        this.videoUrl = of(null);
    }
}
