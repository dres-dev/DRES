import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Title } from '@angular/platform-browser';
import { Overlay } from '@angular/cdk/overlay';
import { of, Subject } from 'rxjs';

import { RunViewerComponent } from './run-viewer.component';
import { AppConfig } from '../app.config';
import { WebSocketService } from '../services/websocket.service';
import { EvaluationService } from '../../../openapi';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

// ── Helpers ───────────────────────────────────────────────────────────────────

function wsMsg(type: ServerMessageType.ServerMessageTypeEnum): IWsServerMessage {
  return { evaluationId: 'eval-1', type, timestamp: Date.now() };
}

const IRRELEVANT_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.PING,
];

const REFRESH_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
  ServerMessageType.ServerMessageTypeEnum.TASK_PREPARE,
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_START,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_END,
];

// ── Suite ─────────────────────────────────────────────────────────────────────

describe('RunViewerComponent WebSocket wiring', () => {
  let component: RunViewerComponent;
  let wsService: jasmine.SpyObj<WebSocketService>;
  let runService: jasmine.SpyObj<EvaluationService>;
  let messages$: Subject<IWsServerMessage>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();

    wsService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect'], {
      messages$: messages$.asObservable(),
    });

    runService = jasmine.createSpyObj('EvaluationService', [
      'getApiV2EvaluationByEvaluationIdState',
      'getApiV2EvaluationByEvaluationIdInfo',
    ]);
    runService.getApiV2EvaluationByEvaluationIdState.and.returnValue(
      of({ taskStatus: 'RUNNING', taskTemplateId: 't1' } as any)
    );
    runService.getApiV2EvaluationByEvaluationIdInfo.and.returnValue(
      of({ name: 'Test Eval' } as any)
    );

    TestBed.configureTestingModule({
      declarations: [RunViewerComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: WebSocketService,  useValue: wsService },
        { provide: EvaluationService, useValue: runService },
        { provide: ActivatedRoute,    useValue: { params: of({ runId: 'eval-1' }), paramMap: of({ params: { runId: 'eval-1' }, get: () => 'eval-1' }) } },
        { provide: Router,            useValue: jasmine.createSpyObj('Router', ['navigate'], { url: '/viewer/eval-1' }) },
        { provide: AppConfig,         useValue: { webSocketUrl: 'ws://localhost:8080/api/ws/run' } },
        { provide: MatSnackBar,       useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: Title,             useValue: jasmine.createSpyObj('Title', ['setTitle']) },
        { provide: Overlay,           useValue: {} },
        { provide: 'DOCUMENT',        useValue: document },
      ],
    });

    const fixture = TestBed.createComponent(RunViewerComponent);
    component = fixture.componentInstance;
  });

  // ── connect / disconnect ───────────────────────────────────────────────────

  it('connects the WebSocket with the evaluationId on init', () => {
    component.ngOnInit();
    expect(wsService.connect).toHaveBeenCalledWith('eval-1');
  });

  it('disconnects the WebSocket on destroy', () => {
    component.ngOnInit();
    component.ngOnDestroy();
    expect(wsService.disconnect).toHaveBeenCalled();
  });

  // ── state refresh on WS message ────────────────────────────────────────────

  REFRESH_TYPES.forEach((type) => {
    it(`triggers a state fetch when ${type} message is received`, fakeAsync(() => {
      component.ngOnInit();
      runService.getApiV2EvaluationByEvaluationIdState.calls.reset();

      // Subscribe to state so the observable is active
      component.state.subscribe();

      messages$.next(wsMsg(type));
      tick();

      expect(runService.getApiV2EvaluationByEvaluationIdState).toHaveBeenCalledWith('eval-1');
    }));
  });

  // ── PING is ignored ────────────────────────────────────────────────────────

  IRRELEVANT_TYPES.forEach((type) => {
    it(`does not trigger a state fetch for ${type} message`, fakeAsync(() => {
      component.ngOnInit();
      component.state.subscribe();
      runService.getApiV2EvaluationByEvaluationIdState.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(runService.getApiV2EvaluationByEvaluationIdState).not.toHaveBeenCalled();
    }));
  });

  // ── no polling ─────────────────────────────────────────────────────────────

  it('does not fetch state on a timer when no WS message arrives', fakeAsync(() => {
    component.ngOnInit();
    component.state.subscribe();
    runService.getApiV2EvaluationByEvaluationIdState.calls.reset();

    tick(5000); // advance time — no interval should fire

    expect(runService.getApiV2EvaluationByEvaluationIdState).not.toHaveBeenCalled();
  }));
});
