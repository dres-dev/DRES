import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TemplateBuilderComponent} from './template-builder.component';
import {TemplateBuilderComponentsModule} from './components/template-builder-components.module';
import {MatIconModule} from '@angular/material/icon';
import {MatTabsModule} from '@angular/material/tabs';
import {SharedModule} from '../../shared/shared.module';


@NgModule({
    declarations: [
        TemplateBuilderComponent
    ],
    imports: [
        CommonModule,
        TemplateBuilderComponentsModule,
        MatIconModule,
        MatTabsModule,
        SharedModule
    ],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {
}
