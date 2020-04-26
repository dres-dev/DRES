import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, TaskDescription} from '../../../openapi';
import {BehaviorSubject, combineLatest, interval, Observable, of, Subscription, zip} from 'rxjs';
import {filter, flatMap, map, share, switchMap} from 'rxjs/operators';
import {IWsMessage} from '../model/ws/ws-message.interface';
import {IWsClientMessage} from '../model/ws/ws-client-message.interface';
import {WebSocketSubject} from 'rxjs/webSocket';
import {IWsServerMessage} from '../model/ws/ws-server-message.interface';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {ClientMessageType} from '../model/ws/client-message-type.enum';

type QueryObjectType = 'video' | 'text' | 'image';
interface QueryObject {
    type: QueryObjectType;
    url?: SafeUrl;
    text?: string[];
}
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
    
    polledState: Observable<RunState>;

    /** The currently active task. */
    currentTask = new BehaviorSubject<TaskDescription>(null);
    currentTaskSubscription: Subscription;

    /** The currently active query object. */
    currentQueryObject = new BehaviorSubject<QueryObject>(null);
    currentQueryObjectSubscription: Subscription;

    /** The currently active task. */
    taskPrepareSubscription: Subscription;

    /**
     * Converts a Base65 encoded string into an object URL of a Blob.
     *
     * @param base64 The base64 encoded string.
     */
    private static base64ToUrl(base64: string): string {
        const binary = atob(base64);
        const byteNumbers = new Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            byteNumbers[i] = binary.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray]);
        return window.URL.createObjectURL(blob);
    }

    constructor(protected runService: CompetitionRunService, private sanitizer: DomSanitizer) {}

    /**
     * Create a subscription for task changes.
     */
    ngOnInit(): void {
        /** Observable for the current task. */
        this.currentTaskSubscription = this.state.pipe(
            filter(s =>  (this.currentTask.value == null || this.currentTask.value.name !== s.currentTask.name)),
            share()
        ).subscribe(s => {
            this.currentTask.next(s.currentTask);
        });

        /** Subscription for the current query object. */
        this.currentQueryObjectSubscription = this.currentTask.pipe(
            flatMap(task => this.info.pipe(map(i => i.id))),
            flatMap(id => this.runService.getApiRunWithRunidQuery(id)),
            map(r => {
                switch (r.contentType) {
                    case 'BINARY':
                        return {
                            type: 'video',
                            url: this.sanitizer.bypassSecurityTrustUrl(TaskViewerComponent.base64ToUrl(r.payload))
                        } as QueryObject;
                    case 'TEXT':
                        return {type: 'video', text: r.payload.split(';')} as QueryObject;
                }
            })
        ).subscribe(s => {
            this.currentQueryObject.next(s);
        });

        /** Subscription reacting to TASK_PREPARE message. */
        this.taskPrepareSubscription = combineLatest([
            this.webSocket.pipe(filter(m => m.type === 'TASK_PREPARE')),
            this.currentQueryObject
        ]).subscribe(([m, q]) =>
            this.webSocketSubject.next({runId: (m as IWsServerMessage).runId, type: 'ACK'} as IWsClientMessage)
        );


        /** Observable for the current time (for running tasks only). */
        this.polledState = this.state.pipe(
            switchMap(s => {
                if (s.status === 'RUNNING_TASK') {
                    return interval(1000).pipe(
                        switchMap(t => this.runService.getApiRunStateWithRunid(s.id)),
                    );
                } else {
                    return of(s);
                }
            }),
            share()
        );
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

    public toFormattedTime(milliseconds: number): string {
        const sec = Math.round(milliseconds / 1000);
        const hours   = Math.floor(sec / 3600);
        const minutes = Math.floor(sec / 60) % 60;
        const seconds = sec % 60;

        return [hours, minutes, seconds]
            .map(v => v < 10 ? '0' + v : v)
            .filter((v, i) => v !== '00' || i > 0)
            .join(':');
    }


}
