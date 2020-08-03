import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CollectionViewerComponent } from './collection-viewer.component';

describe('CollectionViewerComponent', () => {
  let component: CollectionViewerComponent;
  let fixture: ComponentFixture<CollectionViewerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CollectionViewerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
