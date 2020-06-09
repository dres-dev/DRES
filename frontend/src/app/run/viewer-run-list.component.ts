import {Component} from '@angular/core';
import {AbstractRunListComponent} from './abstract-run-list.component';
import {CompetitionRunAdminService, CompetitionRunService} from '../../../openapi';
import {Router} from '@angular/router';
import {AccessChecking} from '../model/access-checking.interface';
import {UserGroup} from '../model/user-group.model';
import {AccessRoleService} from '../services/session/access-role.service';

@Component({
    selector: 'app-viewer-run-list',
    templateUrl: './viewer-run-list.component.html'
})
export class ViewerRunListComponent extends AbstractRunListComponent implements AccessChecking{

    judgeGroup = AccessRoleService.JUDGE_GROUP;

    constructor(runService: CompetitionRunService,
                runAdminService: CompetitionRunAdminService,
                router: Router,
                private accessService: AccessRoleService) {
        super(runService, runAdminService, router);
    }

    hasAccessFor(group: UserGroup): boolean {
        return this.accessService.accessGranted(group);
    }
}
