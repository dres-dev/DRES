import {Component, Input, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {map} from 'rxjs/operators';
import {VideoQueryDescription} from '../../../../openapi';

@Component({
    selector: 'app-video-query-object-preview',
    template: `
        <video [src]="(videoUrl | async)" class="video-player" style="width: 100%" autoplay loop>

        </video>
    `
})
export class VideoQueryObjectPreviewComponent implements OnInit {

    @Input() queryObject: Observable<VideoQueryDescription>;

    videoUrl: Observable<SafeUrl>;

    /**
     * Converts a Base65 encoded string into an object URL of a Blob.
     *
     * @param base64 The base64 encoded string.
     */
    private static base64ToUrl(base64: string): string {
        const binary = atob(base64);
        const byteNumbers = new Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            byteNumbers[i] = binary.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray]);
        return window.URL.createObjectURL(blob);
    }

    constructor(private sanitizer: DomSanitizer) {}

    ngOnInit(): void {
        this.videoUrl = this.queryObject.pipe(
            map(d => this.sanitizer.bypassSecurityTrustUrl(VideoQueryObjectPreviewComponent.base64ToUrl(d.video)))
        );
    }
}
