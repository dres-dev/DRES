import {NgModule } from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {CompetitionBuilderComponent} from './competition/competition-builder/competition-builder.component';
import {CompetitionListComponent} from './competition/competition-list/competition-list.component';
import {LoginComponent} from './login-component/login.component';
import {AuthenticationGuard} from './services/session/authentication.guard';
import {UserDetails} from '../../openapi';
import RoleEnum = UserDetails.RoleEnum;
import {RunListComponent} from './run/run-list.component';


const routes: Routes = [
  {
    path: 'competition/list',
    component: CompetitionListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [RoleEnum.ADMIN] }
  },
  {
    path: 'competition/builder/:competitionId',
    component: CompetitionBuilderComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [RoleEnum.ADMIN] }
  },
  {
    path: 'run/list',
    component: RunListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [RoleEnum.ADMIN] }
  },

  { path: 'login', component: LoginComponent },

  // otherwise redirect to home
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
