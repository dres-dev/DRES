import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { TemplateBuilderComponent } from "./template-builder.component";
import { TemplateBuilderComponentsModule } from "./components/template-builder-components.module";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { SharedModule } from "../../shared/shared.module";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatInputModule } from "@angular/material/input";
import { ReactiveFormsModule } from "@angular/forms";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatSelectModule } from "@angular/material/select";
import { CompetitionBuilderModule } from "../../competition/competition-builder/competition-builder.module";


@NgModule({
  declarations: [
    TemplateBuilderComponent

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
