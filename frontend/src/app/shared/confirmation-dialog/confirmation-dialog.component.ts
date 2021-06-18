import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog} from '@angular/material/dialog';

export interface ConfirmationDialogComponentData {
    text: string;
    color: string;
}

@Component({
    selector: 'app-confirmation-dialog',
    templateUrl: './confirmation-dialog.component.html',
    styleUrls: ['./confirmation-dialog.component.scss']
})
export class ConfirmationDialogComponent {

    constructor(public dialog: MatDialog,
                @Inject(MAT_DIALOG_DATA) public data: ConfirmationDialogComponentData) {
    }

}
