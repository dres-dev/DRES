import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {CompetitionBuilderComponent} from './competition/competition-builder/competition-builder.component';
import {CompetitionListComponent} from './competition/competition-list/competition-list.component';
import {LoginComponent} from './user/login/login.component';
import {AuthenticationGuard} from './services/session/authentication.guard';
import {UserDetails} from '../../openapi';
import {AdminRunListComponent} from './run/admin-run-list.component';
import {RunViewerComponent} from './viewer/run-viewer.component';
import {ProfileComponent} from './user/profile/profile.component';
import {AdminUserListComponent} from './user/admin-user-list/admin-user-list.component';
import RoleEnum = UserDetails.RoleEnum;
import {JudgementViewerComponent} from './judgement/judgement-viewer.component';


const routes: Routes = [
  {
    path: 'competition/list',
    component: CompetitionListComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN]}
  },
  {
    path: 'competition/builder/:competitionId',
    component: CompetitionBuilderComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN]}
  },
  {
    path: 'run/list',
    component: AdminRunListComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN, RoleEnum.VIEWER, RoleEnum.PARTICIPANT, RoleEnum.JUDGE]}
  },
  {
    path: 'run/viewer/:runId',
    component: RunViewerComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN, RoleEnum.VIEWER, RoleEnum.PARTICIPANT, RoleEnum.JUDGE]}
  },
  {
    path: 'judge/:runId',
    component: JudgementViewerComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN, RoleEnum.JUDGE]}
  },

  {path: 'login', component: LoginComponent},

  {
    path: 'user',
    component: ProfileComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN, RoleEnum.VIEWER, RoleEnum.JUDGE, RoleEnum.PARTICIPANT]}
  },

  {
    path: 'user/list',
    component: AdminUserListComponent,
    canActivate: [AuthenticationGuard],
    data: {roles: [RoleEnum.ADMIN]}
  },

  // otherwise redirect to home
  {path: '**', redirectTo: ''}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
