import { Pipe, PipeTransform } from '@angular/core';
import {ApiTeam, ApiTeamInfo} from '../../../../openapi';

@Pipe({
  name: 'resolveTeam',
})
export class ResolveTeamPipe implements PipeTransform {
  transform(teamId: string, teams: ApiTeamInfo[]): ApiTeamInfo | null {
    if (!teamId || !teams) {
      return null;
    }
    const filtered = teams.filter((t) => t.id === teamId);
    return filtered.length > 0 ? filtered[0] : null;
  }
}
