import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CompetitionScoreboardViewerComponent } from './competition-scoreboard-viewer.component';

describe('CompetitionScoreboardViewerComponent', () => {
  let component: CompetitionScoreboardViewerComponent;
  let fixture: ComponentFixture<CompetitionScoreboardViewerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CompetitionScoreboardViewerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CompetitionScoreboardViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
