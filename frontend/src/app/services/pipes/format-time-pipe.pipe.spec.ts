import { FormatTimePipePipe } from './format-time-pipe.pipe';

/**
 * A perpetual task reports no remaining time (`null`) while an expired one reports `0`. Rendering both
 * as `00:00:00` claims a deadline that does not exist, so the two cases are pinned separately here.
 */
describe('FormatTimePipePipe', () => {
  let pipe: FormatTimePipePipe;

  beforeEach(() => {
    pipe = new FormatTimePipePipe();
  });

  it('renders an infinity sign for a missing duration', () => {
    expect(pipe.transform(null)).toBe('\u221e');
    expect(pipe.transform(undefined)).toBe('\u221e');
  });

  it('renders a real zero as zero, not as infinity', () => {
    expect(pipe.transform(0, false)).toBe('00:00');
    expect(pipe.transform(0)).toBe('00:00.000');
  });

  it('formats seconds and minutes', () => {
    expect(pipe.transform(59, false)).toBe('00:59');
    expect(pipe.transform(60, false)).toBe('01:00');
    expect(pipe.transform(3599, false)).toBe('59:59');
  });

  it('adds an hours component only when there are hours', () => {
    expect(pipe.transform(3600, false)).toBe('01:00:00');
    expect(pipe.transform(3661, false)).toBe('01:01:01');
  });

  it('treats the value as milliseconds by default', () => {
    expect(pipe.transform(1500)).toBe('00:01.500');
    /* With inMs = false the same number means seconds, so 1500 is twenty-five minutes. */
    expect(pipe.transform(1500, false)).toBe('25:00');
  });

  it('honours a custom fallback', () => {
    expect(pipe.transform(null, false, 'n/a')).toBe('n/a');
  });

  it('renders NaN as the fallback instead of NaN:NaN', () => {
    expect(pipe.transform(Number.NaN)).toBe('\u221e');
  });
});
