import {Component} from '@angular/core';
import {SessionService} from '../services/session/session.service';
import {UserDetails} from '../../../openapi';
import RoleEnum = UserDetails.RoleEnum;
import {Observable} from 'rxjs';

@Component({
    selector: 'app-run-list',
    templateUrl: './run-list.component.html'
})
export class RunListComponent {

    currentRole: Observable<RoleEnum>;
    constructor(private sessionService: SessionService) {
        this.currentRole = this.sessionService.getRole();
    }
}
