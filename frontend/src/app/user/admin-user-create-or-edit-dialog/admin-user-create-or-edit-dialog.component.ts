import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup } from '@angular/forms';
import {ApiRole, ApiUser, UserRequest} from '../../../../openapi';

@Component({
  selector: 'app-admin-user-create-or-edit-dialog',
  templateUrl: './admin-user-create-or-edit-dialog.component.html',
  styleUrls: ['./admin-user-create-or-edit-dialog.component.scss'],
})
export class AdminUserCreateOrEditDialogComponent {
  form: FormGroup = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
    role: new FormControl(''),
  });

  roles = [ApiRole.ADMIN, ApiRole.JUDGE, ApiRole.PARTICIPANT, ApiRole.VIEWER];

  defaultRole: ApiRole = ApiRole.VIEWER;

  constructor(
    public dialogRef: MatDialogRef<AdminUserCreateOrEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public usr?: ApiUser
  ) {
    this.init();
  }

  private init() {
    if (this.isEdit()) {
      console.log('User.Edit');
      this.form.controls.username.setValue(this.usr.username);
      this.form.controls.role.setValue(this.usr.role);
    } else {
      console.log('User.Create');
    }
  }

  downloadProvider = () => this.asJson();

  nameProvider = () => (this.fetchData()?.username ? this.fetchData().username + '.json' : 'user-download.json');

  public isEdit() {
    return this.usr != null;
  }

  uploaded = (userData: string) => {
    this.usr = JSON.parse(userData) as ApiUser;
    this.init();
  };

  public create(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.fetchData());
    }
  }

  fetchData(): UserRequest {
    return {
      username: this.form.controls.username.value,
      password: this.form.controls.password.value,
      role: this.form.controls.role.value,
    } as UserRequest;
  }

  asJson(): string {
    const user = this.fetchData();
    return JSON.stringify({ id: this?.usr?.id, username: user.username, role: user.role } as ApiUser);
  }

  public close(): void {
    this.dialogRef.close(null);
  }
}
