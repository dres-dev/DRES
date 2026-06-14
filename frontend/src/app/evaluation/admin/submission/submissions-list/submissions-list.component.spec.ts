import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';

import { SubmissionsListComponent } from './submissions-list.component';
import { AppConfig } from '../../../../app.config';
import { WebSocketService } from '../../../../services/websocket.service';
import { EvaluationAdministratorService, EvaluationService, TemplateService } from '../../../../../../openapi';
import { IWsServerMessage } from '../../../../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../../../../model/ws/server-message-type.enum';

// ── Helpers ───────────────────────────────────────────────────────────────────

function wsMsg(type: ServerMessageType.ServerMessageTypeEnum): IWsServerMessage {
  return { evaluationId: 'eval-1', type, timestamp: Date.now() };
}

const REFRESH_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
];

const IRRELEVANT_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.PING,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
];

// ── Suite ─────────────────────────────────────────────────────────────────────

describe('SubmissionsListComponent WebSocket wiring', () => {
  let component: SubmissionsListComponent;
  let fixture: ComponentFixture<SubmissionsListComponent>;
  let wsService: jasmine.SpyObj<WebSocketService>;
  let evalService: jasmine.SpyObj<EvaluationService>;
  let evaluationAdminService: jasmine.SpyObj<EvaluationAdministratorService>;
  let templateService: jasmine.SpyObj<TemplateService>;
  let messages$: Subject<IWsServerMessage>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();

    wsService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect'], {
      messages$: messages$.asObservable(),
    });

    evalService = jasmine.createSpyObj('EvaluationService', ['getApiV2EvaluationByEvaluationIdInfo']);
    evalService.getApiV2EvaluationByEvaluationIdInfo.and.returnValue(of({ id: 'eval-1', templateId: 'tpl-1' } as any));

    evaluationAdminService = jasmine.createSpyObj('EvaluationAdministratorService', [
      'getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId',
    ]);
    evaluationAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId.and.returnValue(of([] as any));

    templateService = jasmine.createSpyObj('TemplateService', ['getApiV2TemplateByTemplateIdTaskList']);
    templateService.getApiV2TemplateByTemplateIdTaskList.and.returnValue(of([{ id: 'task-1' }] as any));

    TestBed.configureTestingModule({
      declarations: [SubmissionsListComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: EvaluationService, useValue: evalService },
        { provide: EvaluationAdministratorService, useValue: evaluationAdminService },
        { provide: TemplateService, useValue: templateService },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of({ get: (key: string) => (key === 'runId' ? 'eval-1' : 'task-1') }) },
        },
        { provide: AppConfig, useValue: { resolveApiUrl: (p: string) => `http://localhost${p}` } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: MatDialog, useValue: {} },
        { provide: WebSocketService, useValue: wsService },
      ],
    });

    fixture = TestBed.createComponent(SubmissionsListComponent);
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

  // ── submission list refresh on WS message ─────────────────────────────────

  REFRESH_TYPES.forEach((type) => {
    it(`refreshes the submission list when ${type} message is received`, fakeAsync(() => {
      component.ngAfterViewInit();
      tick();
      evaluationAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(evaluationAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId).toHaveBeenCalledWith('eval-1', 'task-1');
      discardPeriodicTasks();
    }));
  });

  // ── PING / unrelated messages are ignored ──────────────────────────────────

  IRRELEVANT_TYPES.forEach((type) => {
    it(`does not refresh the submission list for ${type} message`, fakeAsync(() => {
      component.ngAfterViewInit();
      tick();
      evaluationAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(evaluationAdminService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId).not.toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });
});
