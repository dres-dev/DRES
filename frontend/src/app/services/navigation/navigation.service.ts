import {Injectable} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';


/**
 * A service to keep a local history for navigating back within the app.
 * Inspired by: https://nils-mehlhorn.de/posts/angular-navigate-back-previous-page
 */
@Injectable({
    providedIn: 'root'
})
export class NavigationService {

    private history: string[] = [];
    private alternateHistory: string[] = [];

    constructor(
        private router: Router,
    ) {
        this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                this.history.push(event.urlAfterRedirects);
            }
        });
    }

    /**
     * Navigates back in the local application navigation history
     */
    back(forceBack: boolean = false): void {
        let destination = 'home'; // Anything that routes to **
        if (this.history.length > 1) {
            this.alternateHistory.push(this.history.pop()); // Current route is put into the alternate history
            destination = this.history.pop(); // The one before the current one is our next destination
        }
        if (forceBack) {
            while (destination.includes('?')) {
                destination = this.history.pop();
            }
        }
        this.router.navigateByUrl(destination);
    }

    /**
     * Navigates forward in the local application navigation history, basically undo for back()
     */
    forward(): void {
        if (this.alternateHistory.length > 0) {
            this.router.navigateByUrl(this.alternateHistory.pop());
        } // Else: there is no forward anymore.
    }
}
