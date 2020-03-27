import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CompetitionBuilerComponent } from './competition-builer.component';

describe('CompetitionBuilerComponent', () => {
  let component: CompetitionBuilerComponent;
  let fixture: ComponentFixture<CompetitionBuilerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CompetitionBuilerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CompetitionBuilerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
