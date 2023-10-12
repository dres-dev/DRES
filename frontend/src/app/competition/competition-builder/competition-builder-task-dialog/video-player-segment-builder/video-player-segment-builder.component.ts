import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from "@angular/core";
import { Observable, of, Subscription } from 'rxjs';
import { ApiMediaItem, ApiTemporalPoint, ApiTemporalRange } from '../../../../../../openapi';
import { AppConfig } from '../../../../app.config';

/**
 * DTO for [VideoPlayerSegmentBuilder] configuration.
 * Requires either `mediaItem` or `externalPath` to be set.
 * `segmentStart` and `segmentEnd` are optional
 */
export interface VideoPlayerSegmentBuilderData {
  mediaItem?: ApiMediaItem;
  segmentStart?: number;
  segmentEnd?: number;
  externalPath?:string;
}


@Component({
  selector: 'app-video-player-segment-builder',
  templateUrl: './video-player-segment-builder.component.html',
  styleUrls: ['./video-player-segment-builder.component.scss'],
})
export class VideoPlayerSegmentBuilderComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() data: VideoPlayerSegmentBuilderData;
  @Output() rangeChange = new EventEmitter<ApiTemporalRange>();
  @Input() showTitle = true

  @ViewChild('videoPlayer', { static: false }) video: ElementRef;
  videoUrl: Observable<string>;
  playtimeRelative: Observable<number>;
  /* Apparently, some arbitrary default values are required */
  startInSeconds = 0;
  endInSeconds = 100;
  durationInSeconds = 100;

  doLoop = true;

  private requestSub: Subscription;

  isMediaItemPlayer = false;

  constructor(
    public config: AppConfig /*,
                public dialogRef: MatDialogRef<VideoPlayerSegmentBuilderData>,
                @Inject(MAT_DIALOG_DATA) public data: VideoPlayerSegmentBuilderData*/
  ) {
  }

  ngAfterViewInit(): void {
    console.log("VIDEO DATA: ",this.data);
    console.log("MEDIA ITEM", this.isMediaItemPlayer)
    setTimeout(() => {
      /*
       * timeout because of value changed after checking thingy
       * https://blog.angular-university.io/angular-debugging/
       */
      if (this.data) {
        this.videoUrl = of(
          this.isMediaItemPlayer ? this.config.resolveMediaItemUrl(this.data.mediaItem.mediaItemId) : this.config.resolveExternalUrl(this.data.externalPath)
        );
      }
      if (this.data.segmentStart) {
        this.startInSeconds = this.data.segmentStart === -1 ? 0 : this.data.segmentStart;
      }
      if (this.data.segmentEnd) {
        this.endInSeconds = this.data.segmentEnd === -1 ? this.data.mediaItem.durationMs / 1000 : this.data.segmentEnd;
      }

      /* Custom loop handler */
      this.video.nativeElement.addEventListener('timeupdate', () => {
        const playtime =
          ((this.video.nativeElement.currentTime - this.startInSeconds) / (this.endInSeconds - this.startInSeconds)) * 100;
        this.playtimeRelative = new Observable<number>((subscriber) => subscriber.next(playtime));
        if (this.endInSeconds) {
          if (this.video.nativeElement.currentTime >= this.endInSeconds) {
            if (this.doLoop) {
              console.log('Rewind video');
              this.video.nativeElement.currentTime = this.startInSeconds;
              this.video.nativeElement.play().then((r) => {});
            } else {
              console.log('Finished. Stopping now');
              this.video.nativeElement.currentTime = this.startInSeconds;
              this.video.nativeElement.pause();
            }
          }
        }
      });

      /* custom handler to force-start when loaded. */
      this.video.nativeElement.addEventListener('loadeddata', () => {
        console.log('Event loadeddata fired.');
        this.video.nativeElement.currentTime = this.startInSeconds;
        this.video.nativeElement.play().then((r) => console.log('Playing video after event fired'));
      });
    });
  }


  stop() {
    this.videoUrl = undefined;
    this.startInSeconds = undefined;
    this.endInSeconds = undefined;
    this.video.nativeElement.pause();
  }

  ngOnDestroy(): void {
    this.stop();
    if (this.requestSub) {
      this.requestSub.unsubscribe();
    }
  }

  togglePlaying() {
    if (this.video && this.video.nativeElement) {
      if (this.video.nativeElement.paused) {
        this.video.nativeElement.play();
      } else {
        this.video.nativeElement.pause();
      }
    }
  }

  play() {
    if (this.videoUrl && this.video && this.video.nativeElement) {
      this.video.nativeElement.play();
    }
  }

  /**
   * Currently only logs the formdata as json
   * @deprecated
   */
  export(): void {
    console.log(this.asJson());
  }

  /**
   * @deprecated
   */
  asJson(): string {
    return JSON.stringify(this.fetchData());
  }

  recalcVideoTime($event: Event) {
    console.log(`Change: ${this.startInSeconds} - ${this.endInSeconds}`);
    this.video.nativeElement.currentTime = this.startInSeconds;
    this.rangeChange.emit(this.fetchData());
  }

  setStart() {
    this.startInSeconds = this.video.nativeElement.currentTime;
    this.recalcVideoTime(null);
  }

  setEnd() {
    this.endInSeconds = this.video.nativeElement.currentTime;
    this.recalcVideoTime(null);
  }

  public fetchData() {
    const out = {
      start: { value: this.startInSeconds.toString(), unit: 'SECONDS' } as ApiTemporalPoint,
      end: { value: this.endInSeconds.toString(), unit: 'SECONDS' } as ApiTemporalPoint,
    } as ApiTemporalRange;
    console.log(`Fetched: ${out}`);
    return out;
  }

  ngOnInit(): void {
    if(this.data){
      if(this.data.mediaItem && this.data.mediaItem.mediaItemId){
        console.log("media item!")
        this.isMediaItemPlayer = true;
      }
    }
  }
}
