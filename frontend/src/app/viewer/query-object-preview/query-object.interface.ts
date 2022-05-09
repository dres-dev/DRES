import { SafeUrl } from '@angular/platform-browser';

export type QueryObjectType = 'video' | 'text' | 'image';
export interface QueryObject {
  type: QueryObjectType;
  url?: SafeUrl;
  text?: string[];
}
