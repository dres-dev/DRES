import { Pipe, PipeTransform } from '@angular/core';
import { ApiUser } from "../../../../../../openapi";

/**
 * Simple filter which filters a list of [ApiUser]s based on whether they are present in another list.
 */
@Pipe({
  name: 'userListInOtherFilter'
})
export class UserListInOtherFilterPipe implements PipeTransform {

  transform(list: ApiUser[], other: ApiUser[]): ApiUser[] {
    if(!list){
      return [];
    }
    if(!other){
      return list;
    }
    return list.filter(it => !other.includes(it))
  }

}
