import { Component, Inject, ViewChild } from "@angular/core";
import { ApiEvaluationTemplate } from "../../../../../../openapi";
import { TemplateImportTreeBranch, TemplateImportTreeComponent } from "../template-import-tree/template-import-tree.component";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

export interface TemplateImportDialogData{
  templates: ApiEvaluationTemplate[];
  branches: TemplateImportTreeBranch;
}

@Component({
  selector: 'app-template-import-dialog',
  templateUrl: './template-import-dialog.component.html',
  styleUrls: ['./template-import-dialog.component.scss']
})
export class TemplateImportDialogComponent {

  @ViewChild('templateImportTree',{static: true}) importTree: TemplateImportTreeComponent

  constructor(
    public dialogRef: MatDialogRef<TemplateImportDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TemplateImportDialogData
  ) {
  }

  title(){
    switch(this.data.branches){
      case TemplateImportTreeBranch.NONE:
        return "---NONE -- Programmer's Error---";
      case TemplateImportTreeBranch.TASK_TYPES:
        return "Task Types"
      case TemplateImportTreeBranch.TASK_GROUPS:
        return "Task Groups"
      case TemplateImportTreeBranch.TASK_TEMPLATES:
        return "Task Templates"
      case TemplateImportTreeBranch.TEAMS:
        return "Teams"
      case TemplateImportTreeBranch.TEAM_GROUPS:
        return "Team Groups"
      case TemplateImportTreeBranch.JUDGES:
        return "Judges"
      case TemplateImportTreeBranch.ALL:
        return "Evaluation Templates"
    }
  }

  public save(){
    this.dialogRef.close(this.importTree.getImportTemplate())
  }

  public close(){
    this.dialogRef.close();
  }


}
