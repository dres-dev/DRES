import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JudgementViewerComponent } from './judgement-viewer.component';
import { JudgementMediaViewerComponent } from './judgement-media-viewer.component';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {FlexModule} from '@angular/flex-layout';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatVideoModule} from 'mat-video';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatBadgeModule} from '@angular/material/badge';
import {MatProgressBarModule} from '@angular/material/progress-bar';



@NgModule({
  declarations: [JudgementViewerComponent, JudgementMediaViewerComponent],
    imports: [
        CommonModule,
        MatIconModule,
        MatButtonModule,
        FlexModule,
        MatTooltipModule,
        MatVideoModule,
        BrowserAnimationsModule,
        MatBadgeModule,
        MatProgressBarModule
    ],
  exports: [ JudgementViewerComponent ]
})
export class JudgementModule { }
