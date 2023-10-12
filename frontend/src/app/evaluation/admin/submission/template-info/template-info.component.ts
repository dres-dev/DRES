import { AfterViewInit, Component, Input } from "@angular/core";
import { ApiTarget, ApiTaskTemplate } from "../../../../../../openapi";

@Component({
  selector: 'app-template-info',
  templateUrl: './template-info.component.html',
  styleUrls: ['./template-info.component.scss']
})
export class TemplateInfoComponent implements AfterViewInit{

  public shownElement: ApiTarget;

  @Input()
  public template: ApiTaskTemplate;

  ngAfterViewInit(): void {
  }

}
