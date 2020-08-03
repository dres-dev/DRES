import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CollectionBuilderDialogComponent } from './collection-builder-dialog.component';

describe('CollectionBuilderDialogComponent', () => {
  let component: CollectionBuilderDialogComponent;
  let fixture: ComponentFixture<CollectionBuilderDialogComponent>;

  beforeEach(async(() => {
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
