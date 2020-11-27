import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {CompetitionBuilderComponent} from './competition/competition-builder/competition-builder.component';
import {CompetitionListComponent} from './competition/competition-list/competition-list.component';
import {LoginComponent} from './user/login/login.component';
import {AuthenticationGuard} from './services/session/authentication.guard';
import {UserDetails} from '../../openapi';
import {RunViewerComponent} from './viewer/run-viewer.component';
import {ProfileComponent} from './user/profile/profile.component';
import {AdminUserListComponent} from './user/admin-user-list/admin-user-list.component';
import {JudgementViewerComponent} from './judgement/judgement-viewer.component';
import {RunListComponent} from './run/run-list.component';
import {RunAdminViewComponent} from './run/run-admin-view.component';
import RoleEnum = UserDetails.RoleEnum;
import {CollectionListComponent} from './collection/collection-list/collection-list.component';
import {CollectionViewerComponent} from './collection/collection-viewer/collection-viewer.component';
import {AdminAuditlogOverviewComponent} from './auditlog/admin-auditlog-overview/admin-auditlog-overview.component';
import {CanDeactivateGuard} from './services/can-deactivate.guard';
import {RunAdminSubmissionsListComponent} from './run/run-admin-submissions-list/run-admin-submissions-list.component';


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
        canDeactivate: [CanDeactivateGuard],
        data: {roles: [RoleEnum.ADMIN]}
    },
    {
        path: 'run/list',
        component: RunListComponent,
        canActivate: [AuthenticationGuard],
        data: {roles: [RoleEnum.ADMIN, RoleEnum.VIEWER, RoleEnum.PARTICIPANT, RoleEnum.JUDGE]}
    },
    {
        path: 'run/admin/:runId',
        component: RunAdminViewComponent,
        canActivate: [AuthenticationGuard],
        data: {roles: [RoleEnum.ADMIN]}
    },
    {
        path: 'run/admin/submissions/:runId/:taskId',
        component: RunAdminSubmissionsListComponent,
        canActivate: [AuthenticationGuard],
        data: {roles: [RoleEnum.ADMIN]}
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
    {
        path: 'collection/list',
        component: CollectionListComponent,
        canActivate: [AuthenticationGuard],
        data: {roles: [RoleEnum.ADMIN]}
    },
    {
        path: 'collection/:collectionId',
        component: CollectionViewerComponent,
        canActivate: [AuthenticationGuard],
        data: {roles: [RoleEnum.ADMIN]}
    },

    {
        path: 'logs/list',
        component: AdminAuditlogOverviewComponent,
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
