package mcsc.plugin.soccer.command;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.team.TeamSide;
import mcsc.plugin.soccer.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /tc &lt;内容&gt;
 * 队内聊天：仅同队队员与 OP 可见。
 */
public class TeamChatCommand implements CommandExecutor, TabCompleter {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    public TeamChatCommand(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!(sender instanceof Player player)) {
            Msg.send(sender, config.prefix(), "&c该命令只能由玩家使用。");
            return true;
        }

        if (args.length == 0) {
            Msg.send(sender, config.prefix(), config.msg("usage-tc"));
            return true;
        }

        TeamSide side = plugin.teams().sideOf(player);
        if (side == null) {
            Msg.send(sender, config.prefix(), config.msg("team-chat-no-team"));
            return true;
        }

        String content = String.join(" ", args);

        // 玩家输入的内容不做颜色码解析，避免普通球员刷特效或伪造系统提示
        String template = Msg.replace(config.msg("team-chat-format"),
                "%team%", plugin.teams().displayOf(side),
                "%player%", player.getName());

        int idx = template.indexOf("%message%");
        Component line;
        if (idx >= 0) {
            line = Msg.color(template.substring(0, idx))
                    .append(Component.text(content))
                    .append(Msg.color(template.substring(idx + "%message%".length())));
        } else {
            line = Msg.color(template).append(Component.text(" " + content));
        }

        int teammates = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean self = p.getUniqueId().equals(player.getUniqueId());
            boolean sameTeam = plugin.teams().sideOf(p) == side;
            // OP 可旁听全部队内频道，方便裁判掌握场上沟通
            if (sameTeam || p.isOp()) {
                p.sendMessage(line);
                if (sameTeam && !self) {
                    teammates++;
                }
            }
        }

        // 控制台同步一份，便于赛后人工审核
        Bukkit.getConsoleSender().sendMessage(line);

        if (teammates == 0) {
            Msg.send(sender, config.prefix(), "&7队内暂无其他在线队友，消息已记录。");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        return List.of();
    }
}
