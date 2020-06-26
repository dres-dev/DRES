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
import { TaskDescription } from './taskDescription';
import { Team } from './team';


export interface RunInfo { 
    id: number;
    name: string;
    description?: string;
    teams: Array<Team>;
    tasks: Array<TaskDescription>;
}

