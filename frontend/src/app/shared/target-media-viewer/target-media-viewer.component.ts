import { Component, Input } from "@angular/core";
import { ApiTarget } from "../../../../openapi";

@Component({
  selector: 'app-target-media-viewer',
  templateUrl: './target-media-viewer.component.html',
  styleUrls: ['./target-media-viewer.component.scss']
})
export class TargetMediaViewerComponent {


  @Input()
  public target: ApiTarget
}
