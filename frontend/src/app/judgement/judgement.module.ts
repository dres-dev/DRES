import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JudgementViewerComponent } from './judgement-viewer.component';
import {MatIconModule} from '@angular/material/icon';



@NgModule({
  declarations: [JudgementViewerComponent],
  imports: [
    CommonModule,
    MatIconModule
  ],
  exports: [ JudgementViewerComponent ]
})
export class JudgementModule { }
