import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminUserCreateOrEditDialogComponent } from './admin-user-create-or-edit-dialog.component';

describe('AdminUserCreateOrEditDialogComponent', () => {
  let component: AdminUserCreateOrEditDialogComponent;
  let fixture: ComponentFixture<AdminUserCreateOrEditDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AdminUserCreateOrEditDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminUserCreateOrEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
