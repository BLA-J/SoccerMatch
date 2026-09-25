package mcsc.plugin.soccer.card;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.team.TeamSide;
import mcsc.plugin.soccer.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 红黄牌管理。
 * 支持累计黄牌自动罚下、罚下传送、以及 /y undo 完整回滚。
 */
public class CardManager {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    /** 每名球员的黄牌数。 */
    private final Map<UUID, Integer> yellowCounts = new HashMap<>();

    /** 已被罚下的球员。 */
    private final Set<UUID> sentOff = new HashSet<>();

    /** 出牌历史栈，栈顶为最近一张黄牌。 */
    private final Deque<CardRecord> history = new ArrayDeque<>();

    public CardManager(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public int yellowCount(Player player) {
        return yellowCounts.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean isSentOff(Player player) {
        return sentOff.contains(player.getUniqueId());
    }

    // ==================== 黄牌 ====================

    public void giveYellow(CommandSender sender, Player target) {
        UUID id = target.getUniqueId();
        int count = yellowCounts.merge(id, 1, Integer::sum);

        CardRecord record = new CardRecord(id, target.getName());

        // 先播报黄牌本身
        plugin.match().broadcastTitle("yellow-card", "%player%", target.getName());
        plugin.match().playSound("yellow-card");
        Msg.broadcast(config.prefix(), Msg.replace(config.msg("card-yellow-broadcast"),
                "%player%", target.getName(),
                "%count%", String.valueOf(count)));

        // 累计达到阈值，自动升级为罚下
        if (count >= config.yellowToRed() && !sentOff.contains(id)) {
            Msg.broadcast(config.prefix(), Msg.replace(config.msg("card-auto-red"),
                    "%player%", target.getName(),
                    "%count%", String.valueOf(count)));

            // 延迟一点再出红牌，避免两个 Title 瞬间叠在一起看不清。
            // 若这段时间内裁判已撤销该黄牌，则放弃罚下。
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (record.cancelled()) {
                    return;
                }
                if (target.isOnline()) {
                    applySendOff(target, record);
                }
            }, 40L);
        }

        history.push(record);
    }

    // ==================== 红牌 ====================

    public void giveRed(CommandSender sender, Player target) {
        CardRecord record = new CardRecord(target.getUniqueId(), target.getName());
        applySendOff(target, record);
        history.push(record);
    }

    /**
     * 执行罚下：播报 + 传送 + 可选移除 tag / 切旁观者。
     * 所有变更前的状态都写入 record，便于撤销。
     */
    private void applySendOff(Player target, CardRecord record) {
        UUID id = target.getUniqueId();

        Location before = target.getLocation().clone();
        GameMode beforeMode = target.getGameMode();
        String removedTag = null;

        plugin.match().broadcastTitle("red-card", "%player%", target.getName());
        plugin.match().playSound("red-card");
        Msg.broadcast(config.prefix(), Msg.replace(config.msg("card-red-broadcast"),
                "%player%", target.getName()));

        sentOff.add(id);

        if (config.removeTeamTagOnSendOff()) {
            TeamSide side = plugin.teams().sideOf(target);
            if (side != null) {
                removedTag = plugin.teams().tagOf(side);
                target.removeScoreboardTag(removedTag);
            }
        }

        if (config.setSpectatorOnSendOff()) {
            target.setGameMode(GameMode.SPECTATOR);
        }

        record.markTriggeredRed(before, beforeMode, removedTag);

        if (config.sendOffEnabled()) {
            Location dest = config.sendOffLocation();
            if (dest != null) {
                target.teleportAsync(dest);
            }
        }
    }

    // ==================== 撤销 ====================

    public void undoLast(CommandSender sender) {
        if (history.isEmpty()) {
            Msg.send(sender, config.prefix(), config.msg("card-undo-empty"));
            return;
        }

        CardRecord record = history.pop();
        record.cancel();
        UUID id = record.uuid();

        // 黄牌计数回退
        int now = Math.max(0, yellowCounts.getOrDefault(id, 0) - 1);
        if (now == 0) {
            yellowCounts.remove(id);
        } else {
            yellowCounts.put(id, now);
        }

        // 播报撤销
        plugin.match().broadcastTitle("yellow-undo", "%player%", record.playerName());
        Msg.broadcast(config.prefix(), Msg.replace(config.msg("card-undo-success"),
                "%player%", record.playerName(),
                "%count%", String.valueOf(now)));

        // 若这张牌导致了罚下，一并回滚罚下状态
        if (record.triggeredRed()) {
            sentOff.remove(id);
            Player target = Bukkit.getPlayer(id);

            if (target != null && target.isOnline()) {
                if (record.removedTag() != null) {
                    target.addScoreboardTag(record.removedTag());
                }
                if (record.previousGameMode() != null) {
                    target.setGameMode(record.previousGameMode());
                }
                if (config.undoRestoresPosition() && record.previousLocation() != null) {
                    target.teleportAsync(record.previousLocation());
                    Msg.broadcast(config.prefix(), Msg.replace(config.msg("card-undo-restored"),
                            "%player%", record.playerName()));
                }
            }
        }
    }

    // ==================== 清理 ====================

    public void clearAll() {
        yellowCounts.clear();
        sentOff.clear();
        history.clear();
    }
}
