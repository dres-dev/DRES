import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';


export interface CompetitionBuilderAddTeamDialogResult {
    name: string;
    number: number;
    color: string;
    logo: string;
}


@Component({
    selector: 'app-competition-builder-add-team-dialog',
    templateUrl: './competition-builder-add-team-dialog.component.html'
})
export class CompetitionBuilderAddTeamDialogComponent implements OnInit {

    form: FormGroup = new FormGroup({
        name: new FormControl(''),
        number: new FormControl(''),
        color: new FormControl(''),
        logo: new FormControl('')
    });

    logoName = '';

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderAddTeamDialogComponent>) {
        this.form.get('color').setValue(this.randomColor());
    }

    ngOnInit(): void {
    }

    public add(): void {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.get('name').value as string,
                number: this.form.get('number').value as number,
                color: this.form.get('color').value as string,
                logo: this.form.get('logo').value as string
            } as CompetitionBuilderAddTeamDialogResult);
        }
    }

    public close(): void {
        this.dialogRef.close(null);
    }

    public randomColor(): string {
        const letters = '0123456789ABCDEF';
        let color = '#';
        for (let i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    }


    /**
     * Processes an uploaded image.
     *
     * @param event
     */
    public processImage(event) {
        const file: File = event.target.files[0];
        const reader = new FileReader();
        this.logoName = file.name;
        reader.readAsDataURL(file);
        reader.onload = () => {
            this.form.get('logo').setValue(reader.result);
        };
    }
}
