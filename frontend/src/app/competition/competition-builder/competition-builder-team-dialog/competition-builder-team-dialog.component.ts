import {Component, Inject} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {RestTeam, UserDetails, UserService} from '../../../../../openapi';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {map, shareReplay} from 'rxjs/operators';
import {Observable} from 'rxjs';


@Component({
    selector: 'app-competition-builder-add-team-dialog',
    templateUrl: './competition-builder-team-dialog.component.html'
})
export class CompetitionBuilderTeamDialogComponent {

    form: FormGroup;
    logoName = '';
    availableUsers: Observable<UserDetails[]>;

    constructor(public dialogRef: MatDialogRef<CompetitionBuilderTeamDialogComponent>,
                public userService: UserService,
                @Inject(MAT_DIALOG_DATA) public team?: RestTeam) {

        this.form = new FormGroup({
            name: new FormControl(team?.name, [Validators.required, Validators.minLength(3)]),
            color: new FormControl(team?.color ? team.color : CompetitionBuilderTeamDialogComponent.randomColor(), [Validators.required, Validators.minLength(7), Validators.maxLength(7)]),
            logoId: new FormControl(team?.logoId),
            logoData: new FormControl(team?.logoData),
            users: new FormControl(team?.users != null ? team.users : []),
            userInput: new FormControl('')
        });
        this.availableUsers = this.userService.getApiUserList().pipe(
            shareReplay(1)
        );
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

    fileProvider = () => this.fetchData()?.name ? this.fetchData().name : 'team-download.json';

    downloadProvider = () => this.asJson();

    /**
     * Adds the selected user to the list of users.
     *
     * @param event
     */
    public addUser(event: MatAutocompleteSelectedEvent): void {
        this.form.get('users').value.push(event.option.value.id);
        this.form.get('userInput').setValue(null);
    }

    /**
     * Removes the selected user from the list of users.
     *
     * @param user The selected user.
     */
    public removeUser(user: number): void {
        const index = this.form.get('users').value.indexOf(user);
        if (index >= 0) {
            this.form.get('users').value.splice(index, 1);
        }
    }

    /**
     * Returns the user for the given user id or null
     *
     * @param id User ID of the desired user.
     */
    public userForId(id: string): Observable<UserDetails> {
        return this.availableUsers.pipe(
            map(users => users.find(u => u.id === id))
        );
    }

    /**
     * Saves all changs in the dialog and closes it.
     */
    public save(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.fetchData());
        }
    }

    asJson(): string {
        return JSON.stringify(this.fetchData());
    }

    fetchData() {
        return {
            name: this.form.get('name').value,
            color: this.form.get('color').value,
            logoData: this.form.get('logoData').value,
            logoId: this.form.get('logoId').value,
            users: this.form.get('users').value
        } as RestTeam;
    }

    /**
     * Closes the dialog without saving.
     */
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
            this.form.get('logoData').setValue(reader.result);
        };
    }
}
