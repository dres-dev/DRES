import {AfterViewInit, Component} from '@angular/core';
import {CollectionService, RestMediaCollection} from '../../../../openapi';
import {Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';

@Component({
    selector: 'app-collection-list',
    templateUrl: './collection-list.component.html',
    styleUrls: ['./collection-list.component.scss']
})
export class CollectionListComponent implements AfterViewInit {

    displayedColumns = ['actions', 'id', 'name', 'description', 'basePath'];
    collections: RestMediaCollection[] = [];
    void;

    constructor(
        private collectionService: CollectionService,
        private routerService: Router,
        private dialog: MatDialog,
        private snackBar: MatSnackBar
    ) {
    }

    refresh() {
        this.collectionService.getApiCollection().subscribe((results: RestMediaCollection[]) => {
            this.collections = results;
        }, (r) => {
          this.collections = [];
          this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
        });
    }

    ngAfterViewInit(): void {
      this.refresh();
    }

    create(){}

    edit(id: string){}

    delete(id: string){}

}
