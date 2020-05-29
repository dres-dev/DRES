import {UserDetails} from '../../../openapi';
import RoleEnum = UserDetails.RoleEnum;

export class UserGroup {

    constructor(public readonly name: string, public readonly roles: RoleEnum[]) {

    }


    debugRoles(): string {
        return JSON.stringify(this.roles);
    }
}
