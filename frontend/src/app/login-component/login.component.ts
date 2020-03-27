import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {SessionService} from '../services/session/session.service';
import {Router} from '@angular/router';

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

  constructor(private sessionService: SessionService, private router: Router) {
  }

  ngOnInit(): void {
  }

  public submit() {
    if (this.form.valid) {
      this.sessionService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe(() => {
        this.router.navigateByUrl('/');
      },
      (e) => {
        console.log("Error")
      });
    }
  }
}
