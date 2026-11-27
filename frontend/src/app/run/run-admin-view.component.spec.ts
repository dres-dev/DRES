import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Subject, merge, of } from 'rxjs';

import { RunAdminViewComponent } from './run-admin-view.component';
import { AppConfig } from '../app.config';
import { WebSocketService } from '../services/websocket.service';
import { EvaluationAdministratorService, EvaluationService, TemplateService } from '../../../openapi';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

/**
 * Double for the WebSocket service: exposes only the contract the component uses and never polls on
 * its own, so a spec sees exactly the requests its trigger caused.
 */
function silentWebSocketService(
  messages$: Subject<IWsServerMessage> = new Subject<IWsServerMessage>(),
  reconnected$: Subject<void> = new Subject<void>(),
  resync$: Subject<void> = new Subject<void>()
): any {
  return {
    messages$: messages$.asObservable(),
    reconnected$: reconnected$.asObservable(),
    resync$: new Subject<void>().asObservable(),
    connect: jasmine.createSpy('connect'),
    disconnect: jasmine.createSpy('disconnect'),
    /* Mirrors the real service: the caller's triggers, plus the recovery signals it always
       merges in. Anything narrower would let a spec pass while the real pipeline polls. */
    fallbackPolling: jasmine
      .createSpy('fallbackPolling')
      .and.callFake((triggers: any) => merge(triggers ?? EMPTY, reconnected$, resync$)),
    reportPollResult: jasmine.createSpy('reportPollResult'),
  };
}


// ── Helpers ───────────────────────────────────────────────────────────────────

function wsMsg(type: ServerMessageType.ServerMessageTypeEnum): IWsServerMessage {
  return { evaluationId: 'eval-1', type, timestamp: Date.now() };
}

const RUN_STATE_REFRESH_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.TASK_PREPARE,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
];

const IRRELEVANT_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.PING,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_START,
];

// ── Suite ─────────────────────────────────────────────────────────────────────

