import {Inject, Injectable} from '@angular/core';
import {UserDetails, UserService} from '../../../../openapi';
import {BehaviorSubject, Observable} from 'rxjs';
import {first, map} from 'rxjs/operators';
import RoleEnum = UserDetails.RoleEnum;

/**
 * This service class is used to keep track of the current session and the current user. To avoid cyclic injections with the HTTP interceptor,
 * this class is isolated from the service that actually performs the HTTP requests through the Open API endpoint.
 */
@Injectable()
export class SessionService {


  /** UserDetails created during login. */
  private userDetails: BehaviorSubject<UserDetails> = new BehaviorSubject<UserDetails>(null);

  constructor() {
  }

  /**
   * Starts a new session with the given user.
   *
   * @param user The user to start the session with.
   */
  public start(user: UserDetails) {
    if (this.userDetails.value == null) {
      this.userDetails.next(user);
      console.log(`Successfully logged in '${this.userDetails.value.username}'.`);
    } else {
      console.log(`The user '${this.userDetails.value.username}' is already logged in. Logout before startin new session.`);
    }
  }

  get currentUser() {
    return this.userDetails.asObservable();
  }

  public refresh(userDetails: UserDetails) {
    this.userDetails.next(userDetails);

  }

  /**
   * Ends the current session.
   */
  public end() {
    if (this.userDetails.value != null) {
      console.log(`Successfully logged out '${this.userDetails.value.username}'.`);
      this.userDetails.next(null);
    } else {
      console.log(`Session cannot be ended. No user is currently logged in.`);
    }
  }

  /**
   * Returns the current login state.
   */
  public isLoggedIn(): Observable<boolean> {
    return this.userDetails.pipe(map(u => u != null));
  }

  /**
   * Returns the username of the current user.
   */
  public getUsername(): Observable<string> {
    return this.userDetails.pipe(map(u => u?.username));
  }

  /**
   * Returns the role of the current user.
   */
  public getRole(): Observable<RoleEnum> {
    return this.userDetails.pipe(map(u => u?.role));
  }
}
