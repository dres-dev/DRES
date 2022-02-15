/**
 * A class with time related utilities. Basically a port of dev.dres.utilities.TimeUtil.kt
 */
import {RestTemporalPoint, RestTemporalRange} from '../../../openapi';

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

    static point2Milliseconds(point: RestTemporalPoint, fps: number): number {
        switch (point.unit) {
            case 'FRAME_NUMBER':
                return (parseFloat(point.value) / fps * 1000);
            case 'SECONDS':
                return (parseFloat(point.value) * 1000);
            case 'MILLISECONDS':
                return parseFloat(point.value);
            case 'TIMECODE':
                return TimeUtilities.timeCode2Milliseconds(point.value, fps);
        }
    }

    static point2Milliseconds24fps(point: RestTemporalPoint): number {
        return this.point2Milliseconds(point, 24);
    }

    static range2Milliseconds(range: RestTemporalRange, fps: number): [number, number] {
        return [this.point2Milliseconds(range.start, fps), this.point2Milliseconds(range.end, fps)];
    }

    static range2Milliseconds24fps(range: RestTemporalRange): [number, number] {
        return this.range2Milliseconds(range, 24);
    }

    static timeCode2Milliseconds(timecode: string, fps: number): number {
        // console.log(`Input: ${timecode}`);
        /*
const a1 = '01:02:03:456';
const a2 = '02:03:456';
const a3 = '03:456';
const a4 = '456';

const regexpSize = /([0-9]+:)?([0-5]?\d:)?([0-5]?\d:)?([0-9]+)/;

console.log(a1.match(regexpSize));
console.log(a2.match(regexpSize));
console.log(a3.match(regexpSize));
console.log(a4.match(regexpSize));
for testing at e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/test
         */
        const matches = timecode.match(this.timeCodeRegexParsing);
        // console.log(`Matches: ${matches}`);

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
        // console.log(`parsed: ${hours}:${minutes}:${seconds}:${frames}`);

        const ms = hours * this.msPerHour + minutes * this.msPerMinute + seconds * 1000 + (1000 * frames / fps);
        // console.log(`Output: ${ms}`);
        return ms;
    }

    static timeCode2Milliseconds24fps(timecode: string): number {
        return this.timeCode2Milliseconds(timecode, 24);
    }
}
