/**
 * DRES API
 * API for DRES (Distributed Retrieval Evaluation Server), Version 1.0
 *
 * The version of the OpenAPI document: 1.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { RestLogoutAuditLogEntryAllOf } from './restLogoutAuditLogEntryAllOf';
import { RestAuditLogEntry } from './restAuditLogEntry';


export interface RestLogoutAuditLogEntry { 
    type: RestLogoutAuditLogEntry.TypeEnum;
    id: string;
    timestamp: number;
    session: string;
    api: RestLogoutAuditLogEntry.ApiEnum;
}
export namespace RestLogoutAuditLogEntry {
    export type TypeEnum = 'COMPETITION_START' | 'COMPETITION_END' | 'TASK_START' | 'TASK_MODIFIED' | 'TASK_END' | 'SUBMISSION' | 'PREPARE_JUDGEMENT' | 'JUDGEMENT' | 'LOGIN' | 'LOGOUT';
    export const TypeEnum = {
        CompetitionStart: 'COMPETITION_START' as TypeEnum,
        CompetitionEnd: 'COMPETITION_END' as TypeEnum,
        TaskStart: 'TASK_START' as TypeEnum,
        TaskModified: 'TASK_MODIFIED' as TypeEnum,
        TaskEnd: 'TASK_END' as TypeEnum,
        Submission: 'SUBMISSION' as TypeEnum,
        PrepareJudgement: 'PREPARE_JUDGEMENT' as TypeEnum,
        Judgement: 'JUDGEMENT' as TypeEnum,
        Login: 'LOGIN' as TypeEnum,
        Logout: 'LOGOUT' as TypeEnum
    };
    export type ApiEnum = 'REST' | 'CLI' | 'INTERNAL';
    export const ApiEnum = {
        Rest: 'REST' as ApiEnum,
        Cli: 'CLI' as ApiEnum,
        Internal: 'INTERNAL' as ApiEnum
    };
}


