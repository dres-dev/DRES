import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RestTeam, UserDetails, UserService } from '../../../../../openapi';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import {map, shareReplay, startWith} from 'rxjs/operators';
import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import { AppConfig } from '../../../app.config';

@Component({
  selector: 'app-competition-builder-add-team-dialog',
  templateUrl: './competition-builder-team-dialog.component.html',
})
export class CompetitionBuilderTeamDialogComponent {
  form: FormGroup;
  logoName = '';
  availableUsers: Observable<UserDetails[]>;
  userInput: FormControl = new FormControl('');
  filteredUsers: Observable<UserDetails[]>;
  static colorPalette = [
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
    '#BF0039',
  ];

  constructor(
    private dialogRef: MatDialogRef<CompetitionBuilderTeamDialogComponent>,
    private userService: UserService,
    private config: AppConfig,
    @Inject(MAT_DIALOG_DATA) private team?: RestTeam
  ) {
    this.form = new FormGroup({
      name: new FormControl(team?.name, [Validators.required, Validators.minLength(3)]),
      color: new FormControl(team?.color ? team.color : CompetitionBuilderTeamDialogComponent.randomColor(), [
        Validators.required,
        Validators.minLength(7),
        Validators.maxLength(7),
      ]),
      logoId: new FormControl(team?.logoId),
      logoData: new FormControl(team?.logoData),
      users: new FormControl(team?.users != null ? team.users : []),
      userInput: this.userInput,
    });
    this.availableUsers = this.userService.getApiV1UserList().pipe(
        map((value) => {
          const filtered = value.filter((user) => user.role !== 'JUDGE' && user.role !== 'VIEWER');
          return filtered.sort((a, b) => a.username.localeCompare(b.username));
        }),
        shareReplay(1)
    );
    this.filteredUsers = combineLatest([this.userInput.valueChanges, this.availableUsers]).pipe(
      map(pair => {
            const filterValue = (typeof pair[0] === 'string') ? pair[0].toLowerCase() : '';
            return pair[1].filter(x => x.username.toLowerCase().includes(filterValue));
          }
      )
    );

  }

  /**
   * Picks a random color from the palette.
   */
  private static randomColor(): string {
    return this.colorPalette[Math.floor(Math.random() * this.colorPalette.length)];
  }

  palette = () => CompetitionBuilderTeamDialogComponent.colorPalette

  fileProvider = () => (this.fetchData()?.name ? this.fetchData().name : 'team-download.json');

  downloadProvider = () => this.asJson();

  /**
   * Adds the selected user to the list of users.
   *
   * @param event @{MatAutocompleteSelectedEvent}
   */
  public addUser(event: MatAutocompleteSelectedEvent): void {
    this.form.get('users').value.push(event.option.value.id);
    this.userInput.setValue('');
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
   * Called by the color picker when the selected color changes.
   *
   * @param color New color value (hex RGB).
   */
  public onColorChange(color: string) {
    this.form.get('color').setValue(color);
  }

  /**
   * Returns the user for the given user id or null
   *
   * @param id User ID of the desired user.
   */
  public userForId(id: string): Observable<UserDetails> {
    return this.availableUsers.pipe(map((users) => users.find((u) => u.id === id)));
  }

  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(): string {
    if (this.form.get('logoData').value != null) {
      return this.form.get('logoData').value;
    } else {
      const tmp = this.config.resolveApiUrl(`/competition/logo/${this.form.get('logoId').value}`);
      console.log('resolving url: ' + tmp);
      return tmp;
    }
  }

  public teamLogoAlt(): string {
    const alt = this.form.get('logoId').value;
    if (alt == null) {
      return 'Logo not set';
    } else {
      return alt;
    }
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
      users: this.form.get('users').value,
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
