import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {MediaItemBuilderDialogComponent} from './media-item-builder-dialog.component';

describe('MediaItemBuilderDialogComponent', () => {
  let component: MediaItemBuilderDialogComponent;
  let fixture: ComponentFixture<MediaItemBuilderDialogComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ MediaItemBuilderDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MediaItemBuilderDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
