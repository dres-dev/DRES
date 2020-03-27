import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { CompetitionBuilerComponent } from './competition-builer/competition-builer.component';
import { CompetitionListComponent } from './competition-list/competition-list.component';


const routes: Routes = [
  { path: 'competition/list', component: CompetitionListComponent },
  { path: 'competition/builder', component: CompetitionBuilerComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
