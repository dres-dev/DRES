import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AdvancedBuilderDialogComponent } from './advanced-builder-dialog.component';

describe('AdvancedBuilderDialogComponent', () => {
  let component: AdvancedBuilderDialogComponent;
  let fixture: ComponentFixture<AdvancedBuilderDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AdvancedBuilderDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedBuilderDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
