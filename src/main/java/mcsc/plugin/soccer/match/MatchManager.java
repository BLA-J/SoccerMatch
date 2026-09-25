package mcsc.plugin.soccer.match;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.team.TeamSide;
import mcsc.plugin.soccer.util.Msg;
import mcsc.plugin.soccer.util.TimeUtil;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

/**
 * 比赛核心管理器：状态机、计时、比分、上下半场。
 */
public class MatchManager {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    private MatchState state = MatchState.IDLE;
    private MatchHalf half = MatchHalf.FIRST;

    private int remaining;
    private int redScore;
    private int blueScore;

    private BukkitTask timerTask;
    private BukkitTask countdownTask;

    /** 上次进球的时刻，用于绊线钩防抖。 */
    private long lastGoalAt = 0L;

    public MatchManager(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.remaining = config.duration();
    }

    // ==================== 状态查询 ====================

    public MatchState state() {
        return state;
    }

    public MatchHalf half() {
        return half;
    }

    public int remaining() {
        return remaining;
    }

    public int redScore() {
        return redScore;
    }

    public int blueScore() {
        return blueScore;
    }

    /** 当前是否应当冻结球员移动。 */
    public boolean shouldFreeze() {
        if (state == MatchState.COUNTDOWN && config.freezeOnCountdown()) {
            return true;
        }
        return state == MatchState.PAUSED && config.freezeOnPause();
    }

    // ==================== /js start ====================

