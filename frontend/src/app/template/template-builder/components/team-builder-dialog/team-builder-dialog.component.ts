import { Component, ElementRef, Inject, ViewChild } from "@angular/core";
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { Observable, startWith } from "rxjs";
import { ApiTeam, ApiUser, UserService } from "../../../../../../openapi";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { AppConfig } from "../../../../app.config";
import { map, shareReplay, tap } from "rxjs/operators";
import { MatAutocompleteSelectedEvent } from "@angular/material/autocomplete";
import { COMMA, ENTER } from "@angular/cdk/keycodes";
import { MatChipInput, MatChipInputEvent } from "@angular/material/chips";
import { TemplateBuilderService } from "../../template-builder.service";
import { CdkDragDrop, moveItemInArray, transferArrayItem } from "@angular/cdk/drag-drop";
import { SearchBoxComponent } from "../../../../shared/search-box/search-box.component";

@Component({
  selector: 'app-team-builder-dialog',
  templateUrl: './team-builder-dialog.component.html',
  styleUrls: ['./team-builder-dialog.component.scss']
})
export class TeamBuilderDialogComponent {
  form: FormGroup;
  separatorKeyCodes: number[] = [ENTER, COMMA];
  @ViewChild('userInput') userInput: ElementRef<HTMLInputElement>
  @ViewChild('memberFilter') memberFilter: SearchBoxComponent
  @ViewChild('userFilter') userFilter: SearchBoxComponent

  logoName = '';
  users: ApiUser[];
  memberFilterText: string;
  availableFilterText: string;
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
    private dialogRef: MatDialogRef<TeamBuilderDialogComponent>,
    private userService: UserService,
    private builder: TemplateBuilderService,
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
    this.filterUsers()
    this.availableUsers = this.form.get('userInput').valueChanges.pipe(
      startWith(''),
      map(value => this.filterAvailableUsers(value || ''))
    );
  }

  fileProvider = () => (this.fetchData()?.name ? this.fetchData().name : 'team-download.json');

  downloadProvider = () => this.asJson();


  /**
   * Selected user gets added to the list of users
   */
  public selectedUser(event: MatAutocompleteSelectedEvent){
    this.form.get('users').value.push(event.option.value);
    this.form.get('userInput').setValue(null,{emit: false});
    this.userInput.nativeElement.value = '';
  }

  public filterUsers() {
    this.userService.getApiV2UserList().subscribe(value => {
      const roles = value
        .filter(u =>  u.role === "PARTICIPANT" || u.role === "ADMIN");
      this.users = roles.filter(u => {
        return !this.builder.isUserInTeam(u);
      });
    });
  }

  public drop(event: CdkDragDrop<ApiUser[]>){
    let prevList = event.previousContainer.data;
    let newList = event.container.data;
    if(event.previousContainer === event.container){
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex)
    }else{
      if(event.previousContainer.id === "memberList"){
        prevList = this.form.get('users').value;
        newList = this.users;
      }else{
        newList = this.form.get('users').value;
        prevList = this.users;
      }
      let prevIdx = prevList.indexOf(event.previousContainer.data[event.previousIndex])
      let currIdx = newList.indexOf(event.container.data[event.currentIndex])
      transferArrayItem(prevList, newList, prevIdx, currIdx)
    }
    this.memberFilter?.clear()
    this.userFilter?.clear();
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

  onMemberFilterChanged(filter: string){
    console.log(`MEMBER: was: ${this.memberFilterText} is: ${filter}`)
    this.memberFilterText = filter;
  }

  onAvailalbeFilterChanged(filter: string){
    console.log(`AVAIL: was: ${this.availableFilterText} is: ${filter}`)
    this.availableFilterText= filter;
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

  /**
   * Filters available users for the chip input autocomplete.
   * Teammembers (i.e. users that are part of the team's userlist) are excluded
   * @param value
   * @private
   */
  private filterAvailableUsers(value: string | ApiUser): ApiUser[] {
    let users : ApiUser[];
    if(! (typeof value === 'string')){
      users = this.users;
    }else {
      if (value) {
        const filterValue = (value as string).toLowerCase();

        users = this.users?.filter(user => user.username.toLowerCase().includes(filterValue));
      } else {
        users = this.users;
      }
    }
    /* always exclude members */
    return users?.filter(user => !this.form.get('users').value.map(u => u.username).includes(user.username))
  }
}
