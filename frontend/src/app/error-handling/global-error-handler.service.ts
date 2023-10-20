import { Injectable, NgZone } from "@angular/core";
import { ErrorDialogService } from "../error/error-dialog.service";
import { HttpErrorResponse } from "@angular/common/http";

@Injectable()
export class GlobalErrorHandlerService {

  constructor(
    private errorDialogService: ErrorDialogService,
    private zone: NgZone
  ) {}

  handleError(error: Error) {
    this.zone.run(() =>
      this.errorDialogService.openDialog(
        error?.message
      )
    );

    console.error('Error from global error handler', error);
  }
}
