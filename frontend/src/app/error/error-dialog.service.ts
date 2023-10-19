import { Injectable } from '@angular/core';
import { MatDialog } from "@angular/material/dialog";
import { ErrorDialogComponent } from "./error-dialog/error-dialog.component";

@Injectable()
export class ErrorDialogService {

  private opened = false;

  constructor(private dialog: MatDialog) {

  }

  openDialog(message: string):void {
    if(!this.opened){
      this.opened = true;
      const dialogRef = this.dialog.open(ErrorDialogComponent, {
        data: {message},
        maxHeight: '100%;',
        width: '800px',
        maxWidth: '100%',
        disableClose: true,
        hasBackdrop: true
      });

      dialogRef.afterClosed().subscribe(() => this.opened = false);
    }
  }
}
