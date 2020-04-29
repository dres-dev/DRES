import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {CompetitionRunService, QueryDescription, RunInfo, RunState, TaskDescription} from '../../../openapi';
import {BehaviorSubject, combineLatest, interval, Observable, of, Subscription} from 'rxjs';
import {catchError, filter, flatMap, map, switchMap} from 'rxjs/operators';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {WebSocketSubject} from 'rxjs/webSocket';
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';

@Component({
    selector: 'app-task-viewer',
    templateUrl: './task-viewer.component.html',
    styleUrls: ['./task-viewer.component.scss']
})
export class TaskViewerComponent implements OnInit, OnDestroy {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
    @Input() webSocket: Observable<IWsMessage>;
    @Input() webSocketSubject: WebSocketSubject<IWsMessage>;
    
    timeLeft: Observable<number>;
    timeElapsed: Observable<number>;

    /** The currently active task. */
    currentTask = new BehaviorSubject<TaskDescription>(null);
    currentTaskSubscription: Subscription;

    /** The currently active query object. */
    currentQueryObject = new BehaviorSubject<QueryDescription>(null);
    currentQueryObjectSubscription: Subscription;

    /** The currently active task. */
    taskPrepareSubscription: Subscription;

    constructor(protected runService: CompetitionRunService) {}

    /**
     * Create a subscription for task changes.
     */
    ngOnInit(): void {
        /* Observable for the current task. */
        this.currentTaskSubscription = this.state.pipe(
            filter(s =>  (this.currentTask.value == null || this.currentTask.value.name !== s.currentTask.name)),
        ).subscribe(s => {
            this.currentTask.next(s.currentTask);
        });

        /* Subscription for the current query object. */
        this.currentQueryObjectSubscription = this.currentTask.pipe(
            flatMap(task => this.info.pipe(map(i => i.id))),
            flatMap(id => this.runService.getApiRunWithRunidQuery(id)),
            catchError(e => {
                console.log('Warning: Could not load query object due to error: ' + e);
                return of(null);
            })
        ).subscribe(s => {
            this.currentQueryObject.next(s);
        });

        /* Subscription reacting to TASK_PREPARE message. */
        this.taskPrepareSubscription = combineLatest([
            this.webSocket.pipe(filter(m => m.type === 'TASK_PREPARE')),
            this.currentQueryObject
        ]).subscribe(([m, q]) =>
            this.webSocketSubject.next({runId: (m as IWsServerMessage).runId, type: 'ACK'} as IWsClientMessage)
        );


        /* Observable for the time left and time elapsed (for running tasks only). */
        const polledState = this.state.pipe(
            filter(s => s.status === 'RUNNING_TASK'),
            flatMap(s => interval(1000).pipe(switchMap(t => this.runService.getApiRunStateWithRunid(s.id))))
        );

        this.timeLeft = polledState.pipe(map(s => s.timeLeft));
        this.timeElapsed = polledState.pipe(map(s => s.currentTask?.duration - s.timeLeft));
    }

    /**
     * Cleanup all subscriptions.
     */
    ngOnDestroy(): void {
        this.currentTaskSubscription.unsubscribe();
        this.currentTaskSubscription = null;

        this.currentQueryObjectSubscription.unsubscribe();
        this.currentQueryObjectSubscription = null;

        this.taskPrepareSubscription.unsubscribe();
        this.taskPrepareSubscription = null;
    }

    public toFormattedTime(sec: number): string {
        const hours   = Math.floor(sec / 3600);
        const minutes = Math.floor(sec / 60) % 60;
        const seconds = sec % 60;

        return [hours, minutes, seconds]
            .map(v => v < 10 ? '0' + v : v)
            .filter((v, i) => v !== '00' || i > 0)
            .join(':');
    }
}
