package mcsc.plugin.soccer.command;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.util.Msg;
import mcsc.plugin.soccer.util.Perm;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /y &lt;玩家ID&gt;  出示黄牌
 * /y undo       撤销上一张黄牌
 */
public class YellowCardCommand implements CommandExecutor, TabCompleter {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    public YellowCardCommand(SoccerPlugin plugin, SoccerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!Perm.isRefereeOrAutomation(sender)) {
            Msg.send(sender, config.prefix(), config.msg("no-permission"));
            return true;
        }

        if (args.length != 1) {
            Msg.send(sender, config.prefix(), config.msg("usage-yellow"));
            return true;
        }

        // 撤销分支优先于玩家名判断
        if (args[0].equalsIgnoreCase("undo")) {
            plugin.cards().undoLast(sender);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            Msg.send(sender, config.prefix(),
                    Msg.replace(config.msg("player-not-found"), "%player%", args[0]));
            return true;
        }

        plugin.cards().giveYellow(sender, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length != 1 || !Perm.isReferee(sender)) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        if ("undo".startsWith(prefix)) {
            out.add("undo");
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(p.getName());
            }
        }
        return out;
    }
}
