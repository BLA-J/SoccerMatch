package mcsc.plugin.soccer.team;

import mcsc.plugin.soccer.config.SoccerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 队伍管理。
 * 队伍归属完全基于原版 tag，裁判用 /tag @p add red 分队即可，
 * 插件不额外维护名单，避免与服务器现有流程冲突。
 */
public class TeamManager {

    private final SoccerConfig config;

    public TeamManager(SoccerConfig config) {
        this.config = config;
    }

    /**
     * 取得玩家所属队伍，无队伍返回 null。
     * 同时拥有两个 tag 时以红队优先，并在控制台提示。
     */
    public TeamSide sideOf(Player player) {
        var tags = player.getScoreboardTags();
        boolean red = tags.contains(config.redTag());
        boolean blue = tags.contains(config.blueTag());

        if (red && blue) {
            Bukkit.getLogger().warning("[SoccerMatch] 玩家 " + player.getName()
                    + " 同时拥有红蓝两队 tag，已按红队处理，请检查分队。");
            return TeamSide.RED;
        }
        if (red) {
            return TeamSide.RED;
        }
        if (blue) {
            return TeamSide.BLUE;
        }
        return null;
    }

    /** 队伍显示名（含颜色码），无队伍时返回观众文案。 */
    public String displayOf(TeamSide side) {
        if (side == null) {
            return config.spectatorDisplay();
        }
        return side == TeamSide.RED ? config.redDisplay() : config.blueDisplay();
    }

    /** 玩家的队伍显示名。 */
    public String displayOf(Player player) {
        return displayOf(sideOf(player));
    }

    /** 该队伍对应的 tag 名。 */
    public String tagOf(TeamSide side) {
        return side == TeamSide.RED ? config.redTag() : config.blueTag();
    }

    /** 取得某队全部在线成员。 */
    public List<Player> membersOf(TeamSide side) {
        List<Player> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sideOf(p) == side) {
                list.add(p);
            }
        }
        return list;
    }

    /** 取得所有已分队的在线球员（不含观众）。 */
    public List<Player> allPlayers() {
        List<Player> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sideOf(p) != null) {
                list.add(p);
            }
        }
        return list;
    }

    /** 移除玩家的所有队伍 tag（用于罚下）。 */
    public void clearTags(Player player) {
        player.removeScoreboardTag(config.redTag());
        player.removeScoreboardTag(config.blueTag());
    }
}
