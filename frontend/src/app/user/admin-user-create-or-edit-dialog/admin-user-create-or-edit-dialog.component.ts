import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {UserDetails, UserRequest} from '../../../../openapi';
import {FormControl, FormGroup} from '@angular/forms';
import RoleEnum = UserDetails.RoleEnum;

@Component({
    selector: 'app-admin-user-create-or-edit-dialog',
    templateUrl: './admin-user-create-or-edit-dialog.component.html',
    styleUrls: ['./admin-user-create-or-edit-dialog.component.scss']
})
export class AdminUserCreateOrEditDialogComponent {

    form: FormGroup = new FormGroup({
        username: new FormControl(''),
        password: new FormControl(''),
        role: new FormControl('')
    });

    roles = [RoleEnum.ADMIN, RoleEnum.JUDGE, RoleEnum.PARTICIPANT, RoleEnum.VIEWER];


    defaultRole: RoleEnum = RoleEnum.VIEWER;

    constructor(
        public dialogRef: MatDialogRef<AdminUserCreateOrEditDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public usr?: UserDetails
    ) {
        if (this.isEdit()) {
            console.log('User.Edit');
            this.form.controls.username.setValue(this.usr.username);
            this.form.controls.role.setValue(this.usr.role);
        } else {
            console.log('User.Create');
        }
    }

    downloadProvider = () => this.asJson();

    nameProvider = () => this?.usr?.username ? this.usr.username : 'user-download.json';

    public isEdit() {
        return this.usr != null;
    }

    public create(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.fetchData()
            );
        }
    }

    fetchData(): UserRequest {
        return {
            username: this.form.controls.username.value,
            password: this.form.controls.password.value,
            role: this.form.controls.role.value
        } as UserRequest;
    }

    asJson(): string {
        return JSON.stringify(this.fetchData());
    }

    public close(): void {
        this.dialogRef.close(null);
    }
}
