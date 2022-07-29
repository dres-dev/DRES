import {AfterViewInit, Component, Inject, OnInit, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {TaskType} from '../../../../../openapi';
import {TaskTypesComponent} from '../components/task-types/task-types.component';

/**
 * Wrapper to be able to have an enum value boolean tuple
 */
interface ActivatedType<T> {
  type: T;
  activated: boolean;
}

@Component({
  selector: 'app-competition-builder-task-type',
  templateUrl: './competition-builder-task-type-dialog.component.html',
  styleUrls: ['./competition-builder-task-type-dialog.component.scss'],
})
export class CompetitionBuilderTaskTypeDialogComponent implements OnInit, AfterViewInit {

    @ViewChild('content', {static: true}) comp: TaskTypesComponent;

    constructor(
        public dialogRef: MatDialogRef<CompetitionBuilderTaskTypeDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: TaskType) {
    }

  ngAfterViewInit(): void {}

    save(): void {
        if (this.comp.valid) {
            this.dialogRef.close(this.comp.fetchFromForm());
        }
    }

  close(): void {
    this.dialogRef.close(null);
  }

    fileProvider = () => this.comp.fileProvider();

    downloadProvider = () => this.comp.downloadProvider();

  import(): void {
    // TODO
  }

    ngOnInit(): void {
    }
}
