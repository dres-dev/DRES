import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { AdminUserCreateOrEditDialogComponent } from '../admin-user-create-or-edit-dialog/admin-user-create-or-edit-dialog.component';
import { filter } from 'rxjs/operators';
import { MatSort, Sort } from '@angular/material/sort';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { MatTableDataSource } from '@angular/material/table';
import { ApiUser, ApiUserRequest, UserService } from "../../../../openapi";
import {mergeMap} from 'rxjs';

@Component({
  selector: 'app-admin-user-list',
  templateUrl: './admin-user-list.component.html',
  styleUrls: ['./admin-user-list.component.scss'],
})
export class AdminUserListComponent implements AfterViewInit {
  // TODO Add Team info / link

  displayColumns = ['actions', 'id', 'name', 'role'];

  @ViewChild(MatSort) sort: MatSort;
  dataSource = new MatTableDataSource<ApiUser>([]);


  shouldDisplayFilter = false;
  filterValue = '';

  constructor(
    private snackBar: MatSnackBar,
    private userService: UserService,
    private dialog: MatDialog,
    private liveAnnouncer: LiveAnnouncer
  ) {}

  public create() {
    const dialogRef = this.dialog.open(AdminUserCreateOrEditDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        mergeMap((u: ApiUserRequest) => {
          return this.userService.postApiV2User(u);
        })
      )
      .subscribe({
        next: (r) => {
          this.refresh();
          this.snackBar.open(`Successfully created ${r.username}`, null, {duration: 5000});
        },
        error: (err) => {
          this.snackBar.open(`Error: ${err.error.description}`, null, {duration: 5000});
        }
      });
  }

  public edit(user: ApiUser) {
    const dialogRef = this.dialog.open(AdminUserCreateOrEditDialogComponent, { width: '500px', data: user as ApiUser });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        mergeMap((u: ApiUserRequest) => {
          console.debug(`Edit Result: ${u}`);
          return this.userService.patchApiV2UserByUserId(user.id, u);
        })
      )
      .subscribe({
        next: (r) => {
          this.refresh();
          this.snackBar.open(`Successfully updated ${r.username}`, null, {duration: 5000});
        },
        error: (err) => {
          this.snackBar.open(`Error: ${err.error.description}`, null, {duration: 5000});
        }
      });
  }

  public delete(userId: string) {
    if (confirm(`Do you really want to delete user (${userId})?`)) {
      this.userService.deleteApiV2UserByUserId(userId).subscribe({
        next: (u: ApiUser) => {
          this.refresh();
          this.snackBar.open(`Success: ${u.username} (${u.id}) deleted`, null, {duration: 5000});
        },
        error: (err) => {
          this.snackBar.open(`Error: ${err.error.description}`, null, {duration: 5000});
        }
      });
    }
  }

  public refresh() {
    this.userService.getApiV2UserList().subscribe({
      next: (users: ApiUser[]) => {
        this.dataSource.data = users;
        this.dataSource.sort = this.sort;
      },
      error: (error) => {
        this.dataSource.data = [];
        this.dataSource.sort = this.sort;
        this.snackBar.open(`Error: ${error.error.description}`, null, {duration: 5000});
      }
    });
  }

  ngAfterViewInit(): void {
    this.refresh();
  }

  resolveUserById(_: number, user: ApiUser) {
    return user.id;
  }

  filter() {
    this.dataSource.filter = this.filterValue.trim(); // Purposely case insensitive
  }

  private findForId(id: string) {
    this.dataSource.data.forEach((u) => {
      if (u.id === id) {
        return u;
      }
    });
    return null;
  }

  /**
   * Announce sort change state for assistive technology.
   * Direct adoption from the angular material docs.
   * We only support English everywhere, thus these announcements are in English too.
   */
  announceSortChangeForAccessibility($event: Sort) {
    if ($event.direction) {
      this.liveAnnouncer.announce(`Sorted ${$event.direction}ending on column ${$event.active}`);
    } else {
      this.liveAnnouncer.announce('Sorting cleared0');
    }
  }
}
