import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProfileComponent } from './profile/profile.component';
import {MatCardModule} from '@angular/material/card';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {FlexLayoutModule} from '@angular/flex-layout';
import {MatGridListModule} from '@angular/material/grid-list';
import {MatDividerModule} from '@angular/material/divider';



@NgModule({
  declarations: [ProfileComponent],
    imports: [
        CommonModule,
        MatCardModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        FlexLayoutModule,
        MatGridListModule,
        MatDividerModule
    ]
})
export class UserModule { }
