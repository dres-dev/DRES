import { Injectable, OnDestroy } from '@angular/core';
import { AppConfig } from '../app.config';
import { ApiContentElement } from '../../../openapi';

/**
 * Warms the browser's HTTP cache for the media grants contained in a task hint.
 *
 * The point is to move the network transfer into the waiting window: a hint arrives while the viewer is
 * still counting down, playback begins a moment later, and warming the payloads means the `<video>`
 * element finds them in the cache instead of stalling on a download. That is what makes grant-based
 * delivery feel as fast as the previous inline base64 approach while still being scoped, expiring and
 * revocable.
 *
 * Only hints are prefetched. A task *target* is deliberately not prefetched: it is ground truth that
 * the viewer is not meant to have before the task ends, and its data volume does not need to be
 * front-loaded because it is revealed once at the end rather than replayed under time pressure.
 *
 * Only elements that carry a {@link ApiContentElement.mediaUrl} are fetched; inline base64 content
 * needs no prefetch because it is already in memory.
 *
 * @author DRES
 * @version 1.1.0
 */
@Injectable({
  providedIn: 'root',
})
export class MediaPrefetchService implements OnDestroy {
  /** URLs already prefetched, so repeated hints or replays do not re-download. Bounded to avoid growth. */
  private readonly prefetched = new Set<string>();

  /**
   * Requests that are still in flight.
   *
   * Tracked separately from {@link prefetched} so that {@link cancelInFlight} can abort them: a viewer
   * that navigates away, is torn down, or finishes the task should not keep a download running.
   */
  private readonly inFlight = new Map<string, AbortController>();

  /** Upper bound on remembered URLs; grants expire, so the set only needs to cover an active task. */
  private static readonly MAX_REMEMBERED = 64;

  constructor(private config: AppConfig) {}

  /**
   * Prefetches every media payload in a hint [sequence].
   *
   * Call this for hints only; see the class documentation for why targets are not prefetched.
   *
   * Failures are swallowed on purpose: prefetching is an optimization, and the preview component will
   * request the grant itself if this does not succeed.
   *
   * @param sequence The hint content elements whose media should be warmed.
   */
  public prefetch(sequence: ApiContentElement[] | null | undefined): void {
    if (!sequence) {
      return;
    }
    for (const element of sequence) {
      if (!element?.mediaUrl) {
        continue;
      }
      const url = this.config.resolveMediaGrantUrl(element.mediaUrl);
      this.fetchIntoCache(url);
    }
  }

  /**
   * Aborts every in-flight prefetch and forgets which URLs were warmed.
   *
   * Called when the viewer leaves a task: the grants belong to that task, and remembering them past
   * that point would make a later task with a recycled id skip a fetch it actually needs.
   */
  public cancelInFlight(): void {
    for (const controller of this.inFlight.values()) {
      controller.abort();
    }
    this.inFlight.clear();
    this.prefetched.clear();
  }

  /**
   * Aborts in-flight prefetches when the service is destroyed. The service is root-provided, so this
   * only matters when the injector itself is torn down (tests, hot reload).
   */
  ngOnDestroy(): void {
    this.cancelInFlight();
  }

  /**
   * Fetches [url] once and lets the browser's HTTP cache keep it.
   *
   * @param url The absolute grant URL.
   */
  private fetchIntoCache(url: string): void {
    if (this.prefetched.has(url) || this.inFlight.has(url)) {
      return;
    }
    /*
     * Drop the oldest entries instead of the whole set. Clearing everything on reaching the cap made a
     * hint with many media elements re-download payloads it had just fetched, which is the opposite of
     * what prefetching is for.
     */
    if (this.prefetched.size >= MediaPrefetchService.MAX_REMEMBERED) {
      const oldest = this.prefetched.values().next();
      if (!oldest.done) {
        this.prefetched.delete(oldest.value);
      }
    }

    const controller = new AbortController();
    this.prefetched.add(url);
    this.inFlight.set(url, controller);

    fetch(url, { credentials: 'include', cache: 'default', signal: controller.signal })
      .then((response) => {
        if (!response.ok) {
          /* A stale or revoked grant is expected after a task ends; the element will simply load it
           * again, so this is informational rather than an error worth surfacing. */
          this.prefetched.delete(url);
          return;
        }
        return response.blob();
      })
      .catch(() => this.prefetched.delete(url))
      .finally(() => this.inFlight.delete(url));
  }
}
