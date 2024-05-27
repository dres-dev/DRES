import { Pipe, PipeTransform } from '@angular/core';
import { ApiUser } from "../../../../../../openapi";

/**
 * A pipe enabling the filtering of a list of [ApiUser]s based on their username.
 */
@Pipe({
  name: 'userListFilter'
})
export class UserListFilterPipe implements PipeTransform {

  transform(list: ApiUser[], filter: string): ApiUser[] {
    if(!list){
      // Catch no / empty list;
      return [];
    }
    if(!filter){
      // Catch no / empty filter provided
      return list;
    }else{
      return list.filter(user => {
        return user.username.toLowerCase().includes(filter.toLowerCase())
      });
    }
  }

}
