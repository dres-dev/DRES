import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {AbstractTemplateBuilderComponent} from './components/abstract-template-builder.component';
import {DeactivationGuarded} from '../../services/can-deactivate.guard';
import {Observable, Subscription} from 'rxjs';
import {DownloadService, TemplateService, UserService} from '../../../../openapi';
import {ActivatedRoute, Router, RouterStateSnapshot} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {TemplateBuilderService} from './template-builder.service';
import {take} from 'rxjs/operators';

@Component({
  selector: 'app-template-builder',
  templateUrl: './template-builder.component.html',
  styleUrls: ['./template-builder.component.scss']
})
export class TemplateBuilderComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, DeactivationGuarded {
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
      public builderService: TemplateBuilderService
) {
    super(builderService);
  }

  canDeactivate(nextState?: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    return this.builderService.checkDirty();
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

  downloadProvider = () => {
    return this.downloadService.getApiV2DownloadTemplateByTemplateId(this.builderService.getTemplate()?.id).pipe(take(1));
  }

  public save(){
    // FIXME re-enable form validation. possibly on the form-builder?
    console.log("save")
    this.templateService.patchApiV2TemplateByTemplateId(this.builderService.getTemplate().id, this.builderService.getTemplate()).subscribe((s) => {
      this.snackBar.open(s.description, null, {duration: 5000});
      this.builderService.unmarkDirty();
    }, (r) => this.snackBar.open(`Error: ${r?.error?.description}`, null, {duration: 5000}));
  }

  public back(){
    if(this.builderService.checkDirty()){
      this.router.navigate(['/template/list']);
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (!this.builderService.checkDirty()) {
      event.preventDefault();
      event.returnValue = '';
      return;
    }
    delete event.returnValue;
  }

  refresh() {
    if(this.builderService.checkDirty()){
      this.ngOnInit();
    }
  }

}
