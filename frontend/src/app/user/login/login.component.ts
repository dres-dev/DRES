import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AuthenticationService} from '../../services/session/authentication.sevice';
import {filter, first} from 'rxjs/operators';

@Component({
  selector: 'app-login-component',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  form: FormGroup = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  private returnUrl = '';

  constructor(
      private authenticationService: AuthenticationService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar
  ) {

  }

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams.returnUrl || '';
    this.authenticationService.isLoggedIn.pipe(
        first(),
        filter(b => b)
    ).subscribe(b => {
      this.router.navigate(['']);
    });
  }

  public submit() {
    if (this.form.valid) {
      this.authenticationService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe((r) => {
            this.snackBar.open(`Login successful!`, null, {duration: 5000});
            this.router.navigate([this.returnUrl]);
          },
          (error) => {
            this.snackBar.open(`Login failed: ${error.error.description}!`, null, {duration: 5000});
          });
    }
  }
}
