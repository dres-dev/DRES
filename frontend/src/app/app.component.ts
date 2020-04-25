import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {AuthenticationService} from './services/session/authentication.sevice';
import {UserDetails} from '../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {map} from 'rxjs/operators';
import RoleEnum = UserDetails.RoleEnum;
import {Observable} from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'dres-frontend';



  user: Observable<UserDetails>;
  isAdmin: Observable<boolean>;
  loggedIn: Observable<boolean>;
  canJudge: Observable<boolean>;

  constructor(private authenticationService: AuthenticationService, private router: Router, private snackBar: MatSnackBar) {
    this.user = this.authenticationService.user;
    this.loggedIn = this.authenticationService.isLoggedIn;
    this.isAdmin = this.authenticationService.user.pipe(map(u => u?.role === RoleEnum.ADMIN));
    this.canJudge = this.authenticationService.user.pipe(map(u => u?.role === RoleEnum.ADMIN || u?.role === RoleEnum.JUDGE));
  }

  public logout() {
    this.authenticationService.logout().subscribe(() => {
      this.snackBar.open(`Logout Successful!`, null, {duration: 5000});
      this.router.navigate(['/']);
    });
  }

  profile() {
    this.router.navigate(['/user']);
  }
}
