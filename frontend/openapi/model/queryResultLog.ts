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
import { QueryResult } from './queryResult';


export interface QueryResultLog { 
    team: number;
    member: number;
    timestamp: number;
    usedCategories: Array<string>;
    usedTypes: Array<string>;
    sortType: Array<string>;
    resultSetAvailability: string;
    events: Array<QueryResult>;
    serverTimeStamp: number;
}
