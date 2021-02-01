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


export interface RestJudgementAuditLogEntryAllOf { 
    competition?: string;
    validator?: string;
    token?: string;
    verdict?: RestJudgementAuditLogEntryAllOf.VerdictEnum;
    api?: RestJudgementAuditLogEntryAllOf.ApiEnum;
    user?: string;
}
export namespace RestJudgementAuditLogEntryAllOf {
    export type VerdictEnum = 'CORRECT' | 'WRONG' | 'INDETERMINATE' | 'UNDECIDABLE';
    export const VerdictEnum = {
        Correct: 'CORRECT' as VerdictEnum,
        Wrong: 'WRONG' as VerdictEnum,
        Indeterminate: 'INDETERMINATE' as VerdictEnum,
        Undecidable: 'UNDECIDABLE' as VerdictEnum
    };
    export type ApiEnum = 'REST' | 'CLI' | 'INTERNAL';
    export const ApiEnum = {
        Rest: 'REST' as ApiEnum,
        Cli: 'CLI' as ApiEnum,
        Internal: 'INTERNAL' as ApiEnum
    };
}


