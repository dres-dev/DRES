import { Component, Input } from "@angular/core";
import {
  QueryDescriptionExternalFormFieldComponent
} from "../query-description-external-form-field/query-description-external-form-field.component";
import {
  TaskTemplateFormBuilder
} from "../../task-template-form.builder";
import { AppConfig } from "../../../../app.config";

@Component({
  selector: 'app-query-description-external-image-form-field',
  templateUrl: './query-description-external-image-form-field.component.html',
  styleUrls: ['./query-description-external-image-form-field.component.scss']
})
export class QueryDescriptionExternalImageFormFieldComponent extends QueryDescriptionExternalFormFieldComponent{

}
