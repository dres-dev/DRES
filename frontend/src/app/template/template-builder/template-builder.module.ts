import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TemplateBuilderComponent} from './template-builder.component';
import {TemplateBuilderComponentsModule} from './components/template-builder-components.module';
import {MatIconModule} from '@angular/material/icon';
import {MatTabsModule} from '@angular/material/tabs';
import {SharedModule} from '../../shared/shared.module';
import { MatTooltipModule } from "@angular/material/tooltip";
import { QueryDescriptionFormFieldComponent } from './components/query-description-form-field/query-description-form-field.component';
import { QueryDescriptionTextFormFieldComponent } from './components/query-description-text-form-field/query-description-text-form-field.component';
import { QueryDescriptionExternalFormFieldComponent } from './components/query-description-external-form-field/query-description-external-form-field.component';
import { QueryDescriptionMediaItemFormFieldComponent } from './components/query-description-media-item-form-field/query-description-media-item-form-field.component';
import { MatInputModule } from "@angular/material/input";
import { ReactiveFormsModule } from "@angular/forms";
import { QueryDescriptionMediaItemImageFormFieldComponent } from './components/query-description-media-item-image-form-field/query-description-media-item-image-form-field.component';
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { QueryDescriptionMediaItemVideoFormFieldComponent } from './components/query-description-media-item-video-form-field/query-description-media-item-video-form-field.component';
import { MatSelectModule } from "@angular/material/select";
import { CompetitionBuilderModule } from "../../competition/competition-builder/competition-builder.module";
import { QueryDescriptionExternalVideoFormFieldComponent } from './components/query-description-external-video-form-field/query-description-external-video-form-field.component';
import { QueryDescriptionExternalImageFormFieldComponent } from './components/query-description-external-image-form-field/query-description-external-image-form-field.component';


@NgModule({
    declarations: [
        TemplateBuilderComponent,

    ],
  imports: [
    CommonModule,
    MatIconModule,
    MatTabsModule,
    SharedModule,
    MatTooltipModule,
    MatInputModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatSelectModule,
    CompetitionBuilderModule,
    TemplateBuilderComponentsModule
  ],
  exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {
}
