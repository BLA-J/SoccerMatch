package mcsc.plugin.soccer.listener;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.match.MatchState;
import mcsc.plugin.soccer.util.Perm;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 裁判物品监听器。
 *
 * 通过右键绑定物品触发裁判功能：
 * - 裁判哨（纸改名「裁判哨」）：暂停/继续比赛，等价 /js stop / /js restart
 * - 站位杖（木棍改名「站位杖」）：暂停期间双方球员按模板站位
 *
 * 物品识别方式：材质 + 显示名匹配（不区分大小写）。
 * 仅 OP 或拥有 soccer.referee 权限的玩家可使用。
 */
public class RefereeItemListener implements Listener {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    /** 右键冷却，防止快速连点。 */
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public RefereeItemListener(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        // 只处理右键
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 只处理主手
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        // 权限检查
        if (!Perm.isReferee(player)) {
            return;
        }

        // 功能总开关
        if (!config.refereeItemsEnabled()) {
            return;
        }

        // 检查手持物品
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }

        // 冷却检查
        if (!checkCooldown(player)) {
            return;
        }

        // 裁判哨
        if (matchesItem(item, "whistle")) {
            event.setCancelled(true);
            handleWhistle(player);
            return;
        }

        // 站位杖
        if (matchesItem(item, "positioning-wand")) {
            event.setCancelled(true);
            plugin.positioning().position(player);
        }
    }

    /**
     * 裁判哨逻辑：暂停/继续切换。
     * 完全等价于 /js stop 和 /js restart，含相同的状态提示。
     */
    private void handleWhistle(Player player) {
        if (plugin.match().state() == MatchState.PAUSED) {
            // 暂停中 → 继续
            plugin.match().resume(player);
        } else {
            // 其他状态 → 尝试暂停（IDLE 会提示「比赛未开始」）
            plugin.match().pause(player);
        }
    }

    /**
     * 检查手持物品是否匹配配置中的裁判物品。
     * 匹配规则：材质相同 + 显示名相同（不区分大小写）。
     */
    private boolean matchesItem(ItemStack item, String node) {
        Material mat = config.refereeItemMaterial(node);
        if (mat == null || item.getType() != mat) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String expected = config.refereeItemDisplayName(node);
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return meta.getDisplayName().equalsIgnoreCase(expected.trim());
    }

    /** 检查并更新右键冷却。返回 true 表示可以执行。 */
    private boolean checkCooldown(Player player) {
        long cd = config.refereeItemClickCooldown();
        if (cd <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && now - last < cd) {
            return false;
        }
        lastClick.put(player.getUniqueId(), now);
        return true;
    }
}
