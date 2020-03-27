import {NgModule} from '@angular/core';
import {CompetitionBuilerComponent} from './competition-builer/competition-builer.component';
import {CompetitionListComponent} from './competition-list/competition-list.component';
import {MatTableModule} from '@angular/material/table';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';

@NgModule({
    imports: [
        MatTableModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule
    ],
    exports:      [ CompetitionBuilerComponent, CompetitionListComponent ],
    declarations: [ CompetitionBuilerComponent, CompetitionListComponent ],
    providers:    [ ]
})
export class CompetitionModule { }
