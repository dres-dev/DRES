import {Inject, Injectable} from '@angular/core';
import {DefaultService, LoginRequest, UserDetails, UserService} from '../../../../openapi';
import {flatMap, tap} from 'rxjs/operators';
import RoleEnum = UserDetails.RoleEnum;


@Injectable()
export class SessionService {

    /** UserDetails created during login. */
    private userDetails: UserDetails = null

    constructor(
        @Inject(DefaultService) private defaultService: DefaultService,
        @Inject(UserService) private userService: UserService
    ) {}

    /**
     * Tries to login a user with the given username and password.
     *
     * @param user The username.
     * @param pass The password.
     */
    public login(user: string, pass: string) {
        return this.defaultService.postApiLogin({username: user, password: pass } as LoginRequest, 'response').pipe(
            flatMap(data => this.userService.getApiUserInfo()),
            tap(data => {
                this.userDetails = data;
                console.log(`Successfully logged in as '${this.userDetails.username['name']}'.`);
            })
        );
    }

    /**
     * Tries to logout the current user.
     */
    public logout() {
        return this.defaultService.getApiLogout().pipe(
            tap(data => {
                console.log(`User '${this.userDetails.username['name']}' was logged out.`);
                this.userDetails = null;
            })
        );
    }

    /**
     * Returns the curren login state.
     */
    public isLoggedIn(): boolean {
        return this.userDetails != null;
    }

    /**
     * Returns the username of the current user.
     */
    public getUsername(): string {
        return this.userDetails?.username['name'];
    }

    /**
     * Returns the role of the current user.
     */
    public getRole(): RoleEnum {
        return this.userDetails?.role;
    }
}
