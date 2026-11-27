import { MediaPrefetchService } from './media-prefetch.service';
import { AppConfig } from '../app.config';
import { ApiContentElement, ApiContentType } from '../../../openapi';

/**
 * The prefetch service warms the browser cache for hint media. Its bookkeeping decides whether a
 * payload is fetched once, re-fetched needlessly, or left running after the viewer moved on.
 */
describe('MediaPrefetchService', () => {
  let service: MediaPrefetchService;
  let config: AppConfig;
  let calls: { url: string; init?: RequestInit }[];
  let originalFetch: typeof fetch;

  beforeEach(() => {
    config = {
      resolveMediaGrantUrl: (grantPath: string) => `http://dres.test/api/v2/${grantPath}`,
    } as AppConfig;
    service = new MediaPrefetchService(config);
    calls = [];
    originalFetch = globalThis.fetch;
    globalThis.fetch = ((url: string, init?: RequestInit) => {
      calls.push({ url, init });
      return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) } as Response);
    }) as typeof fetch;
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  function element(mediaUrl: string | null): ApiContentElement {
    return { contentType: ApiContentType.VIDEO, offset: 0, mediaUrl } as ApiContentElement;
  }

  it('fetches each media grant with credentials so the bound session is used', () => {
    service.prefetch([element('media/grant/abc')]);

    expect(calls.length).toBe(1);
    expect(calls[0].url).toBe('http://dres.test/api/v2/media/grant/abc');
    expect(calls[0].init?.credentials).toBe('include');
  });

  it('ignores elements without a grant, which carry their content inline', () => {
    service.prefetch([element(null), element(null)]);

    expect(calls.length).toBe(0);
  });

  it('does not fetch the same grant twice', () => {
    service.prefetch([element('media/grant/abc')]);
    service.prefetch([element('media/grant/abc')]);

    expect(calls.length).toBe(1);
  });

  it('does not start a second request for a grant that is still in flight', () => {
    let release: (value: Response) => void;
    globalThis.fetch = ((url: string, init?: RequestInit) => {
      calls.push({ url, init });
      return new Promise<Response>((resolve) => {
        release = resolve;
      });
    }) as typeof fetch;

    service.prefetch([element('media/grant/abc')]);
    service.prefetch([element('media/grant/abc')]);
    expect(calls.length).toBe(1);

    release({ ok: true, blob: () => Promise.resolve(new Blob()) } as Response);
  });

  /**
   * Reaching the cap used to clear the whole set, so a hint with many elements re-downloaded payloads
   * it had just fetched. Only the oldest entry should be dropped.
   */
  it('drops only the oldest grant when the cap is reached', async () => {
    const urls = Array.from({ length: 65 }, (_, i) => `media/grant/g${i}`);
    for (const url of urls) {
      service.prefetch([element(url)]);
      await Promise.resolve();
    }
    expect(calls.length).toBe(65);

    /* g0 was evicted, the recent ones were not. */
    service.prefetch([element('media/grant/g64')]);
    service.prefetch([element('media/grant/g0')]);

    expect(calls.length).toBe(66);
    expect(calls[65].url).toBe('http://dres.test/api/v2/media/grant/g0');
  });

  it('aborts in-flight requests and forgets warmed grants when the task ends', () => {
    const aborted: string[] = [];
    globalThis.fetch = ((url: string, init?: RequestInit) => {
      calls.push({ url, init });
      init?.signal?.addEventListener('abort', () => aborted.push(url));
      return new Promise<Response>(() => undefined);
    }) as typeof fetch;

    service.prefetch([element('media/grant/abc')]);
    service.cancelInFlight();

    expect(aborted).toEqual(['http://dres.test/api/v2/media/grant/abc']);
  });

  it('fetches a grant again after the cache was cleared', () => {
    service.prefetch([element('media/grant/abc')]);
    service.cancelInFlight();
    service.prefetch([element('media/grant/abc')]);

    expect(calls.length).toBe(2);
  });

  it('tolerates a missing sequence', () => {
    expect(() => service.prefetch(null)).not.toThrow();
    expect(() => service.prefetch(undefined)).not.toThrow();
    expect(calls.length).toBe(0);
  });
});
