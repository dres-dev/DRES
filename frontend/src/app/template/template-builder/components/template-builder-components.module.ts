import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TemplateInformationComponent} from './template-information/template-information.component';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import { JudgesListComponent } from './judges-list/judges-list.component';
import {FlexModule} from '@angular/flex-layout';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatTableModule} from '@angular/material/table';
import { TeamsListComponent } from './teams-list/teams-list.component';


@NgModule({
    declarations: [
        TemplateInformationComponent,
        JudgesListComponent,
        TeamsListComponent
    ],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        FlexModule,
        MatButtonModule,
        MatTooltipModule,
        MatIconModule,
        MatMenuModule,
        MatAutocompleteModule,
        MatTableModule
    ],
    exports: [TemplateInformationComponent, JudgesListComponent, TeamsListComponent]
})
export class TemplateBuilderComponentsModule {
}
