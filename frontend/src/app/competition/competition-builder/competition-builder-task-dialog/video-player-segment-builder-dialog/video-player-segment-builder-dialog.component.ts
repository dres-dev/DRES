import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { AppConfig } from '../../../../app.config';
import {
  VideoPlayerSegmentBuilderComponent,
  VideoPlayerSegmentBuilderData,
} from '../video-player-segment-builder/video-player-segment-builder.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

/**
 * @deprecated This component should not be used anymore, as there is no need for a dialog anymore
 */
@Component({
  selector: 'app-video-player-segment-builder-dialog',
  templateUrl: './video-player-segment-builder-dialog.component.html',
  styleUrls: ['./video-player-segment-builder-dialog.component.scss'],
})
export class VideoPlayerSegmentBuilderDialogComponent implements OnInit {
  @ViewChild(VideoPlayerSegmentBuilderComponent) videoPlayer: VideoPlayerSegmentBuilderComponent;

  constructor(
    public config: AppConfig,
    public dialogRef: MatDialogRef<VideoPlayerSegmentBuilderData>,
    @Inject(MAT_DIALOG_DATA) public data: VideoPlayerSegmentBuilderData
  ) {}

  ngOnInit(): void {}

  /**
   * Fetches the data from the form, returns it to the dialog openeer and cloeses this dialog
   */
  save(): void {
    console.log('save');
    this.dialogRef.close(this.videoPlayer.fetchData());
  }

  /**
   * Closes this dialog without saving
   */
  close(): void {
    console.log('close');
    this.dialogRef.close(null);
  }
}
