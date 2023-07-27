import { NgModule } from '@angular/core';
import {RouterModule, Routes, UrlSerializer} from '@angular/router';
import { LoginComponent } from './user/login/login.component';
import { RunViewerComponent } from './viewer/run-viewer.component';
import { ProfileComponent } from './user/profile/profile.component';
import { AdminUserListComponent } from './user/admin-user-list/admin-user-list.component';
import { JudgementViewerComponent } from './judgement/judgement-viewer.component';
import { RunListComponent } from './run/run-list.component';
import { RunAdminViewComponent } from './run/run-admin-view.component';
import { CollectionListComponent } from './collection/collection-list/collection-list.component';
import { CollectionViewerComponent } from './collection/collection-viewer/collection-viewer.component';
import { CanDeactivateGuard } from './services/can-deactivate.guard';
import { RunScoreHistoryComponent } from './run/score-history/run-score-history.component';
import { JudgementVotingViewerComponent } from './judgement/judgement-voting-viewer.component';
import { RunAsyncAdminViewComponent } from './run/run-async-admin-view/run-async-admin-view.component';
import {TemplateBuilderComponent} from './template/template-builder/template-builder.component';
import { TemplateListComponent } from "./template/template-list/template-list.component";
import { SubmissionsListComponent } from "./evaluation/admin/submission/submissions-list/submissions-list.component";
import {
  canActivateAdministrator,
  canActivateAnyRole,
  canActivateJudge,
  canActivatePublicVote
} from "./services/session/guard";
import {NonescapingUrlserializerClass} from "./nonescaping-urlserializer.class";
import {ForbiddenComponent} from "./error/forbidden.component";
import {NotFoundComponent} from "./error/not-found.component";

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
    path: TEMPLATE_ROUTE + '/list',
    component: TemplateListComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: TEMPLATE_ROUTE+'/builder/:templateId',
    component: TemplateBuilderComponent,
    canActivate: [canActivateAdministrator],
    canDeactivate: [CanDeactivateGuard]
  },
  {
    path: EVALUATION_ROUTE+'/list',
    component: RunListComponent,
    canActivate: [canActivateAnyRole]
  },
  {
    path: EVALUATION_ROUTE+'/admin/:runId',
    component: RunAdminViewComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: EVALUATION_ROUTE+'/scores/:runId',
    component: RunScoreHistoryComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: EVALUATION_ROUTE+'/admin/async/:runId',
    component: RunAsyncAdminViewComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: EVALUATION_ROUTE+'/admin/submissions/:runId/:taskId',
    component: SubmissionsListComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: EVALUATION_ROUTE+'/viewer/:runId',
    component: RunViewerComponent,
    canActivate: [canActivateAnyRole]
  },
  {
    path: 'judge/:runId',
    component: JudgementViewerComponent,
    canActivate: [canActivateJudge]
  },
  {
    path: 'vote/:runId',
    component: JudgementVotingViewerComponent,
    canActivate: [canActivatePublicVote]
  },

  {
    path: 'user',
    component: ProfileComponent,
    canActivate: [canActivateAnyRole]
  },

  {
    path: 'user/list',
    component: AdminUserListComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: 'collection/list',
    component: CollectionListComponent,
    canActivate: [canActivateAdministrator]
  },
  {
    path: 'collection/:collectionId',
    component: CollectionViewerComponent,
    canActivate: [canActivateAdministrator]
  },

  /* The login + forbidden page is always accessible. */
  { path: 'login', component: LoginComponent },
  { path: 'forbidden', component: ForbiddenComponent},

  /* Two important 'catch-all's. */
  { path: '', redirectTo: 'evaluation/list', pathMatch: 'full' },
  { path: '**', component: NotFoundComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { enableTracing: false })], // enable tracing for debugging
  exports: [RouterModule],
  providers: [{ provide: UrlSerializer, useClass: NonescapingUrlserializerClass }]
})
export class AppRoutingModule {}
