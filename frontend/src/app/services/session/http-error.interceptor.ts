import {Injectable} from '@angular/core';
import {HttpRequest, HttpHandler, HttpEvent, HttpInterceptor} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {SessionService} from './session.service';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
    constructor(private sessionService: SessionService) { }
    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(request).pipe(catchError(err => {
            if ([401, 403].indexOf(err.status) !== -1) {
                if (this.sessionService.isLoggedIn()) {
                    console.log('Warning: It seems that you were logged out by the server!');
                    this.sessionService.end();
                }
                location.reload();
                const error = err.error.message || err.statusText;
                return throwError(error);
            }
            const error = err.error.message || err.statusText;
            return throwError(error);
        }));
    }
}
