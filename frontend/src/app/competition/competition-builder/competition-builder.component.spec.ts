import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CompetitionBuilderComponent } from './competition-builder.component';

describe('CompetitionBuilerComponent', () => {
  let component: CompetitionBuilderComponent;
  let fixture: ComponentFixture<CompetitionBuilderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CompetitionBuilderComponent ]
    })
    .compileComponents();
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
