import {Component, Input, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {filter, map} from 'rxjs/operators';
import {VideoQueryDescription} from '../../../../openapi';

@Component({
    selector: 'app-video-query-object-preview',
    template: `
        <video *ngIf="(videoUrl | async)" [src]="(videoUrl | async)" type="video/mp4" class="video-player" style="width: 100%" autoplay controls loop [muted]="muted"></video>
    `
})
export class VideoQueryObjectPreviewComponent implements OnInit {

    @Input() queryObject: Observable<VideoQueryDescription>;
    @Input() muted = true;
    videoUrl: Observable<SafeUrl>;


    /**
     * Converts a Base65 encoded string into an object URL of a Blob.
     *
     * @param base64 The base64 encoded string.
     * @param contentType The content type of the data.
     */
    private static base64ToUrl(base64: string, contentType: string): string {
        const binary = atob(base64);
        const byteNumbers = new Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            byteNumbers[i] = binary.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray], {type: contentType});
        return window.URL.createObjectURL(blob);
    }

    constructor(private sanitizer: DomSanitizer) {}

    ngOnInit(): void {
        this.videoUrl = this.queryObject.pipe(
            filter(q => q?.video != null),
            map(q => this.sanitizer.bypassSecurityTrustUrl(VideoQueryObjectPreviewComponent.base64ToUrl(q.video, q.contentType)))
        );
    }
}
