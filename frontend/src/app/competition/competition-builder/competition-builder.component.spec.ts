import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { CompetitionBuilderComponent } from './competition-builder.component';

describe('CompetitionBuilerComponent', () => {
  let component: CompetitionBuilderComponent;
  let fixture: ComponentFixture<CompetitionBuilderComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [CompetitionBuilderComponent],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CompetitionBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
