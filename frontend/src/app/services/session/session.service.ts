import {Injectable} from '@angular/core';
import {UserDetails} from '../../../../openapi';
import RoleEnum = UserDetails.RoleEnum;

/**
 * This service class is used to keep track of the current session and the current user. To avoid cyclic injections with the HTTP interceptor,
 * this class is isolated from the service that actually performs the HTTP requests through the Open API endpoint.
 */
@Injectable()
export class SessionService {

    /** UserDetails created during login. */
    private userDetails: UserDetails = null;

    constructor() {}

    /**
     * Starts a new session with the given user.
     *
     * @param user The user to start the session with.
     */
    public start(user: UserDetails) {
        if (this.userDetails == null) {
            this.userDetails = user;
            console.log(`Successfully logged in '${this.userDetails.username}'.`);
        } else {
            console.log(`The user '${this.userDetails.username}' is already logged in. Logout before startin new session.`);
        }
    }

    /**
     * Ends the current session.
     */
    public end() {
        if (this.userDetails != null) {
            console.log(`Successfully logged out '${this.userDetails.username}'.`);
            this.userDetails = null;
        } else {
            console.log(`Session cannot be ended. No user is currently logged in.`);
        }
    }

    /**
     * Returns the current login state.
     */
    public isLoggedIn(): boolean {
        return this.userDetails != null;
    }

    /**
     * Returns the username of the current user.
     */
    public getUsername(): string {
        return this.userDetails?.username;
    }

    /**
     * Returns the role of the current user.
     */
    public getRole(): RoleEnum {
        return this.userDetails?.role;
    }
}
