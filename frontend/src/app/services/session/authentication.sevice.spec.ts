import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiRole, ApiUser, UserService } from '../../../../openapi';
import { AuthenticationService } from './authentication.sevice';

describe('AuthenticationService route guard', () => {
  function createService(role: ApiRole | null) {
    const userService = jasmine.createSpyObj<UserService>('UserService', ['getApiV2User']);
    if (role == null) {
      userService.getApiV2User.and.returnValue(throwError(() => new Error('not logged in')) as unknown as Observable<HttpEvent<ApiUser>>);
    } else {
      userService.getApiV2User.and.returnValue(of({ role } as ApiUser) as unknown as Observable<HttpEvent<ApiUser>>);
    }
    const router = jasmine.createSpyObj<Router>('Router', ['parseUrl']);
    router.parseUrl.and.callFake((url: string) => url as unknown as UrlTree);
    return { service: new AuthenticationService(userService, router), router };
  }

  it('allows a role in the guard allowlist', (done) => {
    const { service } = createService(ApiRole.ADMIN);
    service.canActivate([ApiRole.ADMIN], {} as ActivatedRouteSnapshot, { url: '/admin' } as RouterStateSnapshot).subscribe((result) => {
      expect(result).toBeTrue();
      done();
    });
  });

  it('redirects a logged-in user with a disallowed role to forbidden', (done) => {
    const { service, router } = createService(ApiRole.PARTICIPANT);
    service.canActivate([ApiRole.ADMIN], {} as ActivatedRouteSnapshot, { url: '/admin' } as RouterStateSnapshot).subscribe((result) => {
      expect(router.parseUrl).toHaveBeenCalledWith('/forbidden');
      expect(result).toBe('/forbidden' as unknown as UrlTree);
      done();
    });
  });

  it('redirects a logged-out user to login with the requested route', (done) => {
    const { service, router } = createService(null);
    service.canActivate([ApiRole.ADMIN], {} as ActivatedRouteSnapshot, { url: '/admin' } as RouterStateSnapshot).subscribe((result) => {
      expect(router.parseUrl).toHaveBeenCalledWith('/login?returnUrl=/admin');
      expect(result).toBe('/login?returnUrl=/admin' as unknown as UrlTree);
      done();
    });
  });
});
