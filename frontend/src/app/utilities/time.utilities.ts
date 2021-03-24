import {TemporalPoint, TemporalRange} from '../../../openapi';

/**
 * A class with time related utilities. Basically a port of dev.dres.utilities.TimeUtil.kt
 */
export class TimeUtilities {


    private static msPerHour = 3_600_000;
    private static msPerMinute = 60_000;

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
        const matches = timecode.split(':');

        const hours = matches[0] ? Number.parseInt(matches[0], 10) : 0;
        const minutes = matches[1] ? Number.parseInt(matches[1], 10) : 0;
        const seconds = matches[2] ? Number.parseInt(matches[2], 10) : 0;
        const frames = matches[3] ? Number.parseInt(matches[3], 10) : 0;

        return hours * this.msPerHour + minutes * this.msPerMinute + seconds * 1000 + (1000 * frames / fps);
    }

    static timeCode2Milliseconds24fps(timecode: string): number {
        return this.timeCode2Milliseconds(timecode, 24);
    }
}
