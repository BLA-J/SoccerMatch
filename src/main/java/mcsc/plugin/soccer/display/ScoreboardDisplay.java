package mcsc.plugin.soccer.display;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 侧边栏计分板。
 *
 * 每名玩家持有独立计分板，因为 %team% 需要显示"你的队伍"。
 * 刷新时只改 Team 的 prefix，不重建 entry，因此不会闪烁。
 */
public class ScoreboardDisplay {

    /** 章节符号，即颜色码前缀 §。 */
    private static final char SECTION = '\u00A7';

    /** 每行使用的唯一占位 entry，颜色码本身不可见。 */
    private static final String[] ENTRIES = new String[16];

    static {
        char[] codes = "0123456789abcdef".toCharArray();
        for (int i = 0; i < codes.length; i++) {
            ENTRIES[i] = "" + SECTION + codes[i] + SECTION + 'r';
        }
    }

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardDisplay(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** 每秒调用一次。 */
    public void refreshAll() {
        if (!config.scoreboardEnabled()) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            refresh(p);
        }
    }

    public void refresh(Player player) {
        if (!config.scoreboardEnabled()) {
            return;
        }

        List<String> lines = config.scoreboardLines();
        Scoreboard board = boards.get(player.getUniqueId());

        // 行数变化（比如重载配置后）需要重建
        if (board == null || board.getObjective(config.objectiveName()) == null
                || board.getTeams().size() != lines.size()) {
            board = build(player, lines.size());
            boards.put(player.getUniqueId(), board);
        }

        Objective obj = board.getObjective(config.objectiveName());
        if (obj == null) {
            return;
        }
        obj.displayName(Msg.color(config.scoreboardTitle()));

        for (int i = 0; i < lines.size(); i++) {
            Team team = board.getTeam(lineTeamName(i));
            if (team == null) {
                continue;
            }
            String text = Placeholders.apply(plugin, lines.get(i), player);
            team.prefix(Msg.color(text));
        }

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    private Scoreboard build(Player player, int lineCount) {
        var manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective(
                config.objectiveName(),
                Criteria.DUMMY,
                Msg.color(config.scoreboardTitle())
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // score 越大越靠上，因此第一行取最大值
        for (int i = 0; i < lineCount && i < ENTRIES.length; i++) {
            String entry = ENTRIES[i];
            Team team = board.registerNewTeam(lineTeamName(i));
            team.addEntry(entry);
            obj.getScore(entry).setScore(lineCount - i);
        }
        return board;
    }

    private String lineTeamName(int index) {
        return "sm_line_" + index;
    }

    /** 玩家退出时清理，避免内存泄漏。 */
    public void remove(Player player) {
        boards.remove(player.getUniqueId());
    }

    /** 配置重载后强制全部重建。 */
    public void rebuildAll() {
        boards.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        if (config.scoreboardEnabled()) {
            refreshAll();
        }
    }

    /** 插件卸载时把玩家还原到主计分板。 */
    public void shutdown() {
        var main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(main);
        }
        boards.clear();
    }
}
