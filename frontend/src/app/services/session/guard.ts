import {ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot} from "@angular/router";
import {AuthenticationService} from "./authentication.sevice";
import {inject} from "@angular/core";
import {ApiRole} from "../../../../openapi";

/**
 * Guard used to determine if a view can be activated that requires the user to hold the {@link ApiRole.ADMIN} role.
 */
export const canActivateAdministrator: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
    return inject(AuthenticationService).canActivate([ApiRole.ADMIN], route, state);
};


/**
 * Guard used to determine if a view can be activated that requires the user to hold the {@link ApiRole.JUDGE} role.
 */
export const canActivateJudge: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
    return inject(AuthenticationService).canActivate([ApiRole.ADMIN, ApiRole.JUDGE], route, state);
};


/**
 * Guard used to determine if a view can be activated that requires the user to hold the {@link ApiRole.JUDGE} role.
 */
export const canActivatePublicVote: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
    return inject(AuthenticationService).canActivate([ApiRole.ADMIN, ApiRole.JUDGE, ApiRole.VIEWER], route, state);
};
/**
 * Guard used to determine if a view can be activated that requires the user to hold any role.
 */
export const canActivateAnyRole: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot)  => {
    return inject(AuthenticationService).canActivate([ApiRole.ADMIN, ApiRole.VIEWER, ApiRole.JUDGE, ApiRole.PARTICIPANT], route, state);
};