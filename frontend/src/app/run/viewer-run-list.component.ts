import { Component, ViewChild } from "@angular/core";
import { AbstractRunListComponent, RunInfoWithState } from './abstract-run-list.component';
import { Router } from '@angular/router';
import { AccessChecking } from '../model/access-checking.interface';
import { UserGroup } from '../model/user-group.model';
import { AccessRoleService } from '../services/session/access-role.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import {DownloadService, EvaluationAdministratorService, EvaluationScoresService, EvaluationService} from '../../../openapi';
import { MatTable } from "@angular/material/table";

@Component({
  selector: 'app-viewer-run-list',
  templateUrl: './viewer-run-list.component.html',
})
export class ViewerRunListComponent extends AbstractRunListComponent implements AccessChecking {
  judgeGroup = AccessRoleService.JUDGE_GROUP;
  viewerGroup = AccessRoleService.VIEWER_GROUP;
  participantGroup = AccessRoleService.PARTICIPANT_GROUP;

  @ViewChild('table', {static: true}) table: MatTable<any>;

  postRefresh: () => void = () => {
    if(this.table){
      this.table.renderRows()
    }
  };

  constructor(
    runService: EvaluationService,
    runAdminService: EvaluationAdministratorService,
    scoreService: EvaluationScoresService,
    downloadService: DownloadService,
    router: Router,
    snackBar: MatSnackBar,
    private accessService: AccessRoleService
  ) {
    super(runService, runAdminService, scoreService, downloadService, router, snackBar);
  }

  hasAccessFor(group: UserGroup): boolean {
    return this.accessService.accessGranted(group);
  }

  cannotAccess(row: RunInfoWithState) {
    if (this.hasAccessFor(this.participantGroup)) {
      return !row.runProperties.participantCanView;
    } else {
      return false;
    }
  }
}
