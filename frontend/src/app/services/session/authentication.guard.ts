import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {SessionService} from './session.service';
import {map, withLatestFrom} from 'rxjs/operators';

@Injectable()
export class AuthenticationGuard implements CanActivate {
  constructor(private router: Router, private sessionService: SessionService) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    return this.sessionService.isLoggedIn().pipe(
        withLatestFrom(this.sessionService.getRole()),
        map(([loggedIn, role]) => {
          console.log(`${loggedIn} ${role}`)
          if (!loggedIn) {
            this.router.navigate(['/login'], {queryParams: {returnUrl: state.url}});
            return false;
          }
          if (route.data.roles && route.data.roles.indexOf(role) === -1) {
            this.router.navigate(['/']);
            return false;
          }
          // authorised so return true
          return true;
        }));

  }
}
