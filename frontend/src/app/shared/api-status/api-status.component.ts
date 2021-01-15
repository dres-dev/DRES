import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {CurrentTime, StatusService} from '../../../../openapi';
import {BehaviorSubject, interval, Subscription} from 'rxjs';
import {catchError, filter, switchMap} from 'rxjs/operators';

@Component({
    selector: 'app-api-status',
    templateUrl: './api-status.component.html',
    styleUrls: ['./api-status.component.scss']
})
export class ApiStatusComponent implements OnInit, OnDestroy {

    @Input() public pingFrequency = 1000; // ms
    rtt: BehaviorSubject<number> = new BehaviorSubject<number>(-1);
    error = false;
    private subscription: Subscription;
    private now = Date.now();

    constructor(private statusService: StatusService) {
    }

    ngOnInit(): void {
        this.subscription = interval(this.pingFrequency).pipe(
            switchMap(_ => {
                this.now = Date.now();
                return this.statusService.getApiStatusTime();
            }),
            catchError(err => {
                this.error = true;
                return null;
            }),
            filter(x => x != null)
        ).subscribe(time => {
            this.rtt.next(((time as CurrentTime).timeStamp - this.now));
        });
        /*this.subscription = interval(this.pingFrequency).subscribe(_ => {
            this.statusService.getApiStatusTime().pipe(
                catchError(err => {
                    this.error = true;
                    return null;
                }),
                filter(x => x != null),
                map(time => {
                    const now = Date.now() / 1000;
                    this.rtt.next((time as CurrentTime).timeStamp - now);
                })
            );
        });*/
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }
}
