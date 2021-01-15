import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackButtonComponent } from './back-button/back-button.component';
import {MatButtonModule} from '@angular/material/button';
import {ServicesModule} from '../services/services.module';
import {MatIconModule} from '@angular/material/icon';
import { ApiStatusComponent } from './api-status/api-status.component';
import {MatTooltipModule} from '@angular/material/tooltip';



@NgModule({
  declarations: [BackButtonComponent, ApiStatusComponent],
    exports: [
        BackButtonComponent, MatButtonModule, ServicesModule, MatIconModule, ApiStatusComponent
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
