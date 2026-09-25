package mcsc.plugin.soccer.command;

import mcsc.plugin.soccer.SoccerPlugin;
import mcsc.plugin.soccer.config.SoccerConfig;
import mcsc.plugin.soccer.util.Msg;
import mcsc.plugin.soccer.util.Perm;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /soccerstatus true   切到上半场
 * /soccerstatus false  切到下半场
 */
public class SoccerStatusCommand implements CommandExecutor, TabCompleter {

    private final SoccerPlugin plugin;
    private final SoccerConfig config;

    public SoccerStatusCommand(SoccerPlugin plugin, SoccerConfig config) {
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
            Msg.send(sender, config.prefix(), config.msg("usage-status"));
            return true;
        }

        String raw = args[0].toLowerCase(Locale.ROOT);
        boolean firstHalf;
        if (raw.equals("true")) {
            firstHalf = true;
        } else if (raw.equals("false")) {
            firstHalf = false;
        } else {
            Msg.send(sender, config.prefix(), config.msg("usage-status"));
            return true;
        }

        plugin.match().switchHalf(sender, firstHalf);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length != 1 || !Perm.isReferee(sender)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        String p = args[0].toLowerCase(Locale.ROOT);
        if ("true".startsWith(p)) {
            out.add("true");
        }
        if ("false".startsWith(p)) {
            out.add("false");
        }
        return out;
    }
}
