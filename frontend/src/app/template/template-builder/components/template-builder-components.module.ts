import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TemplateInformationComponent} from './template-information/template-information.component';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';


@NgModule({
    declarations: [
        TemplateInformationComponent
    ],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule
    ],
    exports: [TemplateInformationComponent]
})
export class TemplateBuilderComponentsModule {
}
