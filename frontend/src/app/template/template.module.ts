import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatInputModule} from '@angular/material/input';
import {ReactiveFormsModule} from '@angular/forms';
import {TemplateBuilderModule} from './template-builder/template-builder.module';


@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        MatInputModule,
        ReactiveFormsModule,
        TemplateBuilderModule
    ]
})
export class TemplateModule {
}
