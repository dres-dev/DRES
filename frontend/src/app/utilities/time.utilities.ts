import {TemporalPoint, TemporalRange} from '../../../openapi';

/**
 * A class with time related utilities. Basically a port of dev.dres.utilities.TimeUtil.kt
 */
export class TimeUtilities {


    private static msPerHour = 3_600_000;
    private static msPerMinute = 60_000;

    public static timeCodeRegex = /^\s*(?:(?:(?:(\d+):)?([0-5]?\d):)?([0-5]?\d):)?(\d+)\s*$/;

    static point2Milliseconds(point: TemporalPoint, fps: number): number {
        switch (point.unit) {
            case 'FRAME_NUMBER':
                return (point.value / fps * 1000);
            case 'SECONDS':
                return (point.value * 1000);
            case 'MILLISECONDS':
                return point.value;
        }
    }

    static point2Milliseconds24fps(point: TemporalPoint): number {
        return this.point2Milliseconds(point, 24);
    }

    static range2Milliseconds(range: TemporalRange, fps: number): [number, number] {
        return [this.point2Milliseconds(range.start, fps), this.point2Milliseconds(range.end, fps)];
    }

    static range2Milliseconds24fps(range: TemporalRange): [number, number] {
        return this.range2Milliseconds(range, 24);
    }

    static timeCode2Milliseconds(timecode: string, fps: number): number {
        console.log(`Input: ${timecode}`);
        const matches = timecode.split(':');
        console.log(`Matches: ${matches}`);
        // const matches = timecode.match(this.timeCodeRegex);
        /* The regex does not work in JavaScript, as can be seen when copy & pasting this snippet to https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions/Groups_and_Ranges

        const regex = /^\s*(?:(?:(?:(\d+):)?([0-5]?\d):)?([0-5]?\d):)?(\d+)\s*$/;
const tc1 = '01:02:03:456'; // 3x :
const tc2 = '02:03:456'; // 2x :
const tc3 = '03:456'; // 1x :
const tc4 = '4567';
console.log(tc1.match(regex));console.log(tc4.match(regex));console.log(tc4.match(regex));console.log(tc4.match(regex));
         */
        let hoursIndex = -1;
        let minutesIndex = -1;
        let secondsIndex = -1;
        let framesIndex = 0;
        switch (matches.length) {
            case 4:
                hoursIndex = 0;
                minutesIndex = 1;
                secondsIndex = 2;
                framesIndex = 3;
                break;
            case 3:
                minutesIndex = 0;
                secondsIndex = 1;
                framesIndex = 2;
                break;
            case 2:
                secondsIndex = 0;
                framesIndex = 1;
                break;
        }

        const hours = hoursIndex >= 0 ? Number.parseInt(matches[hoursIndex], 10) : 0;
        const minutes = minutesIndex >= 0 ? Number.parseInt(matches[minutesIndex], 10) : 0;
        const seconds = secondsIndex >= 0 ? Number.parseInt(matches[secondsIndex], 10) : 0;
        const frames = Number.parseInt(matches[framesIndex], 10);

        const ms = hours * this.msPerHour + minutes * this.msPerMinute + seconds * 1000 + (1000 * frames / fps);
        console.log(`output: ${ms}`);
        return ms;
    }

    static timeCode2Milliseconds24fps(timecode: string): number {
        return this.timeCode2Milliseconds(timecode, 24);
    }
}
