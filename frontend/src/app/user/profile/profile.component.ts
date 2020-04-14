import {Component, OnDestroy, OnInit} from '@angular/core';
import {SessionService} from '../../services/session/session.service';
import {SessionId, UserDetails, UserRequest, UserService} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable, Subscription} from 'rxjs';
import {FormControl, FormGroup} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';
import {first, flatMap} from 'rxjs/operators';
import {AuthenticationService} from '../../services/session/authentication.sevice';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {

  user: Observable<UserDetails>;
  sessionId: Observable<SessionId>;

  editing = false;

  private userSub: Subscription;

  form: FormGroup = new FormGroup({
    username: new FormControl({value: '', disabled: !this.editing}),
    password: new FormControl({value: '', disabled: !this.editing})
  });

  constructor(
      public authenticationService: AuthenticationService,
      public sessionService: SessionService,
      private router: Router,
      private route: ActivatedRoute,
      public userService: UserService,
      private snackBar: MatSnackBar
  ) {
  }

  ngOnInit(): void {
    this.user = this.sessionService.currentUser;
    this.sessionId = this.userService.getApiUserSession();

    this.userSub = this.user.subscribe(u => {
      this.form.patchValue({username: u.username});
    });
  }

  ngOnDestroy(): void {
    this.userSub.unsubscribe();
  }


  public submit() {
    console.log(`Submitting u=${this.form.controls.username.value} and p=${this.form.controls.password.value}`);
    if (this.form.valid) {
      const usr = {username: this.form.controls.username.value} as UserRequest;
      if (this.form.controls.password.value !== '') {
        usr.password = this.form.controls.password.value;
      }
      this.user.pipe(first(), flatMap(u => this.userService.patchApiUserWithId(u.id, usr)))
      .subscribe((r: UserDetails) => {
            this.snackBar.open(`Save successful!`, null, {duration: 5000});
            this.toggleEdit();
            this.authenticationService.refresh();
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
