import {AfterViewInit, Component} from '@angular/core';
import {MatSnackBar} from '@angular/material/snack-bar';
import {UserDetails, UserRequest, UserService} from '../../../../openapi';
import {MatDialog} from '@angular/material/dialog';
import {AdminUserCreateOrEditDialogComponent} from '../admin-user-create-or-edit-dialog/admin-user-create-or-edit-dialog.component';
import {filter, flatMap} from 'rxjs/operators';

@Component({
  selector: 'app-admin-user-list',
  templateUrl: './admin-user-list.component.html',
  styleUrls: ['./admin-user-list.component.scss']
})
export class AdminUserListComponent implements AfterViewInit {

  // TODO Add Team info / link

  displayColumns = ['actions', 'id', 'name', 'role'];
  users: UserDetails[] = [];

  constructor(
      private snackBar: MatSnackBar,
      private userService: UserService,
      private dialog: MatDialog
  ) {
  }


  public create() {
    const dialogRef = this.dialog.open(AdminUserCreateOrEditDialogComponent, {width: '500px'});
    dialogRef.afterClosed().pipe(
        filter(r => r != null),
        flatMap((u: UserRequest) => {
          return this.userService.postApiUserCreate(u);
        })
    ).subscribe((r) => {
      this.refresh();
      this.snackBar.open(`Successfully created ${r.username}`, null, {duration: 5000});
    }, (err) => {
      this.snackBar.open(`Error: ${err.error.description}`, null, {duration: 5000});
    });
  }

  public edit(user: UserDetails) {
    const dialogRef = this.dialog.open(AdminUserCreateOrEditDialogComponent, {width: '500px', data: user as UserDetails});
    dialogRef.afterClosed().pipe(
        filter(r => r != null),
        flatMap((u: UserRequest) => {
          console.debug(`Edit Result: ${u}`);
          return this.userService.patchApiUserWithId(user.id, u);
        })
    ).subscribe((r) => {
      this.refresh();
      this.snackBar.open(`Successfully updated ${r.username}`, null, {duration: 5000});
    }, (err) => {
      this.snackBar.open(`Error: ${err.error.description}`, null, {duration: 5000});
    });
  }

  public delete(userId: number) {
    if (confirm(`Do you really want to delete user (${userId})?`)) {
      this.userService.deleteApiUserWithId(userId).subscribe((u: UserDetails) => {
            this.refresh();
            this.snackBar.open(`Success: ${u.username} (${u.id}) deleted`, null, {duration: 5000});
          },
          (err) => {
            this.snackBar.open(`Error: ${err.error.description}`, null, {duration: 5000});
          }
      );
    }
  }

  public refresh() {
    this.userService.getApiUserList().subscribe((users: UserDetails[]) => {
          this.users = users;
        },
        (error) => {
          this.users = [];
          this.snackBar.open(`Error: ${error.error.description}`, null, {duration: 5000});
        });
  }

  ngAfterViewInit(): void {
    this.refresh();
  }

  private findForId(id: number) {
    this.users.forEach((u) => {
      if (u.id === id) {
        return u;
      }
    });
    return null;
  }


}
