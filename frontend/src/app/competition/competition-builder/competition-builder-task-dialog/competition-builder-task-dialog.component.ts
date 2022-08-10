import { Component, ElementRef, Inject, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogConfig, MatDialogRef } from '@angular/material/dialog';
import {
  CollectionService,
  ConfiguredOptionQueryComponentOption,
  ConfiguredOptionTargetOption,
  RestMediaCollection,
  RestMediaItem,
  RestTaskDescription,
  RestTemporalPoint,
  RestTemporalRange,
  TaskGroup,
  TaskType,
} from '../../../../../openapi';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { filter, first } from 'rxjs/operators';
import { AppConfig } from '../../../app.config';
import { CompetitionFormBuilder } from './competition-form.builder';
import { VideoPlayerSegmentBuilderData } from './video-player-segment-builder/video-player-segment-builder.component';
import {
  AdvancedBuilderDialogComponent,
  AdvancedBuilderDialogData,
} from './advanced-builder-dialog/advanced-builder-dialog.component';
import { TimeUtilities } from '../../../utilities/time.utilities';
import {EditableTaskComponent} from '../components/editable-task/editable-task.component';

/**
 * Its expected that the taskGroup and taskType properties are correctly given
 * even in the case this is 'edit'!
 */
export interface CompetitionBuilderTaskDialogData {
  taskGroup: TaskGroup;
  taskType: TaskType;
  task?: RestTaskDescription;
}

@Component({
  selector: 'app-competition-builder-task-dialog',
  templateUrl: './competition-builder-task-dialog.component.html',
  styleUrls: ['./competition-builder-task-dialog.component.scss'],
})
export class CompetitionBuilderTaskDialogComponent {

  @ViewChild('taskEditor')
  taskEditor: EditableTaskComponent;

  constructor(
      @Inject(MAT_DIALOG_DATA) public data: CompetitionBuilderTaskDialogData,
    public dialogRef: MatDialogRef<CompetitionBuilderTaskDialogComponent>
  ) {
  }

  uploaded = (taskData: string) => {
    console.log('Not implemented yet')
    /*
    const task = JSON.parse(taskData) as RestTaskDescription;
    this.builder = new CompetitionFormBuilder(this.data.taskGroup, this.data.taskType, this.collectionService, task);
    this.form = this.builder.form;
    console.log('Loaded task: ' + JSON.stringify(task));*/
  };


  /**
   * Handler for 'save' button.
   */
  public save() {
    if (this.taskEditor.isFormValid()) {
      this.dialogRef.close(this.taskEditor.fetchData());
    }
  }

  /**
   * The form data as json
   */
  asJson(): string {
    return JSON.stringify(this.taskEditor.fetchData());
  }

  fileProvider = () => (this.taskEditor.fetchData()?.name ? this.taskEditor.fetchData().name : 'task-download.json');

  downloadProvider = () => this.asJson();


  /**
   * Handler for 'close' button.
   */
  public close(): void {
    this.dialogRef.close(null);
  }

}
