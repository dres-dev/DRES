import { NgModule, ModuleWithProviders, SkipSelf, Optional } from '@angular/core';
import { Configuration } from './configuration';
import { HttpClient } from '@angular/common/http';

import { AuditService } from './api/audit.service';
import { CollectionService } from './api/collection.service';
import { CompetitionService } from './api/competition.service';
import { CompetitionRunService } from './api/competitionRun.service';
import { CompetitionRunAdminService } from './api/competitionRunAdmin.service';
import { CompetitionRunScoresService } from './api/competitionRunScores.service';
import { JudgementService } from './api/judgement.service';
import { LogService } from './api/log.service';
import { StatusService } from './api/status.service';
import { SubmissionService } from './api/submission.service';
import { UserService } from './api/user.service';

@NgModule({
  imports:      [],
  declarations: [],
  exports:      [],
  providers: []
})
export class ApiModule {
    public static forRoot(configurationFactory: () => Configuration): ModuleWithProviders<ApiModule> {
        return {
            ngModule: ApiModule,
            providers: [ { provide: Configuration, useFactory: configurationFactory } ]
        };
    }

    constructor( @Optional() @SkipSelf() parentModule: ApiModule,
                 @Optional() http: HttpClient) {
        if (parentModule) {
            throw new Error('ApiModule is already loaded. Import in your base AppModule only.');
        }
        if (!http) {
            throw new Error('You need to import the HttpClientModule in your AppModule! \n' +
            'See also https://github.com/angular/angular/issues/20575');
        }
    }
}
