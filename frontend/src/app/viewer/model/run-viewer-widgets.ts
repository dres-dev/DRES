/**
 *
 */
export class Widget {
    /** The {@link Widget}s available at the center of the viewer. */
    public static CENTER_WIDGETS: Array<Widget> = [
        new Widget('player', 'Player'),
        new Widget('competition_score',  'Normalized Competition Scores'),
        new Widget('task_type_score', 'Task Type Score')
    ];

    /** The {@link Widget}s available at the bottom of the viewer. */
    public static BOTTOM_WIDGETS: Array<Widget>  = [
        new Widget('team_score', 'Team Scores')
    ];

    private constructor(public readonly name: string, public readonly label: string) {
    }
}
