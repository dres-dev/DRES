import { Component, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, ActivationEnd, Router } from '@angular/router';
import { AuthenticationService } from './services/session/authentication.sevice';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, filter, map, switchMap } from 'rxjs/operators';
import { EMPTY, Observable, Subscription, timer } from 'rxjs';
import { AppConfig } from './app.config';
import { ApiRole, ApiUser, StatusService, SystemStatus } from '../../openapi';
import { MatDialog } from '@angular/material/dialog';
import { ServerInfoComponent } from './shared/server-info/server-info.component';
import { LogService } from './services/logging/log.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false,
})
export class AppComponent implements OnDestroy {
  private readonly LOGGER = inject<LogService>(LogService);

  title = 'dres-frontend';

  user: Observable<ApiUser>;
  isAdmin: Observable<boolean>;
  loggedIn: Observable<boolean>;
  canJudge: Observable<boolean>;
  noUi: Observable<boolean>;
  darkMode = false;
  systemStatuses: SystemStatus[] = [];
  private readonly systemStatusSub: Subscription;
  private adminPollingSub: Subscription;

  get hasSystemWarnings(): boolean {
    return this.systemStatuses.some((status) => status.level === 'WARNING' || status.level === 'ERROR');
  }

  constructor(
    private authenticationService: AuthenticationService,
    private router: Router,
    private snackBar: MatSnackBar,
    public config: AppConfig,
    private dialog: MatDialog,
    private activeRoute: ActivatedRoute,
    private statusService: StatusService
  ) {
    this.noUi = this.router.events.pipe(
      filter((e) => e instanceof ActivationEnd),
      map((e) => {
        return e instanceof ActivationEnd ? { path: e.snapshot.params, query: e.snapshot.queryParams } : {};
      }),
      map((dto) => {
        const p = dto?.path ?? {};
        if (p['runId']) {
          const runIdPathParams = p['runId'].split(';'); // manual handling of matrix params
          for (const pram of runIdPathParams) {
            // either as flag ;noUi or as value noUi=true. when having noUi=false this will negate it and the ui will be shown
            if (pram.startsWith('noUi') && !pram.endsWith('false')) {
              return true;
            }
          }
          return false;
        } else {
          // check query parameters
          const p = dto?.query ?? {};
          return p['noUi'] ?? false;
        }
      })
    );

    this.user = this.authenticationService.user;
    this.loggedIn = this.authenticationService.isLoggedIn;
    this.isAdmin = this.authenticationService.user.pipe(map((u) => u?.role === ApiRole.ADMIN));
    this.canJudge = this.authenticationService.user.pipe(map((u) => u?.role === ApiRole.ADMIN || u?.role === ApiRole.JUDGE));

    this.systemStatusSub = this.authenticationService.user.subscribe((user) => {
      this.systemStatuses = [];
      this.adminPollingSub?.unsubscribe();
      if (user?.role === ApiRole.ADMIN) {
        this.adminPollingSub = timer(0, 60_000)
          .pipe(
            switchMap(() => this.statusService.getApiV2StatusInfo().pipe(catchError(() => EMPTY))),
            map((info) => info.systemStatuses ?? [])
          )
          .subscribe((statuses) => {
            this.systemStatuses = statuses;
          });
      }
    });

    this.darkMode = localStorage.getItem('darkMode') !== 'false';
    this.applyDarkMode();
  }

  ngOnDestroy(): void {
    this.systemStatusSub?.unsubscribe();
    this.adminPollingSub?.unsubscribe();
  }

  public toggleDarkMode() {
    this.darkMode = !this.darkMode;
    localStorage.setItem('darkMode', String(this.darkMode));
    this.applyDarkMode();
  }

  private applyDarkMode() {
    document.documentElement.classList.toggle('dark-theme', this.darkMode);
  }

  public toggleMute() {
    this.config.config.effects.mute = !this.config.config.effects.mute;
    this.LOGGER.fatal('Mute state', this.config.config.effects.mute);
  }

  public logout() {
    this.authenticationService.logout().subscribe(() => {
      this.snackBar.open(`Logout successful!`, null, { duration: 5000 });
      this.router.navigate(['/']).then((r) => {});
    });
  }

  public profile() {
    this.router.navigate(['/user']).then((r) => {});
  }

  public openInfoDialog() {
    this.dialog.open(ServerInfoComponent, { width: '600px' });
  }
}
