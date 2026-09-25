package mcsc.plugin.soccer.positioning;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.match.MatchState;
import mcsc.plugin.soccer.team.TeamSide;
import mcsc.plugin.soccer.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

/**
 * 站位管理器。
 *
 * 裁判右键站位杖时，以裁判位置为发球点，
 * 将双方在线球员按名字母序填入模板槽位，偏移跟随裁判朝向旋转。
 * 站位后球员立即冻结（由 MatchListener 的暂停冻结机制保证），
 * 直到比赛恢复才解冻。
 */
public class PositioningManager {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    public PositioningManager(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 执行站位。
     *
     * @param referee 执行站位的裁判（右键站位杖的玩家）
     */
    public void position(Player referee) {
        // 只在暂停状态下有效
        if (plugin.match().state() != MatchState.PAUSED) {
            Msg.send(referee, config.prefix(), config.msg("positioning-not-paused"));
            return;
        }

        List<SlotOffset> redSlots = config.positioningSlots(TeamSide.RED);
        List<SlotOffset> blueSlots = config.positioningSlots(TeamSide.BLUE);

        if (redSlots.isEmpty() && blueSlots.isEmpty()) {
            Msg.send(referee, config.prefix(), config.msg("positioning-no-template"));
            return;
        }

        Location anchor = referee.getLocation();
        float refYaw = anchor.getYaw();
        double rad = Math.toRadians(refYaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        int count = 0;
        count += positionTeam(TeamSide.RED, redSlots, anchor, cos, sin, refYaw);
        count += positionTeam(TeamSide.BLUE, blueSlots, anchor, cos, sin, refYaw);

        if (count == 0) {
            Msg.send(referee, config.prefix(), config.msg("positioning-no-players"));
        } else {
            Msg.send(referee, config.prefix(),
                    Msg.replace(config.msg("positioning-done"), "%count%", String.valueOf(count)));
            plugin.match().playSound("positioning");
        }
    }

    /**
     * 将一队的在线球员传送到模板槽位。
     *
     * @return 实际被传送的球员数
     */
    private int positionTeam(TeamSide side, List<SlotOffset> slots,
                             Location anchor, double cos, double sin, float refYaw) {
        if (slots.isEmpty()) {
            return 0;
        }

        List<Player> members = plugin.teams().membersOf(side);
        if (members.isEmpty()) {
            return 0;
        }

        // 按玩家名字母序排列，保证站位分配稳定可预测
        members.sort(Comparator.comparing(Player::getName));

        int maxSlots = Math.min(slots.size(), config.positioningMaxSlots());
        int maxPlayers = Math.min(members.size(), maxSlots);

        int count = 0;
        for (int i = 0; i < maxPlayers; i++) {
            SlotOffset slot = slots.get(i);
            Player player = members.get(i);

            // 旋转计算：模板 (x=右, z=前) → 世界偏移
            // 推导过程：
            //   裁判 yaw=θ 时，前方向量 = (−sin θ, 0, cos θ)，右方向量 = (−cos θ, 0, −sin θ)
            //   世界偏移 = x * 右方 + y * 上 + z * 前方
            //   worldX = −x·cos θ − z·sin θ
            //   worldZ = −x·sin θ + z·cos θ
            double worldX = -slot.x() * cos - slot.z() * sin;
            double worldY = slot.y();
            double worldZ = -slot.x() * sin + slot.z() * cos;

            Location target = anchor.clone().add(worldX, worldY, worldZ);
            // 球员面朝方向 = 裁判朝向 + 模板偏转
            target.setYaw(normalizeYaw(refYaw + slot.yaw()));
            target.setPitch(slot.pitch());

            player.teleport(target);
            count++;
        }

        return count;
    }

    /** 将 yaw 归一化到 [0, 360)。 */
    private float normalizeYaw(float yaw) {
        yaw = yaw % 360f;
        if (yaw < 0) {
            yaw += 360f;
        }
        return yaw;
    }
}
