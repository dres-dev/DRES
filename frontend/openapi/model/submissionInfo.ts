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


export interface SubmissionInfo { 
    team: number;
    submissionTime: number;
    status: SubmissionInfo.StatusEnum;
    collection?: string;
    item?: string;
    timeCode?: string;
}
export namespace SubmissionInfo {
    export type StatusEnum = 'CORRECT' | 'WRONG' | 'INDETERMINATE';
    export const StatusEnum = {
        CORRECT: 'CORRECT' as StatusEnum,
        WRONG: 'WRONG' as StatusEnum,
        INDETERMINATE: 'INDETERMINATE' as StatusEnum
    };
}

