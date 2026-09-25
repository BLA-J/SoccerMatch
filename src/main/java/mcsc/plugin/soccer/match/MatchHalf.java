package mcsc.plugin.soccer.match;

/**
 * 上下半场。
 * /soccerstatus true  -> FIRST（上半场）
 * /soccerstatus false -> SECOND（下半场）
 */
public enum MatchHalf {
    FIRST("上半场"),
    SECOND("下半场");

    private final String display;

    MatchHalf(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public static MatchHalf of(boolean firstHalf) {
        return firstHalf ? FIRST : SECOND;
    }
}
