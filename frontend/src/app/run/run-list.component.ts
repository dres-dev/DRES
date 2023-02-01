import { Component } from '@angular/core';
import { ApiUser } from '../../../openapi';
import { Observable } from 'rxjs';
import { AuthenticationService } from '../services/session/authentication.sevice';
import { map } from 'rxjs/operators';
import { AccessChecking } from '../model/access-checking.interface';
import { UserGroup } from '../model/user-group.model';
import { AccessRoleService } from '../services/session/access-role.service';
import RoleEnum = ApiUser.RoleEnum;

@Component({
  selector: 'app-run-list',
  templateUrl: './run-list.component.html',
})
export class RunListComponent implements AccessChecking {
  currentRole: Observable<RoleEnum>;

  adminGroup = AccessRoleService.ADMIN_GROUP;

  constructor(private authenticationService: AuthenticationService, private accessService: AccessRoleService) {
    this.currentRole = this.authenticationService.user.pipe(map((u) => u.role));
  }

  hasAccessFor(group: UserGroup): boolean {
    return this.accessService.accessGranted(group);
  }
}
