import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminAuditlogOverviewComponent } from './admin-auditlog-overview/admin-auditlog-overview.component';
import {SharedModule} from '../shared/shared.module';
import {MatTableModule} from '@angular/material/table';
import {ServicesModule} from '../services/services.module';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ClipboardModule} from '@angular/cdk/clipboard';



@NgModule({
  declarations: [AdminAuditlogOverviewComponent],
    imports: [
        CommonModule,
        SharedModule,
        MatTableModule,
        ServicesModule,
        MatTooltipModule,
        ClipboardModule
    ]
})
export class AuditlogModule { }
