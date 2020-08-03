import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {filter, map} from 'rxjs/operators';
import {QueryContentElement, QueryHint} from '../../../../openapi';

@Component({
    selector: 'app-video-query-object-preview',
    template: `
        <video #player *ngIf="(videoUrl | async)" [src]="(videoUrl | async)" class="video-player" style="width: 100%" controls [muted]="muted" (canplay)="handleCanPlay()" (ended)="handleEnded()"></video>
    `
})
export class VideoQueryObjectPreviewComponent implements OnInit {

    /** Observable of current {@link QueryContentElement} that should be displayed. Provided by user of this component. */
    @Input() queryObject: Observable<QueryContentElement>;

    /** Flag indicating whether video player should be muted or not. Can be provided by a user of this component. */
    @Input() muted = true;

    /** Indicates after how many repetitions the video player should be muted (default = 1). Can be provided by a user of this component. */
    @Input() muteAfter = 1;

    @ViewChild('player') player: ElementRef<HTMLVideoElement>;
    videoUrl: Observable<SafeUrl>;
    numberOfLoops = 0;

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
            filter(q => q.contentType === 'VIDEO'),
            map(q => this.sanitizer.bypassSecurityTrustUrl(VideoQueryObjectPreviewComponent.base64ToUrl(q.content, 'video/mp4')))
        );
    }

    /**
     * Handles availability of data for video player. Requests fullscreen mode and starts playback
     */
    public handleCanPlay() {
        this.player.nativeElement.play().then(s => {});
    }

    /**
     * Handles end of playback in video player. Mutes video and exists fullscreen mode (if enabled). Then restarts playback.
     */
    public handleEnded() {
        this.player.nativeElement.play().then(s => {
            this.numberOfLoops += 1;
            this.muted = (this.numberOfLoops >= this.muteAfter);
        });
    }
}
