import {Component} from '@angular/core';
import {UserDetails} from '../../../openapi';
import RoleEnum = UserDetails.RoleEnum;
import {Observable} from 'rxjs';
import {AuthenticationService} from '../services/session/authentication.sevice';
import {map} from 'rxjs/operators';

@Component({
    selector: 'app-run-list',
    templateUrl: './run-list.component.html'
})
export class RunListComponent {

    currentRole: Observable<RoleEnum>;
    constructor(private authenticationService: AuthenticationService) {
        this.currentRole = this.authenticationService.user.pipe(map(u => u.role));
    }
}
