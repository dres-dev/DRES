import {Inject, Injectable} from '@angular/core';
import {catchError, filter, map, shareReplay, tap, withLatestFrom} from 'rxjs/operators';
import {BehaviorSubject, mergeMap, Observable, of, Subscription} from 'rxjs';
import {ApiRole, ApiUser, LoginRequest, ApiUserRequest, UserService} from '../../../../openapi';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree} from "@angular/router";

/**
 * This service class is used to facilitate login and logout through the UserService API.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {

  /** A {@link BehaviorSubject} that captures the current login-state. */
  private _loggedIn: BehaviorSubject<boolean> = new BehaviorSubject(false)

  /**
   * Constructor
   */
  constructor(@Inject(UserService) private userService: UserService, private router: Router) {
    this.userService.getApiV2User().subscribe(
      () => this._loggedIn.next(true),
      () => this._loggedIn.next(false)
    )
  }

  /**
   * Tries to login a user with the given username and password. Returns an Observable!
   *
   * @param user The username.
   * @param pass The password.
   */
  public login(user: string, pass: string) {
    return this.userService.postApiV2Login({ username: user, password: pass } as LoginRequest).pipe(
      mergeMap(() => this.userService.getApiV2User()),
      tap((data) => {
        this._loggedIn.next(true);
        console.log(`Successfully logged in as '${data.username}'.`)
      })
    );
  }

  /**
   * Tries to logout the current user. Returns an Observable!
   */
  public logout() {
    return this.userService.getApiV2Logout().pipe(
        catchError((e) => of(null)),
        tap(() => {
          this._loggedIn.next(false);
          console.log(`User was logged out.`)
        })
    );
  }

  /**
   * Tries to update a user's profile information. Returns an Observable!
   *
   * @param user The UserRequest object to update the profile with.
   */
  public updateUser(user: ApiUserRequest) {
    return this.user.pipe(
      mergeMap((u: ApiUser) => this.userService.patchApiV2UserByUserId(u.id, user))
    );
  }

  /**
   * Returns the current login state as Observable.
   *
   * A call to this method always results in an API call to make sure,
   * that the user is still logged in.
   */
  get isLoggedIn(): Observable<boolean> {
    return this._loggedIn.asObservable()
  }

  /**
   * Returns the currently logged in {@link ApiUser} as Observable.
   */
  get user(): Observable<ApiUser |null> {
    return this.isLoggedIn.pipe(
        mergeMap(loggedIn=> {
        if (loggedIn) {
          return this.userService.getApiV2User()
        } else {
          return of(null)
        }
      }))
  }

  /**
   * Returns the {@link ApiRole} of the current user as Observable.
   */
  get role(): Observable<ApiRole | null> {
    return this.user.pipe(map((u) => u?.role))
  }

  /**
   * This function is used to check if a particular route can be activated. It is
   * used by the {@link CanActivateFn} defined in guards.ts
   *
   * @param rolesAllows The list of {@link ApiRole}s allowed
   * @param route The {@link ActivatedRouteSnapshot}
   * @param state The {@link RouterStateSnapshot}
   */
  public canActivate(rolesAllows: Array<ApiRole>, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    return this.role.pipe(
        map((role) => {
          if (!role) {
            return this.router.parseUrl(`/login?returnUrl=${state.url}`)
          } else if (route.data.roles && route.data.roles.indexOf(role) === -1) {
            return this.router.parseUrl('/forbidden');
          } else {
            return true;
          }
        })
    );
  }
}
