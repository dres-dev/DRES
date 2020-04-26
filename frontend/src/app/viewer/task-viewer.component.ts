import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {CompetitionRunService, KisVisualTaskDescription, MediaService, RunInfo, RunState} from '../../../openapi';
import {interval, Observable, of, Subscription} from 'rxjs';
import {filter, flatMap, map, share, switchMap, tap} from 'rxjs/operators';
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
    
    polledState: Observable<RunState>;
    mediaSource: Observable<string>;

    constructor(protected runService: CompetitionRunService, protected mediaService: MediaService) {}

    ngOnInit(): void {
        this.mediaSource = this.webSocket.pipe(
            filter(m => (m as IWsServerMessage).type === 'TASK_PREPARE'),
            flatMap(m => this.runService.getApiRunStateWithRunid(m.runId)),
            map(s => s.currentTask as KisVisualTaskDescription),
            flatMap(t => this.mediaService.getApiMediaWithCollectionWithItem('v3c1', t.item.name, 'response')),
            map(r => window.URL.createObjectURL(r.url)),
            tap(s => {
                this.webSocketSubject.next({runId: 1, type: 'ACK'} as IWsClientMessage);
            })
        );

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
     * Cleanup subscription.
     */
    ngOnDestroy(): void {
        //this.prepareSubscription.unsubscribe();
        //this.prepareSubscription = null;
    }
    
    public toHHMMSS(milliseconds: number): string {
        const sec_num = Math.round(milliseconds / 1000);
        const hours   = Math.floor(sec_num / 3600);
        const minutes = Math.floor(sec_num / 60) % 60;
        const seconds = sec_num % 60;

        return [hours, minutes, seconds]
            .map(v => v < 10 ? '0' + v : v)
            .filter((v, i) => v !== '00' || i > 0)
            .join(':');
    }


}
