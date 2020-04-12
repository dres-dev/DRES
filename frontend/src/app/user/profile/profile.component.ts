import {Component, OnInit} from '@angular/core';
import {SessionService} from '../../services/session/session.service';
import {SessionId, UserDetails, UserRequest, UserService} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {FormControl, FormGroup} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  user: Observable<UserDetails>;
  sessionId: Observable<SessionId>;

  editing = false;

  private id: number;

  form: FormGroup = new FormGroup({
    username: new FormControl({value: '', disabled: !this.editing}),
    password: new FormControl({value: '', disabled: !this.editing})
  });

  constructor(
      public sessionService: SessionService,
      private router: Router,
      private route: ActivatedRoute,
      public userService: UserService,
      private snackBar: MatSnackBar
  ) {
  }

  ngOnInit(): void {
    this.syncUser();
  }

  private syncUser(): void {
    this.user = this.userService.getApiUserInfo();
    this.sessionId = this.userService.getApiUserSession();

    this.user.subscribe((u: UserDetails) => {
      this.form.patchValue({username: u.username});
      this.id = u.id;
    });

  }

  public submit() {
    console.log(`Submitting u=${this.form.controls.username.value} and p=${this.form.controls.password.value}`);
    if (this.form.valid) {
      let usr = {username: this.form.controls.username.value} as UserRequest;
      if (this.form.controls.password.value !== '') {
        usr.password = this.form.controls.password.value;
      }
      this.userService.patchApiUserWithId(this.id, usr).subscribe((r: UserDetails) => {
            this.snackBar.open(`Save successful!`, null, {duration: 5000});
            this.toggleEdit();
            this.syncUser();
          },
          (error) => {
            this.snackBar.open(`Save failed: ${error.error.description}!`, null, {duration: 5000});
          });
    }
  }

  public reset() {
    this.toggleEdit();
    // TODO reset form components
  }

  isEditing() {
    return this.editing;
  }

  toggleEdit() {
    this.editing = !this.editing;
    if (this.editing) {
      this.form.controls.username.enable();
      this.form.controls.password.enable();
    } else {
      this.form.controls.username.disable();
      this.form.controls.password.disable();
    }
  }
}
