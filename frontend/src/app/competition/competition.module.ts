import {NgModule} from '@angular/core';
import {CompetitionBuilerComponent} from './competition-builer/competition-builer.component';
import {CompetitionListComponent} from './competition-list/competition-list.component';
import {MatTableModule} from '@angular/material/table';

@NgModule({
    imports: [
        MatTableModule
    ],
    exports:      [ CompetitionBuilerComponent, CompetitionListComponent ],
    declarations: [ CompetitionBuilerComponent, CompetitionListComponent ],
    providers:    [ ]
})
export class CompetitionModule { }
