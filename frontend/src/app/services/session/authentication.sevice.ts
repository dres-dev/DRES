import {Inject, Injectable} from '@angular/core';
import {catchError, flatMap, map, tap, withLatestFrom} from 'rxjs/operators';
import {Observable, of} from 'rxjs';
import {ApiRole, ApiUser, LoginRequest, UserRequest, UserService} from '../../../../openapi';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree} from "@angular/router";

/**
 * This service class is used to facilitate login and logout through the UserService API.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {

  /**
   * Constructor
   */
  constructor(@Inject(UserService) private userService: UserService, private router: Router) {}

  /**
   * Tries to login a user with the given username and password. Returns an Observable!
   *
   * @param user The username.
   * @param pass The password.
   */
  public login(user: string, pass: string) {
    return this.userService.postApiV2Login({ username: user, password: pass } as LoginRequest).pipe(
      flatMap(() => this.userService.getApiV2User()),
      tap((data) => console.log(`Successfully logged in as '${data.username}'.`))
    );
  }

  /**
   * Tries to update a user's profile information. Returns an Observable!
   *
   * @param user The UserRequest object to update the profile with.
   */
  public updateUser(user: UserRequest) {
    return this.user.pipe(
      flatMap((u: ApiUser) => this.userService.patchApiV2UserByUserId(u.id, user))
    );
  }

  /**
   * Tries to logout the current user. Returns an Observable!
   */
  public logout() {
    return this.userService.getApiV2Logout().pipe(
      catchError((e) => of(null)),
      tap(() => console.log(`User was logged out.`))
    );
  }

  /**
   * Returns the current login state as Observable.
   *
   * A call to this method always results in an API call to make sure,
   * that the user is still logged in.
   */
  get isLoggedIn(): Observable<boolean> {
    return this.userService.getApiV2User().pipe(
      map((u) => u != null),
      catchError((e) => of(false))
    );
  }

  /**
   * Returns the username of the current user as Observable.
   */
  get user(): Observable<ApiUser> {
    return this.userService.getApiV2User();
  }

  /**
   * Returns the role of the current user as Observable.
   */
  get role(): Observable<ApiRole> {
    return this.userService.getApiV2User().pipe(map((u) => u?.role));
  }

  /**
   * This function is used to check if a particular route can be activated. It is
   * used by the {@link CanActivateFn} defined in guards.ts
   *
   * @param rolesAllows The list of {@link ApiRole}s allowed
   * @param route The {@link ActivatedRouteSnapshot}
   * @param state The {@link RouterStateSnapshot}
   */
  public canActivate(rolesAllows: Array<ApiRole>, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean | UrlTree> {
    return this.isLoggedIn.pipe(
        withLatestFrom(this.role),
        map(([loggedIn, role]) => {
          if (!loggedIn) {
            return this.router.parseUrl(`/login?returnUrl=${state.url}`)
          }
          if (route.data.roles && route.data.roles.indexOf(role) === -1) {
            return this.router.parseUrl('/forbidden');
          }
          return true;
        })
    ).toPromise();
  }
}
