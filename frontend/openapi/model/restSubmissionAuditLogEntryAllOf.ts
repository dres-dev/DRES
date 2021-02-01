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
import { SubmissionInfo } from './submissionInfo';


export interface RestSubmissionAuditLogEntryAllOf { 
    competition?: string;
    taskName?: string;
    submission?: SubmissionInfo;
    api?: RestSubmissionAuditLogEntryAllOf.ApiEnum;
    user?: string;
    address?: string;
}
export namespace RestSubmissionAuditLogEntryAllOf {
    export type ApiEnum = 'REST' | 'CLI' | 'INTERNAL';
    export const ApiEnum = {
        Rest: 'REST' as ApiEnum,
        Cli: 'CLI' as ApiEnum,
        Internal: 'INTERNAL' as ApiEnum
    };
}


