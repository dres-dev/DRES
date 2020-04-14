import {Inject, Injectable} from '@angular/core';
import {LoginRequest, UserDetails, UserRequest, UserService} from '../../../../openapi';
import {filter, flatMap, map, tap} from 'rxjs/operators';
import {BehaviorSubject, Observable} from 'rxjs';
import RoleEnum = UserRequest.RoleEnum;

/**
 * This service class is used to facilitate login and logout thorugh the UserService API.
 */
@Injectable({
    providedIn: 'root'
})
export class AuthenticationService {
    /** UserDetails created during login. */
    private userDetails: BehaviorSubject<UserDetails> = new BehaviorSubject<UserDetails>(null);

    /**
     * Constructor
     */
    constructor(@Inject(UserService) private userService: UserService) {}

    /**
     * Tries to login a user with the given username and password. Returns an Observable!
     *
     * @param user The username.
     * @param pass The password.
     */
    public login(user: string, pass: string) {
        return this.userService.postApiLogin({username: user, password: pass} as LoginRequest).pipe(
            flatMap(() => this.userService.getApiUserInfo()),
            tap(data => {
                this.userDetails.next(data);
                console.log(`Successfully logged in '${this.userDetails.value.username}'.`);
            })
        );
    }

    /**
     * Tries to update a user's profile information. Returns an Observable!
     *
     * @param user The UserRequest object to update the profile with.
     */
    public updateUser(user: UserRequest) {
        return this.user.pipe(
            flatMap(u => this.userService.patchApiUserWithId(u.id, user)),
            tap(u => this.userDetails.next(u))
        );
    }

    /**
     * Tries to logout the current user. Returns an Observable!
     */
    public logout() {
        return this.userDetails.pipe(
            filter(u => u != null),
            flatMap(() => this.userService.getApiLogout()),
            tap(() => {
                this.userDetails.next(null);
                console.log(`User was logged out.`);
            })
        );
    }

    /**
     * Returns the current login state as Observable.
     */
    get isLoggedIn(): Observable<boolean> {
        return this.userDetails.pipe(map(u => u != null));
    }

    /**
     * Returns the username of the current user as Observable.
     */
    get user(): Observable<UserDetails> {
        return this.userDetails.asObservable();
    }

    /**
     * Returns the role of the current user as Observable.
     */
    get role(): Observable<RoleEnum> {
        return this.userDetails.pipe(map(u => u?.role));
    }
}
