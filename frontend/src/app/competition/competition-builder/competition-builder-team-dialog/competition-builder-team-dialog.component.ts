import { Component, Inject } from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { map, shareReplay } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { AppConfig } from '../../../app.config';
import {ApiTeam, ApiUser, UserService} from '../../../../../openapi';

@Component({
  selector: 'app-competition-builder-add-team-dialog',
  templateUrl: './competition-builder-team-dialog.component.html',
  styleUrls: ['./competition-builder-team-dialog.component.scss']
})
export class CompetitionBuilderTeamDialogComponent {
  form: FormGroup;
  logoName = '';
  availableUsers: Observable<ApiUser[]>;
  colorPalette = [
    '#BF0000',
    '#BF3900',
    '#BF7200',
    '#BFAC00',
    '#99BF00',
    '#5FBF00',
    '#26BF00',
    '#00BF13',
    '#00BF4C',
    '#00BF85',
    '#00BFBF',
    '#0085BF',
    '#004CBF',
    '#0013BF',
    '#2600BF',
    '#5F00BF',
    '#9800BF',
    '#BF00AC',
    '#BF0072',
  ];

  constructor(
    private dialogRef: MatDialogRef<CompetitionBuilderTeamDialogComponent>,
    private userService: UserService,
    private config: AppConfig,
    @Inject(MAT_DIALOG_DATA) private team?: ApiTeam
  ) {
    this.form = new FormGroup({
      id: new FormControl(team?.id),
      name: new FormControl(team?.name, [Validators.required, Validators.minLength(3)]),
      color: new FormControl(team?.color || this.colorPalette[Math.floor(Math.random() * this.colorPalette.length)], [
        Validators.required,
        Validators.minLength(7),
        Validators.maxLength(7),
      ]),
      logoData: new FormControl(team?.logoData),
      users: new FormControl(team?.users || []),
      userInput: new FormControl(''),
    });
    this.availableUsers = this.userService.getApiV2UserList().pipe(
      map((value) => value.filter((user) => user.role !== 'JUDGE' && user.role !== 'VIEWER')),
      shareReplay(1)
    );
  }

  fileProvider = () => (this.fetchData()?.name ? this.fetchData().name : 'team-download.json');

  downloadProvider = () => this.asJson();

  /**
   * Adds the selected user to the list of users.
   *
   * @param event @{MatAutocompleteSelectedEvent}
   */
  public addUser(event: MatAutocompleteSelectedEvent): void {
    this.form.get('users').value.push(event.option.value);
    this.form.get('userInput').setValue(null);
  }

  /**
   * Removes the selected user from the list of users.
   *
   * @param user The selected user.
   */
  public removeUser(user: ApiUser): void {
    const index = this.form.get('users').value.indexOf(user);
    if (index >= 0) {
      this.form.get('users').value.splice(index, 1);
    }
  }

  public sortUsersByName = (userA: ApiUser, userB: ApiUser) => userA.username.localeCompare(userB.username);

  /**
   * Returns the user for the given user id or null
   *
   * @param id User ID of the desired user.
   */
  public userForId(id: string): Observable<ApiUser> {
    return this.availableUsers.pipe(map((users) => users.find((u) => u.id === id)));
  }

  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(): string {
    if (this.form.get('logoData').value != null) {
      return this.form.get('logoData').value;
    } else if(this.team){
      return this.config.resolveApiUrl(`/template/logo/${this.team.id}`);
    } else {
      return "";
    }
  }

  /**
   * Saves all changes in the dialog and closes it.
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
      id: this.form.get('id').value,
      name: this.form.get('name').value,
      color: this.form.get('color').value,
      logoData: this.form.get('logoData').value,
      users: this.form.get('users').value,
    } as ApiTeam;
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

  /**
   * Called by the color picker when the selected color changes.
   *
   * @param color New color value (hex RGB).
   */
  public onColorChange(color: string) {
    this.form.get('color').setValue(color);
  }
}
