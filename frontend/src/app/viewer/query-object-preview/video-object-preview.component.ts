import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { filter, map } from 'rxjs/operators';
import { DataUtilities } from '../../utilities/data.utilities';
import {ApiHint} from '../../../../openapi';

@Component({
  selector: 'app-video-object-preview',
  template: `
    <div class="video-container">
      <video
        #player
        *ngIf="videoUrl | async"
        [src]="videoUrl | async"
        class="video-player"
        style="width: 100%"
        controls
        [muted]="muted"
        (canplay)="handleCanPlay()"
        (ended)="handleEnded()"
      ></video>
    </div>
  `,
})
export class VideoObjectPreviewComponent implements OnInit {
  /** Observable of current {@link ContentElement} that should be displayed. Provided by user of this component. */
  @Input() queryObject: Observable<ApiHint>;

  /** Flag indicating whether video player should be muted or not. Can be provided by a user of this component. */
  @Input() muted = true;

  /** Indicates after how many repetitions the video player should be muted (default = 1). Can be provided by a user of this component. */
  @Input() muteAfter = 1;

  /** Reference to the {@link HTMLVideoElement} used for playback. */
  @ViewChild('player') player: ElementRef<HTMLVideoElement>;

  /** Current video to display (as data URL). */
  videoUrl: Observable<SafeUrl>;
  numberOfLoops = 0;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.videoUrl = this.queryObject.pipe(
      filter((q) => q.type === 'VIDEO'),
      map((q) => {
        if (q.mediaItem) {
          // FIXME differ between external video and internal (i.e. media item). the following code is very likely broken
          return this.sanitizer.bypassSecurityTrustUrl(DataUtilities.base64ToUrl(q.mediaItem, 'video/mp4')); // FIXME should q.dataType be respected?
        } else {
          return null;
        }
      })
    );
  }

  /**
   * Handles availability of data for video player. Requests fullscreen mode and starts playback
   */
  public handleCanPlay() {
    this.player.nativeElement.play().then((s) => {});
  }

  /**
   * Handles end of playback in video player. Mutes video and exists fullscreen mode (if enabled). Then restarts playback.
   */
  public handleEnded() {
    this.player.nativeElement.play().then((s) => {
      this.numberOfLoops += 1;
      this.muted = this.numberOfLoops >= this.muteAfter;
    });
  }
}
