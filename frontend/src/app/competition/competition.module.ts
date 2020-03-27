import {NgModule} from '@angular/core';
import {CompetitionBuilerComponent} from './competition-builer/competition-builer.component';
import {CompetitionListComponent} from './competition-list/competition-list.component';

@NgModule({
    imports:      [ ],
    exports:      [ CompetitionBuilerComponent, CompetitionListComponent ],
    declarations: [ CompetitionBuilerComponent, CompetitionListComponent ],
    providers:    [ ]
})
export class CompetitionModule { }
