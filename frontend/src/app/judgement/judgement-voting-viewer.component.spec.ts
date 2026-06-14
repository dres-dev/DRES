import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, Subject } from 'rxjs';

import { JudgementVotingViewerComponent } from './judgement-voting-viewer.component';
import { AppConfig } from '../app.config';
import { WebSocketService } from '../services/websocket.service';
import { JudgementService } from '../../../openapi';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

// ── Helpers ───────────────────────────────────────────────────────────────────

function wsMsg(type: ServerMessageType.ServerMessageTypeEnum): IWsServerMessage {
  return { evaluationId: 'eval-1', type, timestamp: Date.now() };
}

const REFRESH_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
  ServerMessageType.ServerMessageTypeEnum.TASK_START,
  ServerMessageType.ServerMessageTypeEnum.TASK_END,
];

const IRRELEVANT_TYPES: ServerMessageType.ServerMessageTypeEnum[] = [
  ServerMessageType.ServerMessageTypeEnum.PING,
  ServerMessageType.ServerMessageTypeEnum.COMPETITION_UPDATE,
];

// ── Suite ─────────────────────────────────────────────────────────────────────

describe('JudgementVotingViewerComponent WebSocket wiring', () => {
  let component: JudgementVotingViewerComponent;
  let wsService: jasmine.SpyObj<WebSocketService>;
  let judgementService: jasmine.SpyObj<JudgementService>;
  let messages$: Subject<IWsServerMessage>;
  let fixture: ComponentFixture<JudgementVotingViewerComponent>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();

    wsService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect'], {
      messages$: messages$.asObservable(),
    });

    judgementService = jasmine.createSpyObj('JudgementService', ['getApiV2EvaluationByEvaluationIdVoteNext']);
    judgementService.getApiV2EvaluationByEvaluationIdVoteNext.and.returnValue(of({ status: 202, body: null } as any));

    TestBed.configureTestingModule({
      declarations: [JudgementVotingViewerComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: JudgementService, useValue: judgementService },
        { provide: ActivatedRoute, useValue: { params: of({ runId: 'eval-1' }) } },
        { provide: AppConfig, useValue: { resolveUrl: (p: string) => `http://localhost/${p}` } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: WebSocketService, useValue: wsService },
      ],
    });

    fixture = TestBed.createComponent(JudgementVotingViewerComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    (component as any).requestSub?.unsubscribe();
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

  // ── vote request refresh on WS message ─────────────────────────────────────

  REFRESH_TYPES.forEach((type) => {
    it(`fetches the next vote request when ${type} message is received`, fakeAsync(() => {
      component.ngOnInit();
      judgementService.getApiV2EvaluationByEvaluationIdVoteNext.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(judgementService.getApiV2EvaluationByEvaluationIdVoteNext.calls.mostRecent().args).toEqual(['eval-1', 'response']);

      discardPeriodicTasks();
    }));
  });

  // ── PING / unrelated messages are ignored ──────────────────────────────────

  IRRELEVANT_TYPES.forEach((type) => {
    it(`does not fetch the next vote request for ${type} message`, fakeAsync(() => {
      component.ngOnInit();
      tick(); // consume the initial fallback-poll emission
      judgementService.getApiV2EvaluationByEvaluationIdVoteNext.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(judgementService.getApiV2EvaluationByEvaluationIdVoteNext).not.toHaveBeenCalled();

      discardPeriodicTasks();
    }));
  });
});
