import {Component} from '@angular/core';
import {AbstractRunListComponent} from './abstract-run-list.component';
import {CompetitionRunAdminService, CompetitionRunService} from '../../../openapi';
import {Router} from '@angular/router';

@Component({
    selector: 'app-viewer-run-list',
    templateUrl: './viewer-run-list.component.html'
})
export class ViewerRunListComponent extends AbstractRunListComponent {
    constructor(runService: CompetitionRunService,
                runAdminService: CompetitionRunAdminService,
                router: Router) {
        super(runService, runAdminService, router);
    }

}
