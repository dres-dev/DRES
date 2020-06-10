import {UserGroup} from './user-group.model';

export interface AccessChecking {

    hasAccessFor(group: UserGroup): boolean;
}
