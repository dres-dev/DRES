import { Component, inject } from "@angular/core";
import { Router } from '@angular/router';
import { AuthenticationService } from './services/session/authentication.sevice';
import { MatSnackBar } from '@angular/material/snack-bar';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { AppConfig } from './app.config';
import {ApiRole, ApiUser} from '../../openapi';
import { MatDialog } from "@angular/material/dialog";
import { ServerInfoComponent } from "./shared/server-info/server-info.component";
import { LogService } from "./services/logging/log.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {

  private readonly LOGGER = inject<LogService>(LogService);

  title = 'dres-frontend';

  user: Observable<ApiUser>;
  isAdmin: Observable<boolean>;
  loggedIn: Observable<boolean>;
  canJudge: Observable<boolean>;

  constructor(
    private authenticationService: AuthenticationService,
    private router: Router,
    private snackBar: MatSnackBar,
    public config: AppConfig,
    private dialog: MatDialog,
  ) {
    this.user = this.authenticationService.user;
    this.loggedIn = this.authenticationService.isLoggedIn;
    this.isAdmin = this.authenticationService.user.pipe(map((u) => u?.role === ApiRole.ADMIN));
    this.canJudge = this.authenticationService.user.pipe(map((u) => u?.role === ApiRole.ADMIN || u?.role === ApiRole.JUDGE));
  }

  /**
   *
   */
  public toggleMute() {
    this.config.config.effects.mute = !this.config.config.effects.mute;
    this.LOGGER.fatal("Mute state", this.config.config.effects.mute)
  }

  public logout() {
    this.authenticationService.logout().subscribe(() => {
      this.snackBar.open(`Logout successful!`, null, { duration: 5000 });
      this.router.navigate(['/']).then(r => {});
    });
  }

  public profile() {
    this.router.navigate(['/user']).then(r => {});
  }

  public openInfoDialog(){
    this.dialog.open(ServerInfoComponent, { width: '600px' });
  }
}
