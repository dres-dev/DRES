export * from './collection.service';
import {CollectionService} from './collection.service';
import {CompetitionService} from './competition.service';
import {CompetitionRunService} from './competitionRun.service';
import {CompetitionRunAdminService} from './competitionRunAdmin.service';
import {JudgementService} from './judgement.service';
import {LogService} from './log.service';
import {SubmissionService} from './submission.service';
import {UserService} from './user.service';

export * from './competition.service';
export * from './competitionRun.service';
export * from './competitionRunAdmin.service';
export * from './judgement.service';
export * from './log.service';
export * from './submission.service';
export * from './user.service';
export const APIS = [CollectionService, CompetitionService, CompetitionRunService, CompetitionRunAdminService, JudgementService, LogService, SubmissionService, UserService];
