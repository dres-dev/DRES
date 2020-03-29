import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { CompetitionBuilerComponent } from './competition/competition-builer/competition-builer.component';
import { CompetitionListComponent } from './competition/competition-list/competition-list.component';
import {LoginComponent} from './login-component/login.component';


const routes: Routes = [
  { path: 'competition/list', component: CompetitionListComponent },
  { path: 'competition/builder/:competitionId', component: CompetitionBuilerComponent },
  { path: 'login', component: LoginComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
