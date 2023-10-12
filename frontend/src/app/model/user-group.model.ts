import {ApiRole} from '../../../openapi';

export class UserGroup {
  constructor(public readonly name: string, public readonly roles: ApiRole[]) {}

  debugRoles(): string {
    return JSON.stringify(this.roles);
  }
}
