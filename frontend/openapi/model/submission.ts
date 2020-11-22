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
import { TaskRun } from './taskRun';
import { MediaItem } from './mediaItem';


export interface Submission { 
    teamId?: string;
    memberId?: string;
    timestamp: number;
    item: MediaItem;
    uid?: string;
    status: Submission.StatusEnum;
    uid_1rSl5jE: string;
    teamId_1rSl5jE: string;
    memberId_1rSl5jE: string;
    taskRun$backend?: TaskRun;
}
export namespace Submission {
    export type StatusEnum = 'CORRECT' | 'WRONG' | 'INDETERMINATE' | 'UNDECIDABLE';
    export const StatusEnum = {
        CORRECT: 'CORRECT' as StatusEnum,
        WRONG: 'WRONG' as StatusEnum,
        INDETERMINATE: 'INDETERMINATE' as StatusEnum,
        UNDECIDABLE: 'UNDECIDABLE' as StatusEnum
    };
}


