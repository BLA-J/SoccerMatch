package mcsc.plugin.soccer.config;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.positioning.SlotOffset;
import mcsc.plugin.soccer.team.TeamSide;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 配置封装层。
 * 所有对 config.yml 的读取都集中在这里，方便统一处理默认值与容错。
 */
public class SoccerConfig {

    private final SoccerPlugin plugin;
    private FileConfiguration cfg;

    public SoccerConfig(SoccerPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    // ==================== 比赛 ====================

    public int duration() {
        return Math.max(1, cfg.getInt("match.duration", 600));
    }

    public int countdown() {
        return Math.max(0, cfg.getInt("match.countdown", 3));
    }

    public boolean freezeOnCountdown() {
        return cfg.getBoolean("match.freeze-on-countdown", true);
    }

    public boolean freezeOnPause() {
        return cfg.getBoolean("match.freeze-on-pause", true);
    }

    public boolean freezeBlocksInteract() {
        return cfg.getBoolean("match.freeze-blocks-interact", true);
    }

    public boolean freezeExemptOp() {
        return cfg.getBoolean("match.freeze-exempt-op", true);
    }

    public boolean freezePlayersOnly() {
        return cfg.getBoolean("match.freeze-players-only", true);
    }

    public boolean resetTimerOnHalfChange() {
        return cfg.getBoolean("match.reset-timer-on-half-change", true);
    }

    public boolean pauseAfterHalfChange() {
        return cfg.getBoolean("match.pause-after-half-change", true);
    }

    /** 进球冷却秒数，用于绊线钩红石防抖。 */
    public int goalCooldown() {
        return Math.max(0, cfg.getInt("match.goal-cooldown", 3));
    }

    // ==================== 计分板 ====================

    public boolean scoreboardEnabled() {
        return cfg.getBoolean("scoreboard.enabled", true);
    }

    public String scoreboardTitle() {
        return cfg.getString("scoreboard.title", "&6&l足球比赛");
    }

    public String objectiveName() {
        String raw = cfg.getString("scoreboard.objective-name", "soccer_match");
        if (raw == null || raw.isBlank()) {
            raw = "soccer_match";
        }
        // objective 名称长度与字符有限制，做一次安全清洗
        String safe = raw.replaceAll("[^A-Za-z0-9_.\\-]", "_");
        return safe.length() > 16 ? safe.substring(0, 16) : safe;
    }

    public List<String> scoreboardLines() {
        List<String> lines = cfg.getStringList("scoreboard.lines");
        if (lines.isEmpty()) {
            lines = List.of("&f剩余: &a%time%", "&c红队 %red%", "&9蓝队 %blue%");
        }
        // 侧边栏最多 15 行
        return lines.size() > 15 ? lines.subList(0, 15) : lines;
    }

    // ==================== BossBar ====================

    public boolean bossBarEnabled() {
        return cfg.getBoolean("bossbar.enabled", true);
    }

    public String bossBarTitle() {
        return cfg.getString("bossbar.title", "&f%half%  &e%time%");
    }

    public BossBar.Color bossBarColor() {
        return parseColor(cfg.getString("bossbar.color", "GREEN"), BossBar.Color.GREEN);
    }

    public BossBar.Color bossBarWarnColor() {
        return parseColor(cfg.getString("bossbar.warn-color", "RED"), BossBar.Color.RED);
    }

    public int bossBarWarnTime() {
        return Math.max(0, cfg.getInt("bossbar.warn-time", 60));
    }

    public BossBar.Overlay bossBarStyle() {
        String raw = cfg.getString("bossbar.style", "PROGRESS");
        try {
            return BossBar.Overlay.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("bossbar.style 配置值无效: " + raw + "，已回退为 PROGRESS");
            return BossBar.Overlay.PROGRESS;
        }
    }

    private BossBar.Color parseColor(String raw, BossBar.Color fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return BossBar.Color.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("BossBar 颜色配置值无效: " + raw + "，已回退为 " + fallback);
            return fallback;
        }
    }

    // ==================== 队伍 ====================

    public String redTag() {
        return cfg.getString("teams.red.tag", "red");
    }

    public String blueTag() {
        return cfg.getString("teams.blue.tag", "blue");
    }

    public String redDisplay() {
        return cfg.getString("teams.red.display", "&c红队");
    }

    public String blueDisplay() {
        return cfg.getString("teams.blue.display", "&9蓝队");
    }

    public String spectatorDisplay() {
        return cfg.getString("teams.spectator-display", "&7观众");
    }

    // ==================== 红黄牌 ====================

    public int yellowToRed() {
        return Math.max(1, cfg.getInt("cards.yellow-to-red", 3));
    }

    public boolean sendOffEnabled() {
        return cfg.getBoolean("cards.send-off.enabled", true);
    }

