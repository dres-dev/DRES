import { Component, OnInit } from '@angular/core';
import { ApiEvaluationTemplate, TemplateService } from "../../../../../openapi";
import {Subscription} from 'rxjs';
import {TemplateBuilderService} from '../template-builder.service';
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";

export abstract class AbstractTemplateBuilderComponent {

  subscription: Subscription;
  routeSub: Subscription;

  private templateId: string;
  protected constructor(
    protected builderService: TemplateBuilderService,
    protected route: ActivatedRoute,
    protected templateService: TemplateService,
    protected snackBar: MatSnackBar
    ) { }

  onInit(): void {
    this.subscription = this.builderService.templateAsObservable().subscribe((t) => {
      this.onChange();
    });
    this.routeSub = this.route.params.subscribe( (p) => {
      this.templateId = p.templateId;
    });
  }

  onDestroy(){
    this.subscription?.unsubscribe();
    this.routeSub?.unsubscribe();
    this.subscription = null;
    this.routeSub = null;
  }

  protected getTemplateId(){
    return this.templateId;
  }


  abstract onChange();

}
