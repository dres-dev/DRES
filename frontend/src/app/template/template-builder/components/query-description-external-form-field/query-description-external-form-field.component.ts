import { Component, Input } from "@angular/core";
import { UntypedFormControl } from "@angular/forms";
import {
  CompetitionFormBuilder
} from "../../../../competition/competition-builder/competition-builder-task-dialog/competition-form.builder";
import { AppConfig } from "../../../../app.config";

@Component({
  selector: 'app-query-description-external-form-field',
  templateUrl: './query-description-external-form-field.component.html',
  styleUrls: ['./query-description-external-form-field.component.scss']
})
export class QueryDescriptionExternalFormFieldComponent {
  @Input()
  pathControl: UntypedFormControl
  @Input()
  formBuilder: CompetitionFormBuilder
  @Input()
  index: number

  showing: boolean = false;

  constructor(public config: AppConfig) {
  }
}
