import {Injectable} from '@angular/core';
import {AuthenticationService} from './authentication.sevice';
import {UserDetails} from '../../../../openapi';
import {UserGroup} from '../../model/user-group.model';
import {BehaviorSubject} from 'rxjs';
import RoleEnum = UserDetails.RoleEnum;

@Injectable()
export class AccessRoleService {

    static readonly VIEWER_GROUP = new UserGroup('viewer', [RoleEnum.Judge, RoleEnum.Viewer, RoleEnum.Participant, RoleEnum.Admin]);
    static readonly PARTICIPANT_GROUP = new UserGroup('participant', [RoleEnum.Participant, RoleEnum.Admin]);
    static readonly JUDGE_GROUP = new UserGroup('judge', [RoleEnum.Judge, RoleEnum.Admin]);
    static readonly ADMIN_GROUP = new UserGroup('admin', [RoleEnum.Admin]);

    private currentRole: BehaviorSubject<RoleEnum> = new BehaviorSubject<RoleEnum>(RoleEnum.Viewer);

    constructor(private authenticationService: AuthenticationService) {
        this.authenticationService.role.subscribe(this.currentRole);
    }

    public accessGranted(group: UserGroup): boolean {
        return group.roles.indexOf(this.currentRole.value) > -1;
    }


}
