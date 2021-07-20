import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {CompetitionListComponent} from './competition-list.component';

describe('CompetitionListComponent', () => {
  let component: CompetitionListComponent;
  let fixture: ComponentFixture<CompetitionListComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ CompetitionListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CompetitionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
