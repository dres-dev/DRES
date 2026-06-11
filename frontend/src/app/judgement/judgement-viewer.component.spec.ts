import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';
import { JudgementViewerComponent } from './judgement-viewer.component';
import { JudgementService } from '../../../openapi';
import { ApiJudgementRequest } from '../../../openapi';

function makeRequest(token: string, desc = 'Find a cat'): ApiJudgementRequest {
  return { token, taskDescription: desc, validator: 'v1', answerSet: { answers: [] } } as any;
}

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