describe('RunAdminViewComponent WebSocket wiring', () => {
  let component: RunAdminViewComponent;
  let fixture: ComponentFixture<RunAdminViewComponent>;
  let wsService: any;
  let runService: jasmine.SpyObj<EvaluationService>;
  let runAdminService: jasmine.SpyObj<EvaluationAdministratorService>;
  let templateService: jasmine.SpyObj<TemplateService>;
  let messages$: Subject<IWsServerMessage>;
  let reconnected$: Subject<void>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();
    reconnected$ = new Subject<void>();

    wsService = silentWebSocketService(messages$, reconnected$) as any;

    runService = jasmine.createSpyObj('EvaluationService', [
      'getApiV2EvaluationByEvaluationIdInfo',
      'getApiV2EvaluationByEvaluationIdState',
    ]);
    runService.getApiV2EvaluationByEvaluationIdInfo.and.returnValue(of({ id: 'eval-1', templateId: 'tpl-1', name: 'Test' } as any));
    runService.getApiV2EvaluationByEvaluationIdState.and.returnValue(of({ taskTemplateId: 't1', taskStatus: 'RUNNING' } as any));

    runAdminService = jasmine.createSpyObj('EvaluationAdministratorService', [
      'getApiV2EvaluationAdminByEvaluationIdOverview',
      'getApiV2EvaluationAdminByEvaluationIdViewerList',
      'getApiV2EvaluationAdminByEvaluationIdTaskPastList',
      'getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId',
    ]);
    runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview.and.returnValue(of({} as any));
    runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList.and.returnValue(of([] as any));
    runAdminService.getApiV2EvaluationAdminByEvaluationIdTaskPastList.and.returnValue(of([] as any));
    runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId.and.returnValue(of([] as any));

    templateService = jasmine.createSpyObj('TemplateService', ['getApiV2TemplateByTemplateIdTeamList']);
    templateService.getApiV2TemplateByTemplateIdTeamList.and.returnValue(of([] as any));

    TestBed.configureTestingModule({
      declarations: [RunAdminViewComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: EvaluationService, useValue: runService },
        { provide: EvaluationAdministratorService, useValue: runAdminService },
        { provide: TemplateService, useValue: templateService },
        { provide: ActivatedRoute, useValue: { params: of({ runId: 'eval-1' }) } },
        { provide: AppConfig, useValue: { resolveApiUrl: (p: string) => `http://localhost${p}` } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']) },
        { provide: MatDialog, useValue: {} },
        { provide: WebSocketService, useValue: wsService },
      ],
    });

    fixture = TestBed.createComponent(RunAdminViewComponent);
    component = fixture.componentInstance;
  });

  // ── connect / disconnect ───────────────────────────────────────────────────

  it('connects the WebSocket with the evaluationId on init', () => {
    component.ngOnInit();
    expect(wsService.connect).toHaveBeenCalledWith('eval-1');
  });

  it('disconnects the WebSocket on destroy', () => {
    component.ngOnInit();
    fixture.destroy();
    expect(wsService.disconnect).toHaveBeenCalled();
  });

  // ── state refresh on WS message ────────────────────────────────────────────

  RUN_STATE_REFRESH_TYPES.forEach((type) => {
    it(`refreshes run state when ${type} message is received`, fakeAsync(() => {
      component.ngOnInit();
      component.run.subscribe();
      tick();
      runService.getApiV2EvaluationByEvaluationIdState.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runService.getApiV2EvaluationByEvaluationIdState).toHaveBeenCalledWith('eval-1');
      discardPeriodicTasks();
    }));

  });

  // ── viewer status refreshes only for viewer status changes ─────────────────

  it('refreshes the viewer list for a matching VIEWER_UPDATE', fakeAsync(() => {
    component.ngOnInit();
    component.viewers.subscribe();
    tick();
    runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList.calls.reset();

    messages$.next(wsMsg(ServerMessageType.ServerMessageTypeEnum.VIEWER_UPDATE));
    tick();

    expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList).toHaveBeenCalledWith('eval-1');
    discardPeriodicTasks();
  }));

  it('ignores viewer status updates for other evaluations', fakeAsync(() => {
    component.ngOnInit();
    component.viewers.subscribe();
    tick();
    runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList.calls.reset();

    messages$.next({ ...wsMsg(ServerMessageType.ServerMessageTypeEnum.VIEWER_UPDATE), evaluationId: 'eval-2' });
    tick();

    expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList).not.toHaveBeenCalled();
    discardPeriodicTasks();
  }));

  RUN_STATE_REFRESH_TYPES.forEach((type) => {
    it(`does not refresh the viewer list for ${type} messages`, fakeAsync(() => {
      component.ngOnInit();
      component.viewers.subscribe();
      tick();
      runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });

  // ── PING / unrelated messages are ignored ──────────────────────────────────

  IRRELEVANT_TYPES.forEach((type) => {
    it(`does not refresh run state for ${type} message`, fakeAsync(() => {
      component.ngOnInit();
      component.run.subscribe();
      tick();
      runService.getApiV2EvaluationByEvaluationIdState.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runService.getApiV2EvaluationByEvaluationIdState).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });

  // ── resync on reconnect ─────────────────────────────────────────────────────

  it('refreshes run state, overview and viewer list when the WebSocket reconnects', fakeAsync(() => {
    component.ngOnInit();
    component.run.subscribe();
    component.runOverview.subscribe();
    component.viewers.subscribe();
    tick();
    runService.getApiV2EvaluationByEvaluationIdState.calls.reset();
    runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview.calls.reset();
    runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList.calls.reset();

    reconnected$.next();
    tick();

    expect(runService.getApiV2EvaluationByEvaluationIdState).toHaveBeenCalledWith('eval-1');
    expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview).toHaveBeenCalledWith('eval-1');
    expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList).toHaveBeenCalledWith('eval-1');
    discardPeriodicTasks();
  }));
});
