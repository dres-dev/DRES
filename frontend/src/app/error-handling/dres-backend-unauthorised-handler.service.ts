import { Injectable, Injector } from "@angular/core";
import { AppConfig } from "../app.config";
import { ActivatedRoute, Router } from "@angular/router";
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from "@angular/common/http";
import { EMPTY, Observable, of } from "rxjs";
import { catchError, tap } from "rxjs/operators";
import { IConfig } from "../model/config.interface";

@Injectable()
export class DresBackendUnauthorisedHandlerService implements HttpInterceptor {

  constructor(private injector: Injector) {
    console.log("Interceptor ctor")
  }

  private config: IConfig;

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    /**
     * Due to a bootstrapping issue, we have to let pass the very first loading of the config -- otherwise we wouldn't know the backend.
     */
    if(req.url.startsWith("config.json?random")){
      return next.handle(req).pipe(tap({
        next: (event) => {
          if(event instanceof  HttpResponse){
            console.log("Receiving first config")
            const resp = event as HttpResponse<any>
            this.config = resp.body as IConfig
          }
        }
      }))
    }else if(!this.config) {
      console.warn("No config present, won't work")
      // in case we do not have a config loaded and its not the first config call, then we just stop working
      return next.handle(req)
    }else{
      return next.handle(req).pipe(
        catchError((err, caught) => {
          if(err instanceof HttpErrorResponse){
            console.log("HTTP Error Response, this", this)
            const e = err as HttpErrorResponse
            const url = new URL(e.url)
            const router = this.injector.get<Router>(Router)
            if(e.status === 401 && url.host.startsWith(this.config.endpoint.host)){
              console.log("401", router)
              if(!router?.url){
                console.log("No router url, reload")
                // somehow we lost the router, lets reload the page
                //window.location.reload()
              }
              console.log("Redirecting")
              let rt
              if(router.url.startsWith("/login")){
                rt = router.parseUrl(router.url)
              }else{
                rt = router.parseUrl(`/login?returnUrl=${router.url}`)
              }
              router.navigateByUrl(rt)
            }
          }
          return EMPTY;
        }));
    }
  }
}
