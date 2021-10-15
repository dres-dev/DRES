import {Component} from '@angular/core';
import {AbstractRunListComponent, RunInfoWithState} from './abstract-run-list.component';
import {
    CompetitionRunAdminService,
    DownloadService,
    CompetitionRunScoresService,
    CompetitionRunService
} from '../../../openapi';
import {Router} from '@angular/router';
import {AccessChecking} from '../model/access-checking.interface';
import {UserGroup} from '../model/user-group.model';
import {AccessRoleService} from '../services/session/access-role.service';

@Component({
    selector: 'app-viewer-run-list',
    templateUrl: './viewer-run-list.component.html'
})
export class ViewerRunListComponent extends AbstractRunListComponent implements AccessChecking {

    judgeGroup = AccessRoleService.JUDGE_GROUP;
    viewerGroup = AccessRoleService.VIEWER_GROUP;
    participantGroup = AccessRoleService.PARTICIPANT_GROUP;

    constructor(runService: CompetitionRunService,
                runAdminService: CompetitionRunAdminService,
                scoreService: CompetitionRunScoresService,
                downloadService: DownloadService,
                router: Router,
                private accessService: AccessRoleService) {
        super(runService, runAdminService, scoreService, downloadService, router);
    }

    hasAccessFor(group: UserGroup): boolean {
        return this.accessService.accessGranted(group);
    }

    cannotAccess(row: RunInfoWithState) {
        if (this.hasAccessFor(this.participantGroup)) {
            return !row.participantsCanView;
        } else {
            return false;
        }

    }
}
