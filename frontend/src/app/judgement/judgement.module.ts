import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JudgementViewerComponent } from './judgement-viewer.component';
import { JudgementMediaViewerComponent } from './judgement-media-viewer.component';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {FlexModule} from '@angular/flex-layout';
import {MatTooltipModule} from '@angular/material/tooltip';




@NgModule({
  declarations: [JudgementViewerComponent, JudgementMediaViewerComponent],
    imports: [
        CommonModule,
        MatIconModule,
        MatButtonModule,
        FlexModule,
        MatTooltipModule
    ],
  exports: [ JudgementViewerComponent ]
})
export class JudgementModule { }
