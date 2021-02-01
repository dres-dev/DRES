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
import { TemporalRange } from './temporalRange';


export interface RestTaskDescriptionComponent { 
    type: RestTaskDescriptionComponent.TypeEnum;
    start?: number;
    end?: number;
    description?: string;
    path?: string;
    dataType?: string;
    mediaItem?: string;
    range?: TemporalRange;
}
export namespace RestTaskDescriptionComponent {
    export type TypeEnum = 'IMAGE_ITEM' | 'VIDEO_ITEM_SEGMENT' | 'TEXT' | 'EXTERNAL_IMAGE' | 'EXTERNAL_VIDEO';
    export const TypeEnum = {
        ImageItem: 'IMAGE_ITEM' as TypeEnum,
        VideoItemSegment: 'VIDEO_ITEM_SEGMENT' as TypeEnum,
        Text: 'TEXT' as TypeEnum,
        ExternalImage: 'EXTERNAL_IMAGE' as TypeEnum,
        ExternalVideo: 'EXTERNAL_VIDEO' as TypeEnum
    };
}


