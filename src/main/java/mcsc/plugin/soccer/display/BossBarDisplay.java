package mcsc.plugin.soccer.display;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.match.MatchState;
import mcsc.plugin.soccer.util.Msg;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 屏幕正上方的 BossBar 计时条。
 * 全服共用一条，比赛未开始时自动隐藏，避免常驻挡视线。
 */
public class BossBarDisplay {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    private BossBar bar;
    private final Set<UUID> shown = new HashSet<>();

    public BossBarDisplay(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** 每秒调用一次。 */
    public void refresh() {
        if (!config.bossBarEnabled()) {
            hideAll();
            return;
        }

        MatchState state = plugin.match().state();
        if (state == MatchState.IDLE) {
            hideAll();
            return;
        }

        if (bar == null) {
            bar = BossBar.bossBar(
                    Msg.color(config.bossBarTitle()),
                    1.0f,
                    config.bossBarColor(),
                    config.bossBarStyle()
            );
        }

        bar.name(Msg.color(Placeholders.apply(plugin, config.bossBarTitle(), null)));
        bar.progress(plugin.match().progress());
        bar.overlay(config.bossBarStyle());

        int warn = config.bossBarWarnTime();
        boolean warning = warn > 0 && plugin.match().remaining() <= warn;
        bar.color(warning ? config.bossBarWarnColor() : config.bossBarColor());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (shown.add(p.getUniqueId())) {
                p.showBossBar(bar);
            }
        }
        // 清理已下线玩家的记录
        shown.removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    public void hideAll() {
        if (bar == null) {
            shown.clear();
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(bar);
        }
        shown.clear();
    }

    public void remove(Player player) {
        if (bar != null) {
            player.hideBossBar(bar);
        }
        shown.remove(player.getUniqueId());
    }

    /** 配置重载后重建，使颜色/样式立即生效。 */
    public void rebuild() {
        hideAll();
        bar = null;
    }

    public void shutdown() {
        hideAll();
        bar = null;
    }
}
