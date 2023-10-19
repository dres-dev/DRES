import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialog } from "@angular/material/dialog";
import { ConfirmationDialogComponentData } from "../confirmation-dialog/confirmation-dialog.component";

export interface InformationDialogComponentData {
  title?: string,
  text?: string,
  closeLbl?: string,
  color?: 'primary' | 'warn' | 'accent' | null
}

@Component({
  selector: 'app-information-dialog',
  templateUrl: './information-dialog.component.html',
  styleUrls: ['./information-dialog.component.scss']
})
export class InformationDialogComponent {

  color = this.data?.color || 'primary';
  text = this.data?.text || '';
  title= this.data?.title || 'Information'
  closeLbl = this.data?.closeLbl || 'Ok'

  constructor(public dialog: MatDialog, @Inject(MAT_DIALOG_DATA) public data: InformationDialogComponentData) {}

}
