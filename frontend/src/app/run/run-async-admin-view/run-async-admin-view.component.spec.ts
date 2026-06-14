import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, Subject } from 'rxjs';

import { RunAsyncAdminViewComponent } from './run-async-admin-view.component';
import { AppConfig } from '../../app.config';
import { WebSocketService } from '../../services/websocket.service';
import {
  DownloadService,
  EvaluationAdministratorService,
  EvaluationClientService,
  EvaluationScoresService,
  EvaluationService,
} from '../../../../openapi';
import { IWsServerMessage } from '../../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../../model/ws/server-message-type.enum';

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

describe('RunAsyncAdminViewComponent WebSocket wiring', () => {
  let component: RunAsyncAdminViewComponent;
  let fixture: ComponentFixture<RunAsyncAdminViewComponent>;
  let wsService: jasmine.SpyObj<WebSocketService>;
  let evaluationService: jasmine.SpyObj<EvaluationService>;
  let runAdminService: jasmine.SpyObj<EvaluationAdministratorService>;
  let messages$: Subject<IWsServerMessage>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();

    wsService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect'], {
      messages$: messages$.asObservable(),
    });

    evaluationService = jasmine.createSpyObj('EvaluationService', ['getApiV2EvaluationByEvaluationIdInfo']);
    evaluationService.getApiV2EvaluationByEvaluationIdInfo.and.returnValue(
      of({ id: 'eval-1', templateId: 'tpl-1', name: 'Test', teams: [], taskTemplates: [{ templateId: 't1' }] } as any)
    );

    runAdminService = jasmine.createSpyObj('EvaluationAdministratorService', [
      'getApiV2EvaluationAdminByEvaluationIdOverview',
      'getApiV2EvaluationAdminByEvaluationIdTaskPastList',
      'getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId',
    ]);
    runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview.and.returnValue(of({} as any));
    runAdminService.getApiV2EvaluationAdminByEvaluationIdTaskPastList.and.returnValue(of([] as any));
    runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId.and.returnValue(of([] as any));

    TestBed.configureTestingModule({
      declarations: [RunAsyncAdminViewComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: EvaluationService, useValue: evaluationService },
        { provide: EvaluationAdministratorService, useValue: runAdminService },
        { provide: EvaluationClientService, useValue: {} },
        { provide: EvaluationScoresService, useValue: {} },
        { provide: DownloadService, useValue: {} },
        {
          provide: ActivatedRoute,
          // Use a non-completing Observable: the component subscribes its `runId`
          // BehaviorSubject directly to `params`, so a source like `of(...)` that
          // completes synchronously would also complete (close) that subject.
          useValue: { params: new Observable((subscriber) => subscriber.next({ runId: 'eval-1' })) },
        },
        { provide: AppConfig, useValue: { resolveApiUrl: (p: string) => `http://localhost${p}` } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']) },
        { provide: MatDialog, useValue: {} },
        { provide: WebSocketService, useValue: wsService },
      ],
    });

    fixture = TestBed.createComponent(RunAsyncAdminViewComponent);
    component = fixture.componentInstance;
  });

  // ── connect / disconnect ───────────────────────────────────────────────────

  it('connects the WebSocket with the evaluationId on init', () => {
    component.ngAfterViewInit();
    expect(wsService.connect).toHaveBeenCalledWith('eval-1');
  });

  it('disconnects the WebSocket on destroy', () => {
    component.ngAfterViewInit();
    fixture.destroy();
    expect(wsService.disconnect).toHaveBeenCalled();
  });

  // ── refresh on WS message ──────────────────────────────────────────────────

  REFRESH_TYPES.forEach((type) => {
    it(`refreshes the run overview when ${type} message is received`, fakeAsync(() => {
      component.ngAfterViewInit();
      component.run.subscribe();
      tick();
      runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview).toHaveBeenCalledWith('eval-1');
      discardPeriodicTasks();
    }));

    it(`refreshes the task submission counts when ${type} message is received`, fakeAsync(() => {
      component.ngAfterViewInit();
      component.taskSubmissionCounts.subscribe();
      tick();
      runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId).toHaveBeenCalledWith('eval-1', 't1');
      discardPeriodicTasks();
    }));
  });

  // ── PING / unrelated messages are ignored ──────────────────────────────────

  IRRELEVANT_TYPES.forEach((type) => {
    it(`does not refresh the run overview for ${type} message`, fakeAsync(() => {
      component.ngAfterViewInit();
      component.run.subscribe();
      tick();
      runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runAdminService.getApiV2EvaluationAdminByEvaluationIdOverview).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });
});
