import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JudgementViewerComponent } from './judgement-viewer.component';
import { JudgementMediaViewerComponent } from './judgement-media-viewer.component';
import {MatIconModule} from '@angular/material/icon';



@NgModule({
  declarations: [JudgementViewerComponent, JudgementMediaViewerComponent],
  imports: [
    CommonModule,
    MatIconModule
  ],
  exports: [ JudgementViewerComponent ]
})
export class JudgementModule { }
