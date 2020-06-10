import {Injectable} from '@angular/core';
import {AuthenticationService} from './authentication.sevice';
import {UserDetails} from '../../../../openapi';
import {UserGroup} from '../../model/user-group.model';
import {BehaviorSubject} from 'rxjs';
import RoleEnum = UserDetails.RoleEnum;

@Injectable()
export class AccessRoleService {

    static readonly VIEWER_GROUP = new UserGroup('viewer', [RoleEnum.JUDGE, RoleEnum.VIEWER, RoleEnum.PARTICIPANT, RoleEnum.ADMIN]);
    static readonly PARTICIPANT_GROUP = new UserGroup('participant', [RoleEnum.PARTICIPANT, RoleEnum.ADMIN]);
    static readonly JUDGE_GROUP = new UserGroup('judge', [RoleEnum.JUDGE, RoleEnum.ADMIN]);
    static readonly ADMIN_GROUP = new UserGroup('admin', [RoleEnum.ADMIN]);

    private currentRole: BehaviorSubject<RoleEnum> = new BehaviorSubject<RoleEnum>(RoleEnum.VIEWER);

    constructor(private authenticationService: AuthenticationService) {
        this.authenticationService.role.subscribe(this.currentRole);
    }

    public accessGranted(group: UserGroup): boolean {
        return group.roles.indexOf(this.currentRole.value) > -1;
    }


}