    public void start(CommandSender sender) {
        if (state.isStarted()) {
            Msg.send(sender, config.prefix(), config.msg("match-already-running"));
            return;
        }

        redScore = 0;
        blueScore = 0;
        half = MatchHalf.FIRST;
        remaining = config.duration();
        lastGoalAt = 0L;

        int cd = config.countdown();
        if (cd <= 0) {
            beginRunning(true);
            return;
        }

        state = MatchState.COUNTDOWN;
        Msg.broadcast(config.prefix(), config.msg("match-starting"));

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int left = cd;

            @Override
            public void run() {
                if (left > 0) {
                    // 3-2-1 大字。必须走 Msg.color 解析颜色码，
                    // 直接 Component.text 会把 &e&l 当字面量显示出来。
                    broadcastRawTitle(
                            Msg.color(config.countdownFormat().replace("%n%", String.valueOf(left))),
                            Component.empty(),
                            2, 12, 2
                    );
                    playSound("countdown-tick");
                    left--;
                } else {
                    cancelCountdown();
                    beginRunning(true);
                }
            }
        }, 0L, 20L);
    }

    /** 正式进入计时状态。 */
    private void beginRunning(boolean announce) {
        state = MatchState.RUNNING;
        if (announce) {
            broadcastTitle("match-start", "%half%", half.display());
            playSound("whistle");
        }
        startTimer();
    }

    private void startTimer() {
        cancelTimer();
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != MatchState.RUNNING) {
                return;
            }
            remaining--;
            if (remaining <= 0) {
                remaining = 0;
                endByTime();
            }
        }, 20L, 20L);
    }

    // ==================== /js stop ====================

    public void pause(CommandSender sender) {
        if (state == MatchState.IDLE) {
            Msg.send(sender, config.prefix(), config.msg("match-not-started"));
            return;
        }
        if (state == MatchState.PAUSED) {
            Msg.send(sender, config.prefix(), config.msg("match-already-paused"));
            return;
        }
        // 开球倒计时途中被叫停，直接终止倒计时并进入暂停
        if (state == MatchState.COUNTDOWN) {
            cancelCountdown();
        }

        state = MatchState.PAUSED;
        Msg.broadcast(config.prefix(), config.msg("match-stopped"));
        broadcastTitle("match-pause");
        playSound("whistle");
    }

    // ==================== /js restart ====================

    public void resume(CommandSender sender) {
        // 场景 A：从未开始过比赛
        if (state == MatchState.IDLE) {
            Msg.send(sender, config.prefix(), config.msg("match-not-started"));
            return;
        }
        // 场景 B：比赛正在跑，并没有暂停过
        if (state != MatchState.PAUSED) {
            Msg.send(sender, config.prefix(), config.msg("match-not-paused"));
            return;
        }

        state = MatchState.RUNNING;
        startTimer();
        Msg.broadcast(config.prefix(), config.msg("match-resumed"));
        broadcastTitle("match-resume");
        playSound("whistle");
    }

    // ==================== /js end ====================

    public void end(CommandSender sender) {
        if (state == MatchState.IDLE) {
            Msg.send(sender, config.prefix(), config.msg("match-not-started"));
            return;
        }
        finish();
        Msg.send(sender, config.prefix(), config.msg("match-ended"));
    }

    /** 倒计时归零自动结束。 */
    private void endByTime() {
        finish();
    }

    /** 统一的收尾流程。 */
    private void finish() {
        cancelTimer();
        cancelCountdown();
        state = MatchState.IDLE;

        String result = resultText();
        broadcastTitle("match-end", "%result%", result);
        Msg.broadcast(config.prefix(), config.msg("match-ended") + " &f" + result);
        playSound("match-end");

        if (config.clearCardsOnMatchEnd() && plugin.cards() != null) {
            plugin.cards().clearAll();
        }
        remaining = config.duration();
    }

    /** 依据比分给出胜负文案。 */
    public String resultText() {
        if (redScore > blueScore) {
            return config.msg("result-red-win");
        }
        if (blueScore > redScore) {
            return config.msg("result-blue-win");
        }
        return config.msg("result-draw");
    }

    // ==================== 进球 ====================

    public void goal(CommandSender sender, TeamSide side, String scorer) {
        if (state == MatchState.IDLE) {
            Msg.send(sender, config.prefix(), config.msg("match-not-started"));
            return;
        }

        // 绊线钩极易在球滚过时连续触发多次红石信号，
        // 这里做冷却拦截，避免一次进球被记成好几分。
        long now = System.currentTimeMillis();
        long cd = config.goalCooldown() * 1000L;
        if (cd > 0 && now - lastGoalAt < cd) {
            Msg.send(sender, config.prefix(), config.msg("goal-cooldown"));
            return;
        }
        lastGoalAt = now;

        if (side == TeamSide.RED) {
            redScore++;
        } else {
            blueScore++;
        }

        String teamDisplay = plugin.teams().displayOf(side);
        String subject = (scorer == null || scorer.isBlank())
                ? teamDisplay
                : teamDisplay + " &f" + scorer;

        broadcastTitle("goal",
                "%team%", subject,
                "%red%", String.valueOf(redScore),
                "%blue%", String.valueOf(blueScore));

        Msg.broadcast(config.prefix(), Msg.replace(config.msg("goal-broadcast"),
                "%team%", subject,
                "%red%", String.valueOf(redScore),
                "%blue%", String.valueOf(blueScore)));

        playSound("goal");
    }

    /** 手动修正比分，用于误判纠正。 */
    public void setScore(TeamSide side, int value) {
        int v = Math.max(0, value);
        if (side == TeamSide.RED) {
            redScore = v;
        } else {
            blueScore = v;
        }
    }

    // ==================== 上下半场 ====================

    public void switchHalf(CommandSender sender, boolean firstHalf) {
        MatchHalf target = MatchHalf.of(firstHalf);
        if (target == half) {
            Msg.send(sender, config.prefix(),
                    Msg.replace(config.msg("half-same"), "%half%", target.display()));
            return;
        }

        half = target;

        if (config.resetTimerOnHalfChange()) {
            remaining = config.duration();
        }
        if (config.pauseAfterHalfChange() && state == MatchState.RUNNING) {
            state = MatchState.PAUSED;
            cancelTimer();
        }

        Msg.broadcast(config.prefix(),
                Msg.replace(config.msg("half-switched"), "%half%", half.display()));
        broadcastTitle("half-change", "%half%", half.display());
        playSound("whistle");
    }

    // ==================== 广播工具 ====================

    /**
     * 广播配置里的 Title。
     * 这里用 Adventure 一次性发送主副标题，
     * 因此不存在原版 /title 必须先 subtitle 后 title 的顺序问题。
     */
    public void broadcastTitle(String node, String... placeholders) {
        String main = Msg.replace(config.titleMain(node), placeholders);
        String sub = Msg.replace(config.titleSub(node), placeholders);

        broadcastRawTitle(
                Msg.color(main),
                Msg.color(sub),
                config.titleFadeIn(), config.titleStay(), config.titleFadeOut()
        );
    }

    private void broadcastRawTitle(Component main, Component sub, int fadeIn, int stay, int fadeOut) {
        Title title = Title.title(main, sub, Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        ));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }
    }

    public void playSound(String node) {
        Sound sound = config.sound(node);
        if (sound == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(sound);
        }
    }

    // ==================== 任务清理 ====================

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public void shutdown() {
        cancelTimer();
        cancelCountdown();
        state = MatchState.IDLE;
    }

    /** 配置重载后同步时长上限。 */
    public void onConfigReload() {
        if (state == MatchState.IDLE) {
            remaining = config.duration();
        }
    }

    /** BossBar 进度计算用。 */
    public float progress() {
        int total = config.duration();
        if (total <= 0) {
            return 0f;
        }
        float p = (float) remaining / (float) total;
        return Math.max(0f, Math.min(1f, p));
    }

    public String formattedTime() {
        return TimeUtil.format(remaining);
    }
}
