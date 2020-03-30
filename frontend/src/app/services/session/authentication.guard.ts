import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {SessionService} from './session.service';

@Injectable()
export class AuthenticationGuard implements CanActivate {
    constructor(private router: Router, private sessionService: SessionService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        if (!this.sessionService.isLoggedIn()) {
            this.router.navigate(['/login'], { queryParams: { returnUrl: state.url }});
            return false;
        }

        if (route.data.roles && route.data.roles.indexOf(this.sessionService.getRole()) === -1) {
            this.router.navigate(['/']);
            return false;
        }

        // authorised so return true
        return true;
    }
}
