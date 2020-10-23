import {Component, Inject, OnInit} from '@angular/core';
import {CompetitionFormBuilder} from '../competition-form.builder';
import {AppConfig} from '../../../../app.config';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {VideoPlayerSegmentBuilderData} from '../video-player-segment-builder/video-player-segment-builder.component';

export class AdvancedBuilderDialogData {
    builder: CompetitionFormBuilder;
}

@Component({
    selector: 'app-advanced-builder-dialog',
    templateUrl: './advanced-builder-dialog.component.html',
    styleUrls: ['./advanced-builder-dialog.component.scss']
})
export class AdvancedBuilderDialogComponent implements OnInit {

    public textualInput = '';

    constructor(public config: AppConfig,
                public dialogRef: MatDialogRef<VideoPlayerSegmentBuilderData>,
                @Inject(MAT_DIALOG_DATA) public data: VideoPlayerSegmentBuilderData) {
    }

    ngOnInit(): void {
    }

    /**
     * Fetches the data from the form, returns it to the dialog openeer and cloeses this dialog
     */
    save(): void {
        this.dialogRef.close(this.fetchData());
    }

    /**
     * Closes this dialog without saving
     */
    close(): void {
        this.dialogRef.close(null);
    }

    /**
     * Currently only logs the formdata as json
     */
    export(): void {
        console.log(this.asJson());
    }

    asJson(): string {
        return JSON.stringify(this.fetchData());
    }

    private fetchData() {
        const out = this.textualInput.split('\n');
        console.log(`Fetched: ${out}`);
        return out;
    }

}
