import { Injectable } from '@angular/core';
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
 * @version 1.0.0
 */
@Injectable({
  providedIn: 'root',
})
export class MediaPrefetchService {
  /** URLs already prefetched, so repeated hints or replays do not re-download. Bounded to avoid growth. */
  private readonly prefetched = new Set<string>();

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
   * Fetches [url] once and lets the browser's HTTP cache keep it.
   *
   * @param url The absolute grant URL.
   */
  private fetchIntoCache(url: string): void {
    if (this.prefetched.has(url)) {
      return;
    }
    if (this.prefetched.size >= MediaPrefetchService.MAX_REMEMBERED) {
      this.prefetched.clear();
    }
    this.prefetched.add(url);
    fetch(url, { credentials: 'include', cache: 'default' })
      .then((response) => {
        if (!response.ok) {
          /* A stale or revoked grant is expected after a task ends; the element will simply load it
           * again, so this is informational rather than an error worth surfacing. */
          this.prefetched.delete(url);
          return;
        }
        return response.blob();
      })
      .catch(() => this.prefetched.delete(url));
  }
}
