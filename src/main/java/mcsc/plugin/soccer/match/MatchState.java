package mcsc.plugin.soccer.match;

/**
 * 比赛状态机。
 *
 * IDLE      未开始（尚未 /js start 或已 /js end）
 * COUNTDOWN 开球 3-2-1 倒计时中，球员被冻结
 * RUNNING   进行中，计时递减
 * PAUSED    已被 /js stop 暂停
 */
public enum MatchState {
    IDLE,
    COUNTDOWN,
    RUNNING,
    PAUSED;

    /** 比赛是否已经开始（含倒计时与暂停）。 */
    public boolean isStarted() {
        return this != IDLE;
    }
}
