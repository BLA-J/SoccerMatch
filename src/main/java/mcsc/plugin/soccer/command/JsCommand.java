package mcsc.plugin.soccer.command;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.team.TeamSide;
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
 * /js start    开始比赛
 * /js stop     暂停
 * /js restart  继续
 * /js end      结束
 * /js goal &lt;red|blue&gt; [球员]  进球计分（供球门命令方块调用）
 * /js score &lt;red|blue&gt; &lt;分数&gt; 直接修正比分
 * /js reload   重载配置
 */
public class JsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS =
            List.of("start", "stop", "restart", "end", "goal", "score", "reload");

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    public JsCommand(SoccerPlugin plugin, SoccerConfig config) {
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

        if (args.length == 0) {
            Msg.send(sender, config.prefix(), config.msg("usage-js"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> plugin.match().start(sender);
            case "stop" -> plugin.match().pause(sender);
            case "restart" -> plugin.match().resume(sender);
            case "end" -> plugin.match().end(sender);
            case "goal" -> handleGoal(sender, args);
            case "score" -> handleScore(sender, args);
            case "reload" -> handleReload(sender);
            default -> Msg.send(sender, config.prefix(), config.msg("usage-js"));
        }
        return true;
    }

    private void handleGoal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, config.prefix(), config.msg("goal-usage"));
            return;
        }
        TeamSide side = TeamSide.parse(args[1]);
        if (side == null) {
            Msg.send(sender, config.prefix(), config.msg("goal-usage"));
            return;
        }
        String scorer = args.length >= 3 ? args[2] : null;
        plugin.match().goal(sender, side, scorer);
    }

    private void handleScore(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.send(sender, config.prefix(), "&c用法: /js score <red|blue> <分数>");
            return;
        }
        TeamSide side = TeamSide.parse(args[1]);
        if (side == null) {
            Msg.send(sender, config.prefix(), "&c队伍只能是 red 或 blue");
            return;
        }
        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            Msg.send(sender, config.prefix(), "&c分数必须是整数: &e" + args[2]);
            return;
        }
        plugin.match().setScore(side, value);
        Msg.send(sender, config.prefix(), "&a已将 "
                + plugin.teams().displayOf(side) + " &a的比分设为 &f" + Math.max(0, value));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadAll();
        Msg.send(sender, config.prefix(), config.msg("reloaded"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (!Perm.isReferee(sender)) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("goal") || sub.equals("score")) {
                return filter(List.of("red", "blue"), args[1]);
            }
            return List.of();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("goal")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filter(names, args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("score")) {
            return filter(List.of("0", "1", "2", "3"), args[2]);
        }

        return List.of();
    }

    private List<String> filter(List<String> source, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(s);
            }
        }
        return out;
    }
}
