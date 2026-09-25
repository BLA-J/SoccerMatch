package mcsc.plugin.soccer.listener;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 冻结控制与玩家进出清理。
 */
public class MatchListener implements Listener {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    /** 冻结提示的冷却，避免刷屏。 */
    private final Map<UUID, Long> lastNotice = new HashMap<>();

    public MatchListener(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** 判断该玩家此刻是否应被冻结。 */
    private boolean frozen(Player player) {
        if (!plugin.match().shouldFreeze()) {
            return false;
        }
        if (config.freezeExemptOp() && player.isOp()) {
            return false;
        }
        if (config.freezePlayersOnly() && plugin.teams().sideOf(player) == null) {
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        var from = event.getFrom();
        var to = event.getTo();

        // 只转动视角不算移动，允许玩家四处张望
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        if (!frozen(event.getPlayer())) {
            return;
        }

        // 用 setTo 而不是 setCancelled，画面更平滑不会来回抽搐。
        // 保留 to 的视角，玩家被定住但仍可自由转头观察场上。
        var fixed = from.clone();
        fixed.setYaw(to.getYaw());
        fixed.setPitch(to.getPitch());
        event.setTo(fixed);
        notice(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!config.freezeBlocksInteract()) {
            return;
        }
        if (frozen(event.getPlayer())) {
            event.setCancelled(true);
            notice(event.getPlayer());
        }
    }

    /** 冻结期间禁止击打实体，防止提前把硫磺怪踢出去。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!config.freezeBlocksInteract()) {
            return;
        }
        if (event.getDamager() instanceof Player player && frozen(player)) {
            event.setCancelled(true);
            notice(player);
        }
    }

    private void notice(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastNotice.get(player.getUniqueId());
        if (last != null && now - last < 3000L) {
            return;
        }
        lastNotice.put(player.getUniqueId(), now);
        player.sendActionBar(Msg.color(config.msg("frozen")));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.scoreboard().refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        plugin.scoreboard().remove(p);
        plugin.bossBar().remove(p);
        lastNotice.remove(p.getUniqueId());
    }
}
