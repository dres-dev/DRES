import {Component, Inject, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {RestMediaItem} from '../../../../../openapi';

@Component({
    selector: 'app-media-item-builder-dialog',
    templateUrl: './media-item-builder-dialog.component.html',
    styleUrls: ['./media-item-builder-dialog.component.scss']
})
export class MediaItemBuilderDialogComponent implements OnInit {

    form: FormGroup;

    types = Object.values(RestMediaItem.TypeEnum)
        .sort((a, b) => a.localeCompare(b));

    constructor(
        public dialogRef: MatDialogRef<MediaItemBuilderDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: RestMediaItem
    ) {

        this.form = new FormGroup({
            id: new FormControl(data?.id),
            name: new FormControl(data?.name, [Validators.required, Validators.minLength(3)]),
            location: new FormControl({value: data?.location, disabled: this.isEditing()}),
            type: new FormControl(data?.type, [Validators.required])
        });
        if (data?.type === RestMediaItem.TypeEnum.VIDEO) {
            this.form.addControl('durationMs', new FormControl(data?.durationMs, [Validators.required, Validators.min(1)]));
            this.form.addControl('fps', new FormControl(data?.fps, [Validators.required, Validators.min(1), Validators.max(200)])); // Arbitrarily limiting to 200 fps
        }
    }


    isEditing(): boolean {
        return this.data?.id !== undefined;
    }

    ngOnInit(): void {
    }

    save(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.fetchFormData());
        }
    }

    close(): void {
        this.dialogRef.close(null);
    }

    asJson(): string {
        return JSON.stringify(this.fetchFormData());
    }

    export(): void {
        console.log(this.asJson());
    }

    private fetchFormData(): RestMediaItem {
        const item = {
            name: this.form.get('name').value,
            location: this.form.get('location').value,
            type: this.form.get('type').value
        } as RestMediaItem;
        /* Are we editing ? */
        if (this.isEditing()) {
            item.id = this.form.get('id').value;
        }
        /* only relevant for video */
        if (item.type === RestMediaItem.TypeEnum.VIDEO) {
            item.durationMs = this.form.get('durationMs').value;
            item.fps = this.form.get('fps').value;
        }
        return item;
    }
}
