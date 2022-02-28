import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {CompetitionService, DownloadService, RestCompetitionDescription, UserService} from '../../../../../openapi';
import {ActivatedRoute, Router, RouterStateSnapshot} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {AppConfig} from '../../../app.config';
import {Observable, Subscription} from 'rxjs';
import {take} from 'rxjs/operators';
import {DeactivationGuarded} from '../../../services/can-deactivate.guard';

@Component({
    selector: 'app-tabbed-builder',
    templateUrl: './tabbed-builder.component.html',
    styleUrls: ['./tabbed-builder.component.scss']
})
export class TabbedBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded {

    dirty = false;
    routeSubscription: Subscription;
    changeSubscription: Subscription;

    competitionId: string;
    competition: RestCompetitionDescription;

    constructor(
        private competitionService: CompetitionService,
        private userService: UserService,
        private downloadService: DownloadService,
        private route: ActivatedRoute,
        private routerService: Router,
        private snackBar: MatSnackBar,
        private dialog: MatDialog,
        private config: AppConfig
    ) {
    }

    ngOnDestroy(): void {
        this.routeSubscription.unsubscribe();
        this.changeSubscription.unsubscribe();
    }

    ngOnInit(): void {
        this.routeSubscription = this.route.params.subscribe(p => {
            this.competitionId = p.competitionId;
            // this.refresh();
        });
        // this.changeSubscription = this.form.valueChanges.subscribe(() => {
        //   this.dirty = true;
        // });
    }

    fileProvider = () => {
        // this.fetchDataToCompetition();
        return this.competition?.name ? this.competition.name : 'competition-download.json';
    };

    downloadProvider = () => {
        return this.downloadService.getApiV1DownloadCompetitionWithCompetitionid(this.competitionId)
            .pipe(take(1));
        // .toPromise();
    };

    public save() {
        if (/*this.form.valid*/ true) {
            // this.fetchDataToCompetition();
            this.competitionService.patchApiV1Competition(this.competition).subscribe(
                (c) => {
                    this.snackBar.open(c.description, null, {duration: 5000});
                    this.dirty = false;
                },
                (r) => {
                    this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                }
            );
        }
    }

    public back() {
        if (this.checkDirty()) {
            this.routerService.navigate(['/competition/list']);
        }
    }

    canDeactivate(nextState?: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
        return this.checkDirty();
    }

    @HostListener('window:beforeunload', ['$event'])
    handleBeforeUnload(event: BeforeUnloadEvent) {
        if (!this.checkDirty()) {
            event.preventDefault();
            event.returnValue = '';
            return;
        }
        delete event.returnValue;
    }

    public refresh() {
        if (this.checkDirty()) {
            this.competitionService.getApiV1CompetitionWithCompetitionid(this.competitionId).subscribe(
                (c) => {
                    this.competition = c;
                    /*this.form.get('name').setValue(c.name);
                    this.form.get('description').setValue(c.description);
                    */
                    // TODO fetch other stuff
                    this.dirty = false;
                },
                (r) => {
                    this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                }
            );
        }
    }

    private checkDirty(): boolean {
        if (!this.dirty) {
            return true;
        }
        return confirm('There are unsaved changes in this competition that will be lost. Do you really want to proceed?');
    }
}
