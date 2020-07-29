import {Component} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {CompetitionStartMessage} from '../../../../openapi';


export interface CompetitionStartDialogResult {
    name: string;
    type: CompetitionStartMessage.TypeEnum;
}

@Component({
    selector: 'app-competition-start-dialog',
    templateUrl: 'competition-start-dialog.component.html',
})
export class CompetitionStartDialogComponent {
    form: FormGroup = new FormGroup({name: new FormControl(''), type: new FormControl('')});
    runTypes: CompetitionStartMessage.TypeEnum[] = ['SYNCHRONOUS', 'ASYNCHRONOUS'];

    constructor(public dialogRef: MatDialogRef<CompetitionStartDialogComponent>) {}

    public create(): void {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.get('name').value,
                type: this.form.get('type').value} as CompetitionStartDialogResult);
        }
    }

    public close(): void {
        this.dialogRef.close(null);
    }
}
