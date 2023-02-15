import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TemplateBuilderComponent} from './template-builder.component';
import {TemplateBuilderComponentsModule} from './components/template-builder-components.module';
import {MatIconModule} from '@angular/material/icon';
import {MatTabsModule} from '@angular/material/tabs';
import {SharedModule} from '../../shared/shared.module';
import {FlexModule} from '@angular/flex-layout';


@NgModule({
    declarations: [
        TemplateBuilderComponent
    ],
    imports: [
        CommonModule,
        TemplateBuilderComponentsModule,
        MatIconModule,
        MatTabsModule,
        SharedModule,
        FlexModule
    ],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {
}
