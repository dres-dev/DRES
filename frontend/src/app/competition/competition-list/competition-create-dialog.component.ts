import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormControl, FormGroup} from '@angular/forms';


export interface CompetitionCreateDialogResult {
    name: string
    description: string
}

@Component({
    selector: 'app-competition-create-dialog',
    templateUrl: 'competition-create-dialog.component.html',
})
export class CompetitionCreateDialogComponent {
    form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});

    constructor(public dialogRef: MatDialogRef<CompetitionCreateDialogComponent>) {}

    public create(): void {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.controls.name.value,
                description: this.form.controls.name.value} as CompetitionCreateDialogResult);
        }
    }

    public close(): void {
        this.dialogRef.close({ });
    }
}
