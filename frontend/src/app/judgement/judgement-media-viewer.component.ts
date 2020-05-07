import {Component, Input, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {map} from 'rxjs/operators';
import {AppConfig} from '../app.config';

@Component({
  selector: 'app-judgement-media-viewer',
  templateUrl: './judgement-media-viewer.component.html',
  styleUrls: ['./judgement-media-viewer.component.scss']
})
export class JudgementMediaViewerComponent implements OnInit {

  @Input() itemName: Observable<string>;

  videoUrl: Observable<SafeUrl>;

  constructor(private sanitizer: DomSanitizer, private config: AppConfig) { }

  ngOnInit(): void {
    /*
    this.videoUrl = this.itemName.pipe(map(s => {
      return this.sanitizer.bypassSecurityTrustUrl(s);
    }));
    */
  }

}
