import { ErrorHandler, NgModule } from "@angular/core";
import { CommonModule } from '@angular/common';
import { GlobalErrorHandlerService } from "./global-error-handler.service";
import { HTTP_INTERCEPTORS } from "@angular/common/http";
import { DresBackendUnauthorisedHandlerService } from "./dres-backend-unauthorised-handler.service";



@NgModule({
  declarations: [],
  imports: [
    CommonModule
  ],
  providers: [
    {provide: ErrorHandler, useClass: GlobalErrorHandlerService},

  ]
})
export class ErrorHandlingModule { }
