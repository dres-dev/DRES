import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {SessionService} from '../services/session/session.service';
import {Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';

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

  @Input() error: string | null;

  @Output() submitEM = new EventEmitter();

  constructor(
      private sessionService: SessionService,
      private router: Router,
      private snackBar: MatSnackBar
  ) {
  }

  ngOnInit(): void {}

  public submit() {
    if (this.form.valid) {
      this.sessionService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe((r) => {
        this.snackBar.open(`Login successful!`, null, { duration: 5000});
        this.router.navigateByUrl('/');
      },
      (error) => {
        this.snackBar.open(`Login failed: ${error.error.description}!`, null, { duration: 5000});
      });
    }
  }
}
