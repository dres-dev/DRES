import {ApiModule, Configuration} from '../../../openapi';
import {SessionService} from './session/session.service';
import {NgModule} from '@angular/core';
import {HTTP_INTERCEPTORS} from '@angular/common/http';
import {HttpErrorInterceptor} from './session/http-error.interceptor';
import {AuthenticationGuard} from './session/authentication.guard';
import {AuthenticationService} from './session/authentication.sevice';
import {AppConfig} from '../app.config';

@NgModule({
  imports: [ApiModule.forRoot(() => {
    return new Configuration({
      basePath: `${AppConfig.settings.endpoint.tls ? 'https://' : 'http://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}`,
      withCredentials: true
    });
  })],
  exports: [ApiModule],
  declarations: [],
  providers: [AuthenticationService, AuthenticationGuard, SessionService, {
    provide: HTTP_INTERCEPTORS,
    useClass: HttpErrorInterceptor,
    multi: true
  }]
})
export class ServicesModule {
}
