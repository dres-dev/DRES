import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';

import { RunAdminViewComponent } from './run-admin-view.component';
import { AppConfig } from '../app.config';
import { WebSocketService } from '../services/websocket.service';
import { EvaluationAdministratorService, EvaluationService, TemplateService } from '../../../openapi';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

// ── Helpers ───────────────────────────────────────────────────────────────────

function wsMsg(type: ServerMessageType.ServerMessageTypeEnum): IWsServerMessage {
  return { evaluationId: 'eval-1', type, timestamp: Date.now() };
}

const REFRESH_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
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
  let wsService: jasmine.SpyObj<WebSocketService>;
  let runService: jasmine.SpyObj<EvaluationService>;
  let runAdminService: jasmine.SpyObj<EvaluationAdministratorService>;
  let templateService: jasmine.SpyObj<TemplateService>;
  let messages$: Subject<IWsServerMessage>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();

    wsService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect'], {
      messages$: messages$.asObservable(),
    });

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

  REFRESH_TYPES.forEach((type) => {
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

    it(`refreshes the viewer list when ${type} message is received`, fakeAsync(() => {
      component.ngOnInit();
      component.viewers.subscribe();
      tick();
      runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdViewerList).toHaveBeenCalledWith('eval-1');
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
});
