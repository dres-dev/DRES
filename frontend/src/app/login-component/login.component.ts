import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {SessionService} from '../services/session/session.service';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AuthenticationService} from '../services/session/authentication.sevice';

@Component({
  selector: 'app-login-component',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, AfterViewInit {
  form: FormGroup = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  private returnUrl = '';

  constructor(
      private authenticationService: AuthenticationService,
      private sessionService: SessionService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar
  ) {

  }

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';
  }

  ngAfterViewInit(): void {
    if (this.sessionService.isLoggedIn()) {
      this.router.navigate(['']);
    }
  }

  public submit() {
    if (this.form.valid) {
      this.authenticationService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe((r) => {
        this.snackBar.open(`Login successful!`, null, { duration: 5000});
        this.router.navigate([this.returnUrl]);
      },
      (error) => {
        this.snackBar.open(`Login failed: ${error.error.description}!`, null, { duration: 5000});
      });
    }
  }
}
