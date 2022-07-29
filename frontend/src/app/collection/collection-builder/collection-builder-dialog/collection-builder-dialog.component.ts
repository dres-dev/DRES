import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RestMediaCollection } from '../../../../../openapi';
import { FormControl, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-collection-builder-dialog',
  templateUrl: './collection-builder-dialog.component.html',
  styleUrls: ['./collection-builder-dialog.component.scss'],
})
export class CollectionBuilderDialogComponent implements OnInit {
  form: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<CollectionBuilderDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RestMediaCollection
  ) {
    this.form = new FormGroup({
      id: new FormControl(data?.id),
      name: new FormControl(data?.name, [Validators.required, Validators.minLength(3)]),
      description: new FormControl(data?.description),
      basePath: new FormControl(data?.basePath),
    });
  }

  isEditing(): boolean {
    return this.data?.id !== undefined;
  }

  /**
   * Fetches the data from the form, returns it to the dialog openeer and cloeses this dialog
   */
  save(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.fetchFormData());
    }
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
    return JSON.stringify(this.fetchFormData());
  }

  ngOnInit(): void {}

  /**
   * Fetchs the data from the form and transforms it to wireformat
   */
  private fetchFormData(): RestMediaCollection {
    const col = {
      name: this.form.get('name').value,
      description: this.form.get('description').value,
      basePath: this.form.get('basePath').value,
    } as RestMediaCollection;
    /* Only set id, if pre-existing, i.e. this is an edit dialog */
    if (this.form.get('id')?.value) {
      col.id = this.form.get('id').value;
    }
    return col;
  }
}
