import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {CompetitionService, DownloadService, RestCompetitionDescription, UserService} from '../../../../../openapi';
import {ActivatedRoute, Router, RouterStateSnapshot} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {AppConfig} from '../../../app.config';
import {Observable, Subscription} from 'rxjs';
import {take} from 'rxjs/operators';
import {DeactivationGuarded} from '../../../services/can-deactivate.guard';
import {CompetionBuilderService} from '../competion-builder.service';

@Component({
    selector: 'app-tabbed-builder',
    templateUrl: './tabbed-builder.component.html',
    styleUrls: ['./tabbed-builder.component.scss']
})
export class TabbedBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded {

    dirty = false;
    routeSubscription: Subscription;
    changeSubscription: Subscription;
    competitionSub: Subscription;

    competitionId: string;
    competition: RestCompetitionDescription;

    constructor(
        private builderService: CompetionBuilderService,
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
            this.competitionService.getApiV1CompetitionWithCompetitionid(this.competitionId).subscribe(
                (c) => {
                    /* initialise service with competition from route */
                    this.builderService.initialise(c)
                    /* subscribe to changes on the competition & store local copy */
                    this.competitionSub = this.builderService.asObservable().subscribe(c => this.competition = c)
                },
                (r) => {
                    this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                }
            );
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
        if (this.builderService.checkDirty()) {
            this.routerService.navigate(['/competition/list']);
        }
    }

    canDeactivate(nextState?: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
        return this.builderService.checkDirty();
    }

    @HostListener('window:beforeunload', ['$event'])
    handleBeforeUnload(event: BeforeUnloadEvent) {
        if (!this.builderService.checkDirty()) {
            event.preventDefault();
            event.returnValue = '';
            return;
        }
        delete event.returnValue;
    }

    /**
     * @deprecated
     */
    public refresh() {
        if (this.builderService.checkDirty()) {
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

}
