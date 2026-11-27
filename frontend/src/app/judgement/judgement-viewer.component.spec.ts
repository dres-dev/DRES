import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { EMPTY, Subject, merge, of } from 'rxjs';
import { JudgementViewerComponent } from './judgement-viewer.component';
import { JudgementService } from '../../../openapi';
import { ApiJudgementRequest } from '../../../openapi';
import { WebSocketService } from '../services/websocket.service';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { ServerMessageType } from '../model/ws/server-message-type.enum';

/**
 * Double for the WebSocket service: exposes only the contract this component uses and never polls on
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

function makeRequest(token: string, desc = 'Find a cat'): ApiJudgementRequest {
  return { token, taskDescription: desc, validator: 'v1', answerSet: { answers: [] } } as any;
}

function wsMsg(type: ServerMessageType.ServerMessageTypeEnum): IWsServerMessage {
  return { evaluationId: 'run1', type, timestamp: Date.now() };
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

describe('JudgementViewerComponent', () => {
  let component: JudgementViewerComponent;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockJudgementService: jasmine.SpyObj<JudgementService>;
  let dialogAfterClosed: Subject<void>;

  beforeEach(() => {
    dialogAfterClosed = new Subject<void>();
    const fakeDialogRef = { afterClosed: () => dialogAfterClosed.asObservable() } as MatDialogRef<any>;
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockDialog.open.and.returnValue(fakeDialogRef);
    mockJudgementService = jasmine.createSpyObj('JudgementService', [
      'postApiV2EvaluationByEvaluationIdJudge',
      'getApiV2EvaluationByEvaluationIdJudgeNext',
      'getApiV2EvaluationByEvaluationIdJudgeStatus',
    ]);

    TestBed.configureTestingModule({
      declarations: [JudgementViewerComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: JudgementService, useValue: mockJudgementService },
        { provide: ActivatedRoute, useValue: { params: of({ runId: 'run1' }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: MatDialog, useValue: mockDialog },
        {
          provide: WebSocketService,
          useValue: silentWebSocketService(),
        },
      ],
    });

    const fixture = TestBed.createComponent(JudgementViewerComponent);
    component = fixture.componentInstance;

    component.judgePlayer = jasmine.createSpyObj('JudgementMediaViewerComponent', ['stop', 'togglePlaying']);
  });

  // --- goBack ---

  describe('goBack', () => {
    it('restores the previous request and makes judgment available', () => {
      const req = makeRequest('tok1');
      component.previousJudgementRequest = req;
      component.isJudgmentAvailable = false;

      component.goBack();

      expect(component.judgementRequest).toBe(req);
      expect(component.isJudgmentAvailable).toBeTrue();
    });

    it('clears previousJudgementRequest after going back', () => {
      component.previousJudgementRequest = makeRequest('tok1');
      component.goBack();
      expect(component.previousJudgementRequest).toBeNull();
    });

    it('does nothing when there is no previous request', () => {
      component.previousJudgementRequest = null;
      component.isJudgmentAvailable = false;
      component.goBack();
      expect(component.isJudgmentAvailable).toBeFalse();
      expect(component.judgementRequest).toBeUndefined();
    });

    it('emits the restored request to observableJudgementRequest', () => {
      const req = makeRequest('tok2');
      component.previousJudgementRequest = req;
      let emitted: ApiJudgementRequest | null = null;
      component.observableJudgementRequest.subscribe((r) => (emitted = r));

      component.goBack();

      expect(emitted).toBe(req);
    });
  });

  // --- judge() saves previous request ---

  describe('judge', () => {
    it('saves the current request as previousJudgementRequest before clearing', () => {
      const req = makeRequest('tok3');
      component.judgementRequest = req;
      mockJudgementService.postApiV2EvaluationByEvaluationIdJudge.and.returnValue(of({ description: 'ok' } as any));
      component['runId'] = of('run1');

      component.judge('CORRECT');

      expect(component.previousJudgementRequest).toBe(req);
      expect(component.observableJudgementRequest.value).toBeNull();
      expect(component.judgementRequest).toBeNull();
      expect(component.isJudgmentAvailable).toBeFalse();
    });
  });

  // --- spacebar ---

  describe('handleKeydown', () => {
    it('calls togglePlaying on spacebar', () => {
      const event = new KeyboardEvent('keydown', { code: 'Space' });
      spyOn(event, 'preventDefault');

      component.handleKeydown(event);

      expect(component.judgePlayer.togglePlaying as jasmine.Spy).toHaveBeenCalled();
      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('does not call togglePlaying for other keys', () => {
      const event = new KeyboardEvent('keydown', { code: 'KeyA' });
      component.handleKeydown(event);
      expect(component.judgePlayer.togglePlaying as jasmine.Spy).not.toHaveBeenCalled();
    });

    it('does not crash when judgePlayer is null', () => {
      component.judgePlayer = null;
      const event = new KeyboardEvent('keydown', { code: 'Space' });
      expect(() => component.handleKeydown(event)).not.toThrow();
    });
  });
});

// ── WebSocket wiring ────────────────────────────────────────────────────────

describe('JudgementViewerComponent WebSocket wiring', () => {
  let component: JudgementViewerComponent;
  let fixture: ComponentFixture<JudgementViewerComponent>;
  let wsService: any;
  let judgementService: jasmine.SpyObj<JudgementService>;
  let messages$: Subject<IWsServerMessage>;
  let dialogAfterClosed: Subject<void>;

  beforeEach(() => {
    messages$ = new Subject<IWsServerMessage>();
    dialogAfterClosed = new Subject<void>();

    wsService = silentWebSocketService(messages$);

    judgementService = jasmine.createSpyObj('JudgementService', [
      'postApiV2EvaluationByEvaluationIdJudge',
      'getApiV2EvaluationByEvaluationIdJudgeNext',
      'getApiV2EvaluationByEvaluationIdJudgeStatus',
    ]);
    judgementService.getApiV2EvaluationByEvaluationIdJudgeStatus.and.returnValue(of([] as any));
    judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.and.returnValue(of({ status: 202, body: null } as any));

    const fakeDialogRef = { afterClosed: () => dialogAfterClosed.asObservable() } as MatDialogRef<any>;
    const mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockDialog.open.and.returnValue(fakeDialogRef);

    TestBed.configureTestingModule({
      declarations: [JudgementViewerComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: JudgementService, useValue: judgementService },
        { provide: ActivatedRoute, useValue: { params: of({ runId: 'run1' }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: MatDialog, useValue: mockDialog },
        { provide: WebSocketService, useValue: wsService },
      ],
    });

    fixture = TestBed.createComponent(JudgementViewerComponent);
    component = fixture.componentInstance;
    component.judgePlayer = jasmine.createSpyObj('JudgementMediaViewerComponent', ['stop', 'togglePlaying']);
  });

  // ── connect / disconnect ───────────────────────────────────────────────────

  it('connects the WebSocket with the evaluationId on init', () => {
    component.ngAfterViewInit();
    expect(wsService.connect).toHaveBeenCalledWith('run1');
  });

  it('disconnects the WebSocket on destroy', () => {
    component.ngAfterViewInit();
    fixture.destroy();
    expect(wsService.disconnect).toHaveBeenCalled();
  });

  // ── judgement request / status refresh on WS message ───────────────────────

  REFRESH_TYPES.forEach((type) => {
    it(`fetches the next judgement request and status when ${type} message is received`, fakeAsync(() => {
      component.ngAfterViewInit();
      dialogAfterClosed.next();
      tick();

      judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.calls.reset();
      judgementService.getApiV2EvaluationByEvaluationIdJudgeStatus.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.calls.mostRecent().args).toEqual(['run1', 'response']);
      expect(judgementService.getApiV2EvaluationByEvaluationIdJudgeStatus).toHaveBeenCalledWith('run1');

      discardPeriodicTasks();
    }));
  });

  it('fetches the next judgement immediately after a verdict is accepted', fakeAsync(() => {
    component.ngAfterViewInit();
    dialogAfterClosed.next();
    tick(); // initial immediate poll

    const postResponse = new Subject<any>();
    judgementService.postApiV2EvaluationByEvaluationIdJudge.and.returnValue(postResponse);
    judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.and.returnValue(
      of({ status: 200, body: makeRequest('next') } as any)
    );
    judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.calls.reset();
    component.judgementRequest = makeRequest('current');
    component.isJudgmentAvailable = true;

    component.judge('CORRECT');
    expect(judgementService.getApiV2EvaluationByEvaluationIdJudgeNext).not.toHaveBeenCalled();

    postResponse.next({ description: 'accepted' });
    postResponse.complete();
    tick();

    expect(judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.calls.mostRecent().args).toEqual(['run1', 'response']);
    discardPeriodicTasks();
  }));

  // ── PING / unrelated messages are ignored ──────────────────────────────────

  IRRELEVANT_TYPES.forEach((type) => {
    it(`does not fetch the next judgement request for ${type} message`, fakeAsync(() => {
      component.ngAfterViewInit();
      dialogAfterClosed.next();
      tick();

      judgementService.getApiV2EvaluationByEvaluationIdJudgeNext.calls.reset();
      judgementService.getApiV2EvaluationByEvaluationIdJudgeStatus.calls.reset();

      messages$.next(wsMsg(type));
      tick();

      expect(judgementService.getApiV2EvaluationByEvaluationIdJudgeNext).not.toHaveBeenCalled();
      expect(judgementService.getApiV2EvaluationByEvaluationIdJudgeStatus).not.toHaveBeenCalled();

      discardPeriodicTasks();
    }));
  });
});
