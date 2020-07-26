import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {RestTaskDescriptionComponent, TaskType, TemporalRange} from '../../../../../openapi';


export interface CompetitionBuilderTaskDescriptionComponentDialogData {
    /**
     * The type for which to create
     */
    type: TaskType.ComponentsEnum;
    comp?: RestTaskDescriptionComponent;
}

@Component({
    selector: 'app-competition-builder-task-description-component-dialog',
    templateUrl: './competition-builder-task-description-component-dialog.component.html',
    styleUrls: ['./competition-builder-task-description-component-dialog.component.scss']
})
export class CompetitionBuilderTaskDescriptionComponentDialogComponent implements OnInit {

    /** FromGroup for this dialog. */
    form: FormGroup;

    constructor(
        public dialogRef: MatDialogRef<CompetitionBuilderTaskDescriptionComponentDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderTaskDescriptionComponentDialogData) {
        this.form = new FormGroup({
            type: new FormControl(this?.data.type),
            start: new FormControl(this?.data?.comp?.start, [Validators.required, Validators.min(0)]),
            end: new FormControl(this?.data?.comp?.end, [Validators.required, Validators.min(0)/*, Validators.min(this.form.get('start').value)*/]),
        });
        switch (this?.data.type) {
            case 'IMAGE_ITEM':
                this.form.addControl('mediaItem', new FormControl(this?.data?.comp?.mediaItem, [Validators.required]));
                break;
          case 'VIDEO_ITEM_SEGMENT':
                this.form.addControl('mediaItem', new FormControl(this?.data?.comp?.mediaItem, [Validators.required]));
                this.form.addControl('range.start', new FormControl(this?.data?.comp?.range?.start, [Validators.required, Validators.min(0)]));
                this.form.addControl('range.end', new FormControl(this?.data?.comp?.range?.end, [Validators.required, Validators.min(0)])); // fixme min is actually range.start
                break;
            case 'TEXT':
                this.form.addControl('description', new FormControl(this?.data?.comp?.description, [Validators.required]));
                break;
            case 'EXTERNAL_IMAGE':
                this.form.addControl('payload', new FormControl(this?.data?.comp?.payload, [Validators.required]));
                this.form.addControl('dataType', new FormControl(this?.data?.comp?.dataType, [Validators.required]));
                break;
            case 'EXTERNAL_VIDEO':
                this.form.addControl('payload', new FormControl(this?.data?.comp?.payload, [Validators.required]));
                this.form.addControl('dataType', new FormControl(this?.data?.comp?.dataType, [Validators.required]));
              this.form.addControl('range.start', new FormControl(this?.data?.comp?.range?.start, [Validators.required, Validators.min(0)]));
              this.form.addControl('range.end', new FormControl(this?.data?.comp?.range?.end, [Validators.required, Validators.min(0)])); // fixme min is actually range.start
                break;

        }
    }

    public save(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.fetchFormData());
        }
    }

    public asJson(): string {
        return JSON.stringify(this.fetchFormData());
    }

    public close(): void {
        this.dialogRef.close(null);
    }

    ngOnInit(): void {
    }

    export() {
        console.log(this.asJson());
    }

    isTextual() {
        return this.data.type === TaskType.ComponentsEnum.TEXT;
    }

    isImageItem() {
        return this.data.type === TaskType.ComponentsEnum.IMAGEITEM;
    }

    isVideoItem() {
        return this.data.type === TaskType.ComponentsEnum.VIDEOITEMSEGMENT;
    }

    isExtImage() {
        return this.data.type === TaskType.ComponentsEnum.EXTERNALIMAGE;
    }

    isExtVideo() {
        return this.data.type === TaskType.ComponentsEnum.EXTERNALVIDEO;
    }

    private fetchFormData(): RestTaskDescriptionComponent {
        const data = {
            type: this.form.get('type').value,
            start: this.form.get('start').value,
            end: this.form.get('end').value,
        } as RestTaskDescriptionComponent;
        switch (this.data.type) {
            case 'IMAGE_ITEM':
                data.mediaItem = this.form.get('mediaItem').value;
                break;
            case 'VIDEO_ITEM_SEGMENT':
                data.mediaItem = this.form.get('mediaItem').value;
                data.range = {start: this.form.get('range.start').value, end: this.form.get('range.end').value} as TemporalRange;
                break;
            case 'TEXT':
                data.description = this.form.get('description').value;
                break;
            case 'EXTERNAL_IMAGE':
                data.payload = this.form.get('payload').value;
                data.dataType = this.form.get('dataType').value;
                break;
            case 'EXTERNAL_VIDEO':
                data.payload = this.form.get('payload').value;
                data.dataType = this.form.get('dataType').value;
              data.range = {start: this.form.get('range.start').value, end: this.form.get('range.end').value} as TemporalRange;
                break;

        }

        return data;
    }
}
