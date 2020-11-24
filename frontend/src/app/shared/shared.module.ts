import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackButtonComponent } from './back-button/back-button.component';
import {MatButtonModule} from '@angular/material/button';
import {ServicesModule} from '../services/services.module';
import {MatIconModule} from '@angular/material/icon';



@NgModule({
  declarations: [BackButtonComponent],
  exports: [
    BackButtonComponent, MatButtonModule, ServicesModule, MatIconModule
  ],
  imports: [
    CommonModule,
    MatButtonModule,
    ServicesModule,
    MatIconModule
  ]
})
export class SharedModule { }
