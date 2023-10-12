import { Component, Input } from "@angular/core";
import { ApiMediaItem, ApiTemporalRange } from "../../../../openapi";
import { AppConfig } from "../../app.config";
import { TimeUtilities } from "../../utilities/time.utilities";

@Component({
  selector: 'app-media-item-viewer',
  templateUrl: './media-item-viewer.component.html',
  styleUrls: ['./media-item-viewer.component.scss']
})
export class MediaItemViewerComponent {

  @Input()
  public toggleable: boolean = false;

  @Input()
  public showing: boolean = true;

  @Input()
  public displayNameAndId: boolean = true;

  @Input()
  public item: ApiMediaItem;

  @Input()
  public range?: ApiTemporalRange;

  isRangeSingular():boolean {
    return this.range && TimeUtilities.temporalPointEquals(this.range.start, this.range.end)
  }

  time():number{
    const t =  TimeUtilities.point2Milliseconds(this.range.start, this.item.fps)
    return t;
  }


}
