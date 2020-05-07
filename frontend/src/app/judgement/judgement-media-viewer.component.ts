import {Component, Input, OnInit} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {map} from 'rxjs/operators';
import {AppConfig} from '../app.config';
import {JudgementRequest} from '../../../openapi';

@Component({
  selector: 'app-judgement-media-viewer',
  templateUrl: './judgement-media-viewer.component.html',
  styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent implements OnInit {

  @Input() itemName: Observable<string>;
  @Input() req: BehaviorSubject<JudgementRequest>;

  videoUrl: Observable<SafeUrl>;

  constructor(private sanitizer: DomSanitizer) { }

  ngOnInit(): void {
    const prefix = AppConfig.settings.endpoint;
    // prefix+'/api/media/:collection/:item' + times @ videoplayer
    //const url = prefix + '/api/media/'+ this.req.get._collection+'/'+this.req.get.itemName

    /*
    this.videoUrl = this.itemName.pipe(map(s => {
      return this.sanitizer.bypassSecurityTrustUrl(s);
    }));
    */
  }

}
