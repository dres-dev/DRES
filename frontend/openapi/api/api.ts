export * from './competition.service';
import { CompetitionService } from './competition.service';
export * from './default.service';
import { DefaultService } from './default.service';
export * from './media.service';
import { MediaService } from './media.service';
export * from './user.service';
import { UserService } from './user.service';
export const APIS = [CompetitionService, DefaultService, MediaService, UserService];
