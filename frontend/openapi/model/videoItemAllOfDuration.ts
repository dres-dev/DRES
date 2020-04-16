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
import { VideoItemAllOfDurationUnits } from './videoItemAllOfDurationUnits';


export interface VideoItemAllOfDuration { 
    seconds?: number;
    nano?: number;
    zero?: boolean;
    negative?: boolean;
    units?: Array<VideoItemAllOfDurationUnits>;
}

