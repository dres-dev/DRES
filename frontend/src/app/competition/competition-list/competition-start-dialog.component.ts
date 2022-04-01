import {Component} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {CompetitionStartMessage} from '../../../../openapi';


export interface CompetitionStartDialogResult {
    name: string;
    type: CompetitionStartMessage.TypeEnum;
    participantCanView: Boolean;
    shuffleTasks: Boolean;
}

@Component({
    selector: 'app-competition-start-dialog',
    templateUrl: 'competition-start-dialog.component.html',
})
export class CompetitionStartDialogComponent {
    form: FormGroup = new FormGroup({name: new FormControl(''), type: new FormControl(''), participantsCanView: new FormControl(true), shuffleTasks: new FormControl(false)});
    runTypes: CompetitionStartMessage.TypeEnum[] = ['SYNCHRONOUS', 'ASYNCHRONOUS'];

    constructor(public dialogRef: MatDialogRef<CompetitionStartDialogComponent>) {}

    public create(): void {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.get('name').value,
                type: this.form.get('type').value,
                participantCanView: this.form.get('participantsCanView').value,
                shuffleTasks: this.form.get('shuffleTasks').value } as CompetitionStartDialogResult);
        }
    }

    public close(): void {
        this.dialogRef.close(null);
    }
}
