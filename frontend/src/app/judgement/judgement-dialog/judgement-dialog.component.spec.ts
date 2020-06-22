import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { JudgementDialogComponent } from './judgement-dialog.component';

describe('JudgementWelcomeDialogComponent', () => {
  let component: JudgementDialogComponent;
  let fixture: ComponentFixture<JudgementDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ JudgementDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(JudgementDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
