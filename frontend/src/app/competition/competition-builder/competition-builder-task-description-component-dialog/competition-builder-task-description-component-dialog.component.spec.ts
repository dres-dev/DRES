import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CompetitionBuilderTaskDescriptionComponentDialogComponent } from './competition-builder-task-description-component-dialog.component';

describe('CompetitionBuilderTaskDescriptionComponentDialogComponent', () => {
  let component: CompetitionBuilderTaskDescriptionComponentDialogComponent;
  let fixture: ComponentFixture<CompetitionBuilderTaskDescriptionComponentDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CompetitionBuilderTaskDescriptionComponentDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CompetitionBuilderTaskDescriptionComponentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
