import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackButtonComponent } from './back-button/back-button.component';
import { MatButtonModule } from '@angular/material/button';
import { ServicesModule } from '../services/services.module';
import { MatIconModule } from '@angular/material/icon';
import { ApiStatusComponent } from './api-status/api-status.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DownloadButtonComponent } from './download-button/download-button.component';
import { UploadJsonButtonComponent } from './upload-json-button/upload-json-button.component';
import { ConfirmationDialogComponent } from './confirmation-dialog/confirmation-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';
import { DynamicTableComponent } from './dynamic-table/dynamic-table.component';
import {MatTableModule} from '@angular/material/table';
import { ActionableDynamicTable } from './actionable-dynamic-table/actionable-dynamic-table.component';
import { ServerInfoComponent } from './server-info/server-info.component';
import { TargetMediaViewerComponent } from './target-media-viewer/target-media-viewer.component';
import { MediaItemViewerComponent } from './media-item-viewer/media-item-viewer.component';
import { InformationDialogComponent } from './information-dialog/information-dialog.component';

@NgModule({
  declarations: [
    BackButtonComponent,
    ApiStatusComponent,
    DownloadButtonComponent,
    UploadJsonButtonComponent,
    ConfirmationDialogComponent,
    DynamicTableComponent,
    ActionableDynamicTable,
    ServerInfoComponent,
    TargetMediaViewerComponent,
    MediaItemViewerComponent,
    InformationDialogComponent,
  ],
  exports: [
    BackButtonComponent,
    MatButtonModule,
    ServicesModule,
    MatIconModule,
    ApiStatusComponent,
    DownloadButtonComponent,
    UploadJsonButtonComponent,
    ActionableDynamicTable,
    TargetMediaViewerComponent,
    MediaItemViewerComponent
  ],
  imports: [CommonModule, MatButtonModule, ServicesModule, MatIconModule, MatTooltipModule, MatDialogModule, MatTableModule]
})
export class SharedModule {}
