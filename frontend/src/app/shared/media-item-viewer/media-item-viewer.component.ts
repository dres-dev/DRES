import { Component, Input } from "@angular/core";
import { ApiMediaItem, ApiTemporalRange } from "../../../../openapi";
import { AppConfig } from "../../app.config";

@Component({
  selector: 'app-media-item-viewer',
  templateUrl: './media-item-viewer.component.html',
  styleUrls: ['./media-item-viewer.component.scss']
})
export class MediaItemViewerComponent {


  @Input()
  public displayNameAndId: boolean = true;

  @Input()
  public item: ApiMediaItem;

  @Input()
  public range?: ApiTemporalRange;


}
