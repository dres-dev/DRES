import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BuilderComponent } from './builder/builder.component';
import {GeneralInfoTabComponent} from './builder/tabs/general-info-tab/general-info-tab.component';
import { InformationComponent } from './builder/components/information/information.component';
import { AbstractTemplateBuilderComponent } from './builder/components/abstract-template-builder/abstract-template-builder.component';
import {MatInputModule} from '@angular/material/input';
import {ReactiveFormsModule} from '@angular/forms';



@NgModule({
  declarations: [
    BuilderComponent,
    GeneralInfoTabComponent,
    InformationComponent,
    AbstractTemplateBuilderComponent
  ],
    imports: [
        CommonModule,
        MatInputModule,
        ReactiveFormsModule
    ]
})
export class TemplateModule { }
