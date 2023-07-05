import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthenticationService } from '../../services/session/authentication.sevice';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-login-component',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit, OnDestroy {
  form: UntypedFormGroup = new UntypedFormGroup({
    username: new UntypedFormControl(''),
    password: new UntypedFormControl(''),
  });

  private returnUrl = '/evaluation/list';
  private authenticationServiceSubscription: Subscription;

  constructor(
    private authenticationService: AuthenticationService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams.returnUrl || '/evaluation/list';
    this.authenticationServiceSubscription = this.authenticationService.isLoggedIn.subscribe((b) => {
      if (b) this.router.navigate([this.returnUrl]).then(r => {})
    });
  }

  ngOnDestroy(): void {
    this.authenticationServiceSubscription.unsubscribe();
    this.authenticationServiceSubscription = null;
  }

  public submit() {
    if (this.form.valid) {
      this.authenticationService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe(
        (r) => this.router.navigateByUrl(this.returnUrl).then(r => this.snackBar.open(`Login successful!`, null, { duration: 5000 })),
        (err) => this.snackBar.open(`Login failed due to error: ${err?.error?.description}!`, null, { duration: 5000 })
      );
    }
  }
}
