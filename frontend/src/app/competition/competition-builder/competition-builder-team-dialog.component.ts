import {Component, Inject, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Team} from '../../../../openapi';


@Component({
    selector: 'app-competition-builder-add-team-dialog',
    templateUrl: './competition-builder-team-dialog.component.html'
})
export class CompetitionBuilderTeamDialogComponent implements OnInit {

    form: FormGroup;
    logoName = '';

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTeamDialogComponent>,
                @Inject(MAT_DIALOG_DATA) public team?: Team) {

        if (team) {
            this.form = new FormGroup({
                name: new FormControl(team?.name, [Validators.required, Validators.minLength(3)]),
                color: new FormControl(team?.color, [Validators.required, Validators.minLength(7), Validators.maxLength(7)]),
                logo: new FormControl(team?.logo, Validators.required)
            });
        } else {
            this.form = new FormGroup({
                name: new FormControl('', [Validators.required, Validators.minLength(3)]),
                color: new FormControl(
                    CompetitionBuilderTeamDialogComponent.randomColor(),
                    [Validators.required, Validators.minLength(7), Validators.maxLength(7)]
                ),
                logo: new FormControl('', Validators.required)
            });
        }
    }

    /**
     * Generates a random HTML color.
     */
    private static randomColor(): string {
        const letters = '0123456789ABCDEF';
        let color = '#';
        for (let i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    }

    ngOnInit(): void {
    }

    public save(): void {
        if (this.form.valid) {
            this.dialogRef.close({
                name: this.form.get('name').value,
                color: this.form.get('color').value,
                logo: this.form.get('logo').value
            } as Team);
        }
    }

    public close(): void {
        this.dialogRef.close(null);
    }

    /**
     * Processes an uploaded image.
     *
     * @param event The upload event.
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
