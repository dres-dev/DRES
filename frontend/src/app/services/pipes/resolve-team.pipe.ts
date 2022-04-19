import {Pipe, PipeTransform} from '@angular/core';
import {RestTeam} from '../../../../openapi';

@Pipe({
    name: 'resolveTeam'
})
export class ResolveTeamPipe implements PipeTransform {


    transform(teamId: string, teams: RestTeam[]): RestTeam | null {
        if (!teamId || !teams) {
            return null;
        }
        const filtered = teams.filter(t => t.uid === teamId);
        return filtered.length > 0 ? filtered[0] : null;
    }

}
