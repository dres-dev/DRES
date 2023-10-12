import { Injectable } from "@angular/core";
import { AppConfig } from "../app.config";
import { ActivatedRoute, Router } from "@angular/router";
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { EMPTY, Observable } from "rxjs";
import { catchError, tap } from "rxjs/operators";

@Injectable()
export class DresBackendUnauthorisedHandlerService implements HttpInterceptor {

  constructor(private config: AppConfig, private router: Router, private activatedRoute: ActivatedRoute) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err, caught) => {
      if(err instanceof HttpErrorResponse){
        const e = err as HttpErrorResponse
        const url = new URL(e.url)
        if(e.status === 401 && url.host.startsWith(this.config.config.endpoint.host)){
          let rt
          if(this.router.url.startsWith("/login")){
            rt = this.router.parseUrl(this.router.url)
          }else{
            rt = this.router.parseUrl(`/login?returnUrl=${this.router.url}`)
          }
          this.router.navigateByUrl(rt)
        }
      }
      return EMPTY;
    }));
  }
}
