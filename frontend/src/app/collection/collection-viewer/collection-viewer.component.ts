import {AfterViewInit, Component, OnInit} from '@angular/core';
import {CollectionService} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-collection-viewer',
  templateUrl: './collection-viewer.component.html',
  styleUrls: ['./collection-viewer.component.scss']
})
export class CollectionViewerComponent implements OnInit {

  collectionId: Observable<string>;

  constructor(
      private collectionService: CollectionService,
      private activeRoute: ActivatedRoute,
      private snackBar: MatSnackBar,
      private router: Router,
      private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.collectionId = this.activeRoute.params.pipe(map(p => p.collectionId));
  }



}
