import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {map, withLatestFrom} from 'rxjs/operators';
import {AuthenticationService} from './authentication.sevice';

@Injectable({
    providedIn: 'root'
})
export class AuthenticationGuard implements CanActivate {
    constructor(private router: Router, private authenticationService: AuthenticationService) {
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        return this.authenticationService.isLoggedIn.pipe(
            withLatestFrom(this.authenticationService.role),
            map(([loggedIn, role]) => {
                if (!loggedIn) {
                    this.router.navigate(['/login'], {queryParams: {returnUrl: state.url}});
                    return false;
                }
                if (route.data.roles && route.data.roles.indexOf(role) === -1) {
                    this.router.navigate(['/']);
                    return false;
                }
                return true;
            })
        );
    }
}
