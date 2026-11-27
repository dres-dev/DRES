/**
 * A class with time related utilities. Basically a port of dev.dres.utilities.TimeUtil.kt
 */
import { ApiTemporalPoint, ApiTemporalRange } from '../../../openapi';

export class TimeUtilities {
  /**
   * The proper TIMECODE regex for parsing (except it does not work for ECMAScript).
   * Used for validating whether the TIMECODE input
   */
  public static timeCodeRegex = /^\s*(?:(?:(?:(\d+):)?([0-5]?\d):)?([0-5]?\d):)?(\d+)\s*$/;
  private static msPerHour = 3_600_000;
  private static msPerMinute = 60_000;
  /**
   * ECMAScript compatible version of the timeCodeRegex
   * @private
   */
  private static timeCodeRegexParsing = /([0-9]+:)?([0-5]?\d:)?([0-5]?\d:)?([0-9]+)/;

  static point2Milliseconds(point: ApiTemporalPoint, fps: number): number {
    switch (point.unit) {
      case 'FRAME_NUMBER':
        return (Number.parseInt(point.value, 10) / fps) * 1000;
      case 'SECONDS':
        return Number.parseInt(point.value, 10) * 1000;
      case 'MILLISECONDS':
        return Number.parseInt(point.value, 10);
    }
  }

  static point2Milliseconds24fps(point: ApiTemporalPoint): number {
    return this.point2Milliseconds(point, 24);
  }

  static range2Milliseconds(range: ApiTemporalRange, fps: number): [number, number] {
    return [this.point2Milliseconds(range.start, fps), this.point2Milliseconds(range.end, fps)];
  }

  static range2Milliseconds24fps(range: ApiTemporalRange): [number, number] {
    return this.range2Milliseconds(range, 24);
  }

  /**
   * Converts a timecode such as `01:02:03:456`, `02:03:456`, `03:456` or `456` into milliseconds.
   *
   * The pattern is matched level by level rather than by position, which is why the three cases
   * shuffle the group indices: a shorter timecode simply has fewer leading groups.
   *
   * @param timecode The timecode to convert.
   * @param fps Frames per second, used for the last component.
   * @throws Error when [timecode] does not contain a number this method can interpret.
   */
  static timeCode2Milliseconds(timecode: string, fps: number): number {
    const matches = timecode?.match(this.timeCodeRegexParsing);
    /*
     * Without this guard the code below read `matches[4]` on null and threw
     * "Cannot read properties of null", which pointed at the parse rather than at the malformed input.
     */
    if (!matches || matches[4] == null) {
      throw new Error(`Cannot parse the timecode '${timecode}'.`);
    }

    let hIdx = 1;
    let mIdx = 2;
    let sIdx = 3;

    if (matches[1] && matches[2] && matches[3]) {
      // all specified
    } else if (matches[1] && matches[2]) {
      hIdx = 3; // so its undefined
      mIdx = 1;
      sIdx = 2;
    } else if (matches[1]) {
      hIdx = 3; // so its undefined
      mIdx = 2;
      sIdx = 1;
    } // else I don't care, as 1,2,3 all are undefined

    const hours = matches[hIdx] ? Number.parseInt(matches[hIdx].substring(0, matches[hIdx].length - 1), 10) : 0;
    const minutes = matches[mIdx] ? Number.parseInt(matches[mIdx].substring(0, matches[mIdx].length - 1), 10) : 0;
    const seconds = matches[sIdx] ? Number.parseInt(matches[sIdx].substring(0, matches[sIdx].length - 1), 10) : 0;
    const frames = Number.parseInt(matches[4], 10);
    return hours * this.msPerHour + minutes * this.msPerMinute + seconds * 1000 + (1000 * frames) / fps;
  }

  static timeCode2Milliseconds24fps(timecode: string): number {
    return this.timeCode2Milliseconds(timecode, 24);
  }

  static temporalPointEquals(a: ApiTemporalPoint, b: ApiTemporalPoint) {
    return a.value === b.value && a.unit === b.unit;
  }
}