    /**
     * 罚下传送点。世界不存在时返回 null 并输出警告，调用方需自行兜底。
     */
    public Location sendOffLocation() {
        String worldName = cfg.getString("cards.send-off.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning(
                    "cards.send-off.world 配置的世界 '" + worldName + "' 不存在，罚下传送已跳过。"
                            + "请检查世界名是否与服务器实际名称完全一致（区分大小写）。");
            return null;
        }
        return new Location(
                world,
                cfg.getDouble("cards.send-off.x", 0.5),
                cfg.getDouble("cards.send-off.y", 100.0),
                cfg.getDouble("cards.send-off.z", 0.5),
                (float) cfg.getDouble("cards.send-off.yaw", 0.0),
                (float) cfg.getDouble("cards.send-off.pitch", 0.0)
        );
    }

    public boolean removeTeamTagOnSendOff() {
        return cfg.getBoolean("cards.send-off.remove-team-tag", true);
    }

    public boolean setSpectatorOnSendOff() {
        return cfg.getBoolean("cards.send-off.set-spectator", false);
    }

    public boolean undoRestoresPosition() {
        return cfg.getBoolean("cards.undo-restores-position", true);
    }

    public boolean clearCardsOnMatchEnd() {
        return cfg.getBoolean("cards.clear-on-match-end", true);
    }

    // ==================== Title ====================

    public String titleMain(String node) {
        return cfg.getString("titles." + node + ".main", "");
    }

    public String titleSub(String node) {
        return cfg.getString("titles." + node + ".sub", "");
    }

    /** 开球 3-2-1 大字样式，%n% 为当前数字。 */
    public String countdownFormat() {
        return cfg.getString("titles.countdown-format", "&e&l%n%");
    }

    public int titleFadeIn() {
        return cfg.getInt("titles.fade-in", 10);
    }

    public int titleStay() {
        return cfg.getInt("titles.stay", 50);
    }

    public int titleFadeOut() {
        return cfg.getInt("titles.fade-out", 10);
    }

    // ==================== 音效 ====================

    /**
     * 读取音效。使用 Adventure Sound + Key，
     * 避免依赖 Bukkit Sound 枚举常量（不同版本间会增删改）。
     * 配置为空字符串时返回 null 表示关闭该音效。
     */
    public Sound sound(String node) {
        String raw = cfg.getString("sounds." + node, "");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Key key = raw.contains(":") ? Key.key(raw) : Key.key("minecraft", raw);
            return Sound.sound(key, Sound.Source.MASTER, 1.0f, 1.0f);
        } catch (Exception e) {
            plugin.getLogger().warning("音效 sounds." + node + " 配置值无效: " + raw);
            return null;
        }
    }

    // ==================== 消息 ====================

    public String prefix() {
        return cfg.getString("messages.prefix", "&8[&6足球&8] &r");
    }

    public String msg(String node) {
        return cfg.getString("messages." + node, "&c[缺少配置项: messages." + node + "]");
    }

    // ==================== 裁判物品 ====================

    public boolean refereeItemsEnabled() {
        return cfg.getBoolean("referee-items.enabled", true);
    }

    /** 右键冷却毫秒数。 */
    public long refereeItemClickCooldown() {
        return cfg.getLong("referee-items.click-cooldown", 500L);
    }

    /** 裁判物品的材质，配置无效时返回 null。 */
    public Material refereeItemMaterial(String node) {
        String raw = cfg.getString("referee-items." + node + ".material", "");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Material mat = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        if (mat == null) {
            plugin.getLogger().warning("referee-items." + node + ".material 配置值无效: " + raw);
        }
        return mat;
    }

    /** 裁判物品的显示名。 */
    public String refereeItemDisplayName(String node) {
        return cfg.getString("referee-items." + node + ".display-name", "");
    }

    // ==================== 站位模板 ====================

    /** 每队站位槽位上限。 */
    public int positioningMaxSlots() {
        return Math.max(1, cfg.getInt("positioning.max-slots", 11));
    }

    /**
     * 取得某队的站位模板槽位列表。
     * 配置不存在或格式错误时返回空列表。
     */
    public List<SlotOffset> positioningSlots(TeamSide side) {
        String key = side == TeamSide.RED ? "red" : "blue";
        ConfigurationSection root = cfg.getConfigurationSection("positioning");
        if (root == null) {
            return List.of();
        }
        List<Map<?, ?>> rawList = root.getMapList(key);
        if (rawList.isEmpty()) {
            return List.of();
        }

        List<SlotOffset> slots = new ArrayList<>(rawList.size());
        for (Map<?, ?> map : rawList) {
            try {
                double x = toDouble(map.get("x"), 0.0);
                double y = toDouble(map.get("y"), 0.0);
                double z = toDouble(map.get("z"), 0.0);
                float yaw = (float) toDouble(map.get("yaw"), 0.0);
                float pitch = (float) toDouble(map.get("pitch"), 0.0);
                slots.add(new SlotOffset(x, y, z, yaw, pitch));
            } catch (Exception e) {
                plugin.getLogger().warning("positioning." + key + " 中存在格式错误的槽位，已跳过: " + map);
            }
        }
        return slots;
    }

    /** 安全地把 Object 转为 double。 */
    private double toDouble(Object val, double def) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }
}
