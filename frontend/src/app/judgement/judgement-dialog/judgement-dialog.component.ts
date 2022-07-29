import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { JudgementDialogContent } from './judgement-dialog-content.model';

@Component({
  selector: 'app-judgement-welcome-dialog',
  templateUrl: './judgement-dialog.component.html',
  styleUrls: ['./judgement-dialog.component.scss'],
})
export class JudgementDialogComponent implements OnInit {
  constructor(@Inject(MAT_DIALOG_DATA) public content: JudgementDialogContent) {}

  ngOnInit(): void {}
}
