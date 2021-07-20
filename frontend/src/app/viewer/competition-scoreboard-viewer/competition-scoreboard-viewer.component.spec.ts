import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {CompetitionScoreboardViewerComponent} from './competition-scoreboard-viewer.component';

describe('CompetitionScoreboardViewerComponent', () => {
  let component: CompetitionScoreboardViewerComponent;
  let fixture: ComponentFixture<CompetitionScoreboardViewerComponent>;

  beforeEach(waitForAsync(() => {
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
