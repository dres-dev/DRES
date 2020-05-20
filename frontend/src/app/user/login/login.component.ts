import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AuthenticationService} from '../../services/session/authentication.sevice';
import {Subscription} from 'rxjs';

@Component({
    selector: 'app-login-component',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
    form: FormGroup = new FormGroup({
        username: new FormControl(''),
        password: new FormControl(''),
    });

    private returnUrl = '/';
    private authenticationServiceSubscription: Subscription;

    constructor(private authenticationService: AuthenticationService, private router: Router, private route: ActivatedRoute, private snackBar: MatSnackBar) {
    }

    ngOnInit(): void {
        this.returnUrl = this.route.snapshot.queryParams.returnUrl || '/';
        this.authenticationServiceSubscription = this.authenticationService.isLoggedIn.subscribe(b => {
          if (b) {
            this.router.navigate([this.returnUrl]);
          }
        });
    }

    ngOnDestroy(): void {
        this.authenticationServiceSubscription.unsubscribe();
        this.authenticationServiceSubscription = null;
    }

    public submit() {
        if (this.form.valid) {
            this.authenticationService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe((r) => {
                this.snackBar.open(`Login successful!`, null, {duration: 5000});
            },
            (err) => {
                if (err?.error) {
                    this.snackBar.open(`Login failed: ${err?.error?.description}!`, null, {duration: 5000});
                } else {
                    this.snackBar.open(`Login failed due to a connection issue!`, null, {duration: 5000});
                }
            });
        }
    }
}
