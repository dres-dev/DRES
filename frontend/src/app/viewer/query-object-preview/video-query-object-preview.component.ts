import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {filter, map} from 'rxjs/operators';
import {VideoQueryDescription} from '../../../../openapi';

@Component({
    selector: 'app-video-query-object-preview',
    template: `
        <video #player *ngIf="(videoUrl | async)" [src]="(videoUrl | async)" class="video-player" style="width: 100%" controls [muted]="muted" (canplay)="handleCanPlay()" (ended)="handleEnded()"></video>
    `
})
export class VideoQueryObjectPreviewComponent implements OnInit {

    @Input() muteAfter = 0;
    @Input() queryObject: Observable<VideoQueryDescription>;
    @Input() muted = true;
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
            filter(q => q?.video != null),
            map(q => this.sanitizer.bypassSecurityTrustUrl(VideoQueryObjectPreviewComponent.base64ToUrl(q.video, q.contentType)))
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
        this.muted = (this.numberOfLoops >= this.muteAfter);
        this.player.nativeElement.play().then(s => this.numberOfLoops += 1);
    }
}
