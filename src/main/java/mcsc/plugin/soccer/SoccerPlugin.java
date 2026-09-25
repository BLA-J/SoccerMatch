package mcsc.plugin.soccer;

import mcsc.plugin.soccer.card.CardManager;
import mcsc.plugin.soccer.command.JsCommand;
import mcsc.plugin.soccer.command.RedCardCommand;
import mcsc.plugin.soccer.command.SoccerStatusCommand;
import mcsc.plugin.soccer.command.TeamChatCommand;
import mcsc.plugin.soccer.command.YellowCardCommand;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.display.BossBarDisplay;
import mcsc.plugin.soccer.display.ScoreboardDisplay;
import mcsc.plugin.soccer.listener.MatchListener;
import mcsc.plugin.soccer.listener.RefereeItemListener;
import mcsc.plugin.soccer.match.MatchManager;
import mcsc.plugin.soccer.positioning.PositioningManager;
import mcsc.plugin.soccer.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * SoccerMatch 主类。
 * 目标服务端：Paper 26.2 / Java 25
 */
public class SoccerPlugin extends JavaPlugin {

    private SoccerConfig config;
    private TeamManager teams;
    private MatchManager match;
    private CardManager cards;
    private ScoreboardDisplay scoreboard;
    private BossBarDisplay bossBar;
    private PositioningManager positioning;

    private BukkitTask displayTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new SoccerConfig(this);
        this.teams = new TeamManager(config);
        this.match = new MatchManager(this, config);
        this.cards = new CardManager(this, config);
        this.scoreboard = new ScoreboardDisplay(this, config);
        this.bossBar = new BossBarDisplay(this, config);
        this.positioning = new PositioningManager(this, config);

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new MatchListener(this, config), this);
        Bukkit.getPluginManager().registerEvents(new RefereeItemListener(this, config), this);
        startDisplayTask();

        getLogger().info("SoccerMatch 已启用。使用 /js start 开始比赛。");
    }

    @Override
    public void onDisable() {
        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }
        if (match != null) {
            match.shutdown();
        }
        if (bossBar != null) {
            bossBar.shutdown();
        }
        if (scoreboard != null) {
            scoreboard.shutdown();
        }
        getLogger().info("SoccerMatch 已卸载。");
    }

    private void registerCommands() {
        bind("y", new YellowCardCommand(this, config));
        bind("r", new RedCardCommand(this, config));
        bind("js", new JsCommand(this, config));
        bind("soccerstatus", new SoccerStatusCommand(this, config));
        bind("tc", new TeamChatCommand(this, config));
    }

    private <T extends CommandExecutor & TabCompleter> void bind(String name, T handler) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("命令 /" + name + " 未在 plugin.yml 中注册，该功能不可用！");
            return;
        }
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);
    }

    /** 每秒刷新一次计分板与 BossBar。 */
    private void startDisplayTask() {
        if (displayTask != null) {
            displayTask.cancel();
        }
        displayTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            scoreboard.refreshAll();
            bossBar.refresh();
        }, 20L, 20L);
    }

    /** /js reload 调用。 */
    public void reloadAll() {
        config.reload();
        match.onConfigReload();
        bossBar.rebuild();
        scoreboard.rebuildAll();
        startDisplayTask();
    }

    // ==================== 对外访问 ====================

    public SoccerConfig config() {
        return config;
    }

    public TeamManager teams() {
        return teams;
    }

    public MatchManager match() {
        return match;
    }

    public CardManager cards() {
        return cards;
    }

    public ScoreboardDisplay scoreboard() {
        return scoreboard;
    }

    public BossBarDisplay bossBar() {
        return bossBar;
    }

    public PositioningManager positioning() {
        return positioning;
    }
}
