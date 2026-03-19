import { Component, Input } from "@angular/core";
import { UntypedFormControl } from "@angular/forms";
import {
  TaskTemplateFormBuilder
} from "../../task-template-form.builder";
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
  formBuilder: TaskTemplateFormBuilder
  @Input()
  index: number

  showing: boolean = false;

  constructor(public config: AppConfig) {
  }
}
