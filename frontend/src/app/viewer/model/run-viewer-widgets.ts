/**
 *
 */
export class Widget {
  private constructor(public readonly name: string, public readonly label: string) {}

  public static DEFAULTS = {
    center: 'player',
    left: 'competition_score',
    right: 'task_type_score',
    bottom: 'team_score',
  };

  /** The {@link Widget}s available at the center of the viewer. */
  public static CENTER_WIDGETS: Array<Widget> = [
    new Widget('player', 'Player'),
    new Widget('competition_score', 'Normalized Competition Scores'),
    new Widget('task_type_score', 'Task Type Score'),
  ];

  /** The {@link Widget}s available at the bottom of the viewer. */
  public static BOTTOM_WIDGETS: Array<Widget> = [new Widget('team_score', 'Team Scores')];

  /** Given a name and a default, this resolves and returns a {@link Widget} of the group {@link CENTER_WIDGETS} */
  public static resolveCenterWidget(name: string, fallback: string) {
    return this.CENTER_WIDGETS.find((s) => s.name === name) || this.CENTER_WIDGETS.find((s) => s.name === fallback);
  }

  /** Given a name and a default, this resolves and returns a {@link Widget} of the group {@link BOTTOM_WIDGETS} */
  public static resolveBottomWidget(name: string, fallback: string) {
    return this.BOTTOM_WIDGETS.find((s) => s.name === name) || this.BOTTOM_WIDGETS.find((s) => s.name === fallback);
  }

  /**
   * Resolves the widget for the given position [left, right, center, bottom]
   */
  public static resolveWidget(name: string, position: string) {
    if (name === null || name === 'null') {
      return null;
    }
    if (position === 'bottom') {
      return this.resolveBottomWidget(name, this.DEFAULTS[position]);
    } else {
      return this.resolveCenterWidget(name, this.DEFAULTS[position]);
    }
  }
}
