import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs';
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

    public judge(req: JudgementRequest) {
        console.log('Judge Media: Request:');
        console.log(req);
        let timeRange = '';
        if (req.startTime && req.endTime) {
            timeRange = `#t=${req.startTime},${req.endTime}`;
        }
        // TODO How to know here what type this media item has?
        const path = `/media/${req.collection}/${req.item}${timeRange}`;
        console.log('url is: ' + path);
        const url = this.config.resolveApiUrl(path);
        this.videoUrl = new Observable<SafeUrl>(subscriber => subscriber.next(url));
    }

}
