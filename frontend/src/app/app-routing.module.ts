import { NgModule } from '@angular/core';
import { DefaultUrlSerializer, RouterModule, Routes, UrlSerializer } from '@angular/router';
import { CompetitionBuilderComponent } from './competition/competition-builder/competition-builder.component';
import { CompetitionListComponent } from './competition/competition-list/competition-list.component';
import { LoginComponent } from './user/login/login.component';
import { AuthenticationGuard } from './services/session/authentication.guard';
import { RunViewerComponent } from './viewer/run-viewer.component';
import { ProfileComponent } from './user/profile/profile.component';
import { AdminUserListComponent } from './user/admin-user-list/admin-user-list.component';
import { JudgementViewerComponent } from './judgement/judgement-viewer.component';
import { RunListComponent } from './run/run-list.component';
import { RunAdminViewComponent } from './run/run-admin-view.component';
import { CollectionListComponent } from './collection/collection-list/collection-list.component';
import { CollectionViewerComponent } from './collection/collection-viewer/collection-viewer.component';
import { AdminAuditlogOverviewComponent } from './auditlog/admin-auditlog-overview/admin-auditlog-overview.component';
import { CanDeactivateGuard } from './services/can-deactivate.guard';
import { RunAdminSubmissionsListComponent } from './run/run-admin-submissions-list/run-admin-submissions-list.component';
import { RunScoreHistoryComponent } from './run/score-history/run-score-history.component';
import { JudgementVotingViewerComponent } from './judgement/judgement-voting-viewer.component';
import { RunAsyncAdminViewComponent } from './run/run-async-admin-view/run-async-admin-view.component';
import { NonescapingUrlserializerClass } from './nonescaping-urlserializer.class';
import {ApiRole} from '../../openapi';

/**
 * The ROUTE for evaluation templates.
 * Formerly 'competition'
 */
const TEMPLATE_ROUTE = 'template';
/**
 * The ROUTE for evaluation instances / runs
 * Formerly 'run'
 */
const EVALUATION_ROUTE = 'evaluation';

const routes: Routes = [
  {
    path: TEMPLATE_ROUTE+'/list',
    component: CompetitionListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: TEMPLATE_ROUTE+'/builder/:competitionId',
    component: CompetitionBuilderComponent,
    canActivate: [AuthenticationGuard],
    canDeactivate: [CanDeactivateGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: EVALUATION_ROUTE+'/list',
    component: RunListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN, ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE] },
  },
  {
    path: EVALUATION_ROUTE+'/admin/:runId',
    component: RunAdminViewComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: EVALUATION_ROUTE+'/scores/:runId',
    component: RunScoreHistoryComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: EVALUATION_ROUTE+'/admin/async/:runId',
    component: RunAsyncAdminViewComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: EVALUATION_ROUTE+'/admin/submissions/:runId/:taskId',
    component: RunAdminSubmissionsListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: EVALUATION_ROUTE+'/viewer/:runId',
    component: RunViewerComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN, ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE] },
  },
  {
    path: 'judge/:runId',
    component: JudgementViewerComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN, ApiRole.JUDGE] },
  },
  {
    path: 'vote/:runId',
    component: JudgementVotingViewerComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN, ApiRole.JUDGE, ApiRole.VIEWER] },
  },

  { path: 'login', component: LoginComponent },

  {
    path: 'user',
    component: ProfileComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN, ApiRole.VIEWER, ApiRole.JUDGE, ApiRole.PARTICIPANT] },
  },

  {
    path: 'user/list',
    component: AdminUserListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: 'collection/list',
    component: CollectionListComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },
  {
    path: 'collection/:collectionId',
    component: CollectionViewerComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },

  {
    path: 'logs/list',
    component: AdminAuditlogOverviewComponent,
    canActivate: [AuthenticationGuard],
    data: { roles: [ApiRole.ADMIN] },
  },

  // otherwise redirect to home
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { relativeLinkResolution: 'legacy', enableTracing: false })], // enable tracing for debugging
  exports: [RouterModule],
  providers: [{ provide: UrlSerializer, useClass: NonescapingUrlserializerClass }],
})
export class AppRoutingModule {}
