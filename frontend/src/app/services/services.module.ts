import {ApiModule, Configuration} from '../../../openapi';
import {SessionService} from './session/session.service';
import {NgModule} from '@angular/core';
import {HTTP_INTERCEPTORS} from '@angular/common/http';
import {HttpErrorInterceptor} from './session/http-error.interceptor';
import {AuthenticationGuard} from './session/authentication.guard';
import {AuthenticationService} from './session/authentication.sevice';

@NgModule({
    imports: [ ApiModule.forRoot(() => {
        return new Configuration({
            basePath: `http://localhost:8080`,
            withCredentials: true
        });
    })],
    exports:      [ ApiModule ],
    declarations: [ ],
    providers:    [ AuthenticationService, AuthenticationGuard, SessionService, { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true } ]
})
export class ServicesModule { }
