import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, interval, Subscription } from 'rxjs';
import { catchError, filter, switchMap } from 'rxjs/operators';
import {CurrentTime, StatusService} from '../../../../openapi';

@Component({
  selector: 'app-api-status',
  templateUrl: './api-status.component.html',
  styleUrls: ['./api-status.component.scss'],
})
export class ApiStatusComponent implements OnInit, OnDestroy {
  @Input() public pingFrequency = 10_000; // ms
  rtt: BehaviorSubject<number> = new BehaviorSubject<number>(-1);
  clockDiff: BehaviorSubject<number>;
  error = false;
  private subscription: Subscription;
  private now = Date.now();

  constructor(private statusService: StatusService) {}

  ngOnInit(): void {
    this.subscription = interval(this.pingFrequency)
      .pipe(
        switchMap((_) => {
          this.now = Date.now();
          return this.statusService.getApiV2StatusTime();
        }),
        catchError((err) => {
          this.error = true;
          return null;
        }),
        filter((x) => x != null)
      )
      .subscribe((time: CurrentTime) => {
        const newNow = Date.now();
        this.rtt.next(newNow - this.now);
        this.clockDiff = new BehaviorSubject<number>(Math.abs(time.timeStamp - (this.now + this.rtt.getValue() / 2)));
      });
    /*this.subscription = interval(this.pingFrequency).subscribe(_ => {
            this.statusService.getApiV1StatusTime().pipe(
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
