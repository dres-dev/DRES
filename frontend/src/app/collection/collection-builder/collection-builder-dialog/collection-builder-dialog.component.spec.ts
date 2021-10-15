import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {CollectionBuilderDialogComponent} from './collection-builder-dialog.component';

describe('CollectionBuilderDialogComponent', () => {
  let component: CollectionBuilderDialogComponent;
  let fixture: ComponentFixture<CollectionBuilderDialogComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ CollectionBuilderDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionBuilderDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
