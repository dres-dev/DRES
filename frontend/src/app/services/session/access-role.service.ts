import { Injectable } from '@angular/core';
import { AuthenticationService } from './authentication.sevice';
import { UserGroup } from '../../model/user-group.model';
import { BehaviorSubject } from 'rxjs';
import {ApiRole} from '../../../../openapi';

@Injectable()
export class AccessRoleService {
  static readonly VIEWER_GROUP = new UserGroup('viewer', [
    ApiRole.JUDGE,
    ApiRole.VIEWER,
    ApiRole.PARTICIPANT,
    ApiRole.ADMIN,
  ]);
  static readonly PARTICIPANT_GROUP = new UserGroup('participant', [ApiRole.PARTICIPANT, ApiRole.ADMIN]);
  static readonly JUDGE_GROUP = new UserGroup('judge', [ApiRole.JUDGE, ApiRole.ADMIN]);
  static readonly ADMIN_GROUP = new UserGroup('admin', [ApiRole.ADMIN]);

  private currentRole: BehaviorSubject<ApiRole> = new BehaviorSubject<ApiRole>(ApiRole.VIEWER);

  constructor(private authenticationService: AuthenticationService) {
    this.authenticationService.role.subscribe(this.currentRole);
  }

  public accessGranted(group: UserGroup): boolean {
    return group.roles.indexOf(this.currentRole.value) > -1;
  }
}
