import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { first } from 'rxjs/operators';
import { AuthenticationService } from '../../services/session/authentication.sevice';
import {ApiUser, ApiUserRequest, UserService} from '../../../../openapi';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent implements OnInit, OnDestroy {
  user: Observable<ApiUser>;
  sessionId: Observable<string>;
  loggedIn: Observable<boolean>;

  editing = false;

  private userSub: Subscription;

  form: UntypedFormGroup = new UntypedFormGroup({
    username: new UntypedFormControl({ value: '', disabled: !this.editing }),
    password: new UntypedFormControl({ value: '', disabled: !this.editing }),
  });

  constructor(
    public authenticationService: AuthenticationService,
    private router: Router,
    private route: ActivatedRoute,
    public userService: UserService,
    private snackBar: MatSnackBar
  ) {
    this.user = this.authenticationService.user;
    this.loggedIn = this.authenticationService.isLoggedIn;
    this.sessionId = this.userService.getApiV2UserSession();
  }

  ngOnInit(): void {
    this.userSub = this.user.subscribe((u) => {
      this.form.patchValue({ username: u.username });
    });
  }

  ngOnDestroy(): void {
    this.userSub.unsubscribe();
    this.userSub = null;
  }

  public submit() {
    console.log(`Submitting u=${this.form.controls.username.value} and p=${this.form.controls.password.value}`);
    if (this.form.valid) {
      const usr = { username: this.form.controls.username.value } as ApiUserRequest;
      if (this.form.controls.password.value !== '') {
        usr.password = this.form.controls.password.value;
      }
      this.authenticationService
        .updateUser(usr)
        .pipe(first())
        .subscribe({
          next: (r: ApiUser) => {
            this.snackBar.open(`Save successful!`, null, { duration: 5000 });
            this.toggleEdit();
          },
          error: (error) => {
            this.snackBar.open(`Save failed: ${error.error.description}!`, null, { duration: 5000 });
          }
        });
    }
  }

  public reset() {
    this.toggleEdit();
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
