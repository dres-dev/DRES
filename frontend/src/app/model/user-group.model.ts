import { ApiUser } from '../../../openapi';
import RoleEnum = ApiUser.RoleEnum;

export class UserGroup {
  constructor(public readonly name: string, public readonly roles: RoleEnum[]) {}

  debugRoles(): string {
    return JSON.stringify(this.roles);
  }
}
