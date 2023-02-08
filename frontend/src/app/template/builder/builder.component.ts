import {Component, OnDestroy, OnInit} from '@angular/core';
import {DeactivationGuarded} from '../../services/can-deactivate.guard';
import {ActivatedRoute, Router, RouterStateSnapshot} from '@angular/router';
import {Observable, Subscription} from 'rxjs';
import {TemplateBuilderService} from './template-builder.service';
import {DownloadService, TemplateService, UserService} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AbstractTemplateBuilderComponent} from './components/abstract-template-builder/abstract-template-builder.component';

@Component({
  selector: 'app-builder',
  templateUrl: './builder.component.html',
  styleUrls: ['./builder.component.scss']
})
export class BuilderComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded {
  onChange() {
  }

  routeSub: Subscription;
  changeSub: Subscription;


  constructor(
      private templateService: TemplateService,
      private userService: UserService,
      private downloadService: DownloadService,
      private route: ActivatedRoute,
      private router: Router,
      private snackBar: MatSnackBar,
      builderService: TemplateBuilderService
  ) {
    super(builderService);
  }

  canDeactivate(nextState?: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    return undefined;
  }

  ngOnDestroy(): void {
    this.onDestroy();
    this.changeSub.unsubscribe();
    this.routeSub.unsubscribe();
  }

  ngOnInit(): void {
    this.routeSub = this.route.params.subscribe( (p) => {
      this.templateService.getApiV2TemplateByTemplateId(p.templateId).subscribe((t) => {
        /* initialise from route */
        this.builderService.initialise(t);
      },
          (r) => {
        this.snackBar.open(`Error: ${r?.error?.description}`, null,{duration: 5000});
          });
    });
  }

  fileProvider = () => {
    return this.builderService.getTemplate()?.name ? this.builderService.getTemplate().name : 'evaluation-template-download.json'
  }

}
