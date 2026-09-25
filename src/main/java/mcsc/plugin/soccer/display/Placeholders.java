package mcsc.plugin.soccer.display;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.match.MatchState;
import org.bukkit.entity.Player;

/**
 * 计分板与 BossBar 的占位符替换。
 */
public final class Placeholders {

    private Placeholders() {
    }

    /**
     * @param viewer 观看者，可为 null（此时 %team% 留空）
     */
    public static String apply(SoccerPlugin plugin, String raw, Player viewer) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        var match = plugin.match();
        String team = viewer == null ? "" : plugin.teams().displayOf(viewer);

        return raw
                .replace("%time%", match.formattedTime())
                .replace("%half%", match.half().display())
                .replace("%red%", String.valueOf(match.redScore()))
                .replace("%blue%", String.valueOf(match.blueScore()))
                .replace("%team%", team)
                .replace("%state%", stateText(match.state()));
    }

    public static String stateText(MatchState state) {
        return switch (state) {
            case IDLE -> "未开始";
            case COUNTDOWN -> "即将开始";
            case RUNNING -> "进行中";
            case PAUSED -> "已暂停";
        };
    }
}
