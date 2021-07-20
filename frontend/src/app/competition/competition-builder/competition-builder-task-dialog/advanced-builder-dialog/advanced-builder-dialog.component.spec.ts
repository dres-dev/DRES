import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {AdvancedBuilderDialogComponent} from './advanced-builder-dialog.component';

describe('AdvancedBuilderDialogComponent', () => {
  let component: AdvancedBuilderDialogComponent;
  let fixture: ComponentFixture<AdvancedBuilderDialogComponent>;

  beforeEach(waitForAsync(() => {
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
