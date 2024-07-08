import { Component, ElementRef, Inject, ViewChild } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { ApiMediaItem, CollectionService } from "../../../../../../openapi";

export interface BatchAddTargetDialogData{
  collectionId: string;
}

@Component({
  selector: 'app-batch-add-target-dialog',
  templateUrl: './batch-add-target-dialog.component.html',
  styleUrls: ['./batch-add-target-dialog.component.scss']
})
export class BatchAddTargetDialogComponent {

  @ViewChild('targetArea') textArea: ElementRef<HTMLTextAreaElement>

  private targets: string[] = [];

  private failedNames: string[] = [];

  constructor(
    private dialogRef: MatDialogRef<BatchAddTargetDialogComponent>,
    private mediaService: CollectionService,
    @Inject(MAT_DIALOG_DATA) private data: BatchAddTargetDialogData
  ){

  }

  processUpload(event){
    const file = event.target.files[0];
    const reader = new FileReader();
    reader.readAsText(file);
    reader.onload = () =>{
      const text = (reader.result as string)
      this.textArea.nativeElement.value = text;
    }
  }

  processInput(event){

  }

  close(){
    this.dialogRef.close(null)
  }

  save(){
    /* Sanitation: break up on newline and trim each line */
    const lines = this.textArea.nativeElement.value?.split('\n')
    this.dialogRef.close(lines.map(it => it.trim()))
  }

}
