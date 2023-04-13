import { Component, Inject, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {ApiMediaItem, ApiMediaType} from '../../../../../openapi';

export interface MediaItemBuilderData {
  item?: ApiMediaItem;
  collectionId: string;
}

@Component({
  selector: 'app-media-item-builder-dialog',
  templateUrl: './media-item-builder-dialog.component.html',
  styleUrls: ['./media-item-builder-dialog.component.scss'],
})
export class MediaItemBuilderDialogComponent implements OnInit {
  form: UntypedFormGroup;

  types = Object.values(ApiMediaType).sort((a, b) => a.localeCompare(b));

  constructor(
    public dialogRef: MatDialogRef<MediaItemBuilderDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MediaItemBuilderData
  ) {
    this.form = new UntypedFormGroup({
      id: new UntypedFormControl(data?.item?.mediaItemId),
      name: new UntypedFormControl(data?.item?.name, [Validators.required, Validators.minLength(3)]),
      location: new UntypedFormControl(data?.item?.location, Validators.required),
      type: new UntypedFormControl({ value: data?.item?.type, disabled: this.isEditing() }, [Validators.required]),
      collectionId: new UntypedFormControl(data.collectionId),
    });
    if (data?.item?.type === ApiMediaType.VIDEO) {
      this.form.addControl('durationMs', new UntypedFormControl(data?.item?.durationMs, [Validators.required, Validators.min(1)]));
      this.form.addControl(
        'fps',
        new UntypedFormControl(data?.item?.fps, [Validators.required, Validators.min(1), Validators.max(200)])
      ); // Arbitrarily limiting to 200 fps
    }
  }

  enableVideoItemControls(enable: boolean) {
    if (enable) {
      this.form.addControl('durationMs', new UntypedFormControl(0, [Validators.required, Validators.min(1)]));
      this.form.addControl('fps', new UntypedFormControl(0, [Validators.required, Validators.min(1), Validators.max(200)]));
    } else {
      this.form.removeControl('durationMs');
      this.form.removeControl('fps');
    }
  }

  isEditing(): boolean {
    return this.data?.item?.mediaItemId !== undefined;
  }

  ngOnInit(): void {}

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

  private fetchFormData(): ApiMediaItem {
    const item = {
      name: this.form.get('name').value,
      location: this.form.get('location').value,
      type: this.form.get('type').value,
      collectionId: this.form.get('collectionId').value,
    } as ApiMediaItem;
    /* Are we editing ? */
    if (this.isEditing()) {
      item.mediaItemId = this.form.get('id').value;
    }
    /* only relevant for video */
    if (item.type === ApiMediaType.VIDEO) {
      item.durationMs = this.form.get('durationMs').value;
      item.fps = this.form.get('fps').value;
    }
    return item;
  }
}
