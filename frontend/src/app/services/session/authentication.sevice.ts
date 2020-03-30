import {Inject, Injectable} from '@angular/core';
import {LoginRequest, UserService} from '../../../../openapi';
import {flatMap, tap} from 'rxjs/operators';
import {SessionService} from './session.service';

/**
 * This service class is used to facilitate login and logout thorugh the UserService API.
 */
@Injectable()
export class AuthenticationService {
    constructor(
        @Inject(UserService) private userService: UserService,
        @Inject(SessionService) private sessionService) {
    }

    /**
     * Tries to login a user with the given username and password.
     *
     * @param user The username.
     * @param pass The password.
     */
    public login(user: string, pass: string) {
        return this.userService.postApiLogin({username: user, password: pass } as LoginRequest).pipe(
            flatMap(() => this.userService.getApiUserInfo()),
            tap(data => {
                this.sessionService.start(data);
            })
        );
    }

    /**
     * Tries to logout the current user.
     */
    public logout() {
        return this.userService.getApiLogout().pipe(
            tap(() => {
                if (!this.sessionService.isLoggedIn()) {
                    console.log(`User '${this.sessionService.getUsername()}' was logged out.`);
                    this.sessionService.end();
                } else {
                    console.log(`Nobody is logged in.`);
                }
            })
        );
    }
}
