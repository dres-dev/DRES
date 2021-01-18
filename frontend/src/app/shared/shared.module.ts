import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackButtonComponent } from './back-button/back-button.component';
import {MatButtonModule} from '@angular/material/button';
import {ServicesModule} from '../services/services.module';
import {MatIconModule} from '@angular/material/icon';
import { ApiStatusComponent } from './api-status/api-status.component';
import {MatTooltipModule} from '@angular/material/tooltip';
import { DownloadJsonButtonComponent } from './download-json-button/download-json-button.component';



@NgModule({
  declarations: [BackButtonComponent, ApiStatusComponent, DownloadJsonButtonComponent],
    exports: [
        BackButtonComponent, MatButtonModule, ServicesModule, MatIconModule, ApiStatusComponent, DownloadJsonButtonComponent
    ],
    imports: [
        CommonModule,
        MatButtonModule,
        ServicesModule,
        MatIconModule,
        MatTooltipModule
    ]
})
export class SharedModule { }
