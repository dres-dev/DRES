import {merge, Observable, Subject, timer} from 'rxjs';
import {CompetitionInfo, CompetitionRunAdminService, CompetitionRunService} from '../../../openapi';
import {switchMap} from 'rxjs/operators';
import {Router} from '@angular/router';

export class AbstractRunListComponent {

    displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'timeLeft', 'description', 'teamCount'];
    update = new Subject();
    runs: Observable<CompetitionInfo[]>;

    constructor(protected runService: CompetitionRunService,
                protected runAdminService: CompetitionRunAdminService,
                protected router: Router) {

        this.runs = merge(
            timer(0, 5000),
            this.update
        ).pipe(switchMap(() => this.runService.getApiRun()));
    }

    /**
     *
     * @param runId
     */
    public navigateToViewer(runId: number) {
        this.router.navigate(['/run/viewer', runId]);
    }

    /**
     * Normalizes the display of the time that is left for a competition.
     *
     * @param timeLeft
     */
    public normalizeTimeLeft(timeLeft: number) {
        return timeLeft > -1 ? `${Math.round(timeLeft / 1000)}s` : 'n/a';
    }
}
