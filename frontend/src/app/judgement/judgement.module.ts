import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JudgementViewerComponent } from './judgement-viewer.component';
import { JudgementMediaViewerComponent } from './judgement-media-viewer.component';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {FlexModule} from '@angular/flex-layout';
import {MatTooltipModule} from '@angular/material/tooltip';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatBadgeModule} from '@angular/material/badge';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import { JudgementDialogComponent } from './judgement-dialog/judgement-dialog.component';
import {MatCardModule} from '@angular/material/card';
import {MatDialogModule} from '@angular/material/dialog';
import {SharedModule} from '../shared/shared.module';
import { JudgementVotingViewerComponent } from './judgement-voting-viewer.component';
import {QRCodeModule} from 'angularx-qrcode';



@NgModule({
  declarations: [JudgementViewerComponent, JudgementMediaViewerComponent, JudgementDialogComponent, JudgementVotingViewerComponent],
    imports: [
        CommonModule,
        MatIconModule,
        MatButtonModule,
        FlexModule,
        MatTooltipModule,
        BrowserAnimationsModule,
        MatBadgeModule,
        MatProgressBarModule,
        MatCardModule,
        MatDialogModule,
        SharedModule,
        QRCodeModule
    ],
  exports: [ JudgementViewerComponent ]
})
export class JudgementModule { }
