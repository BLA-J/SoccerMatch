package mcsc.plugin.soccer.util;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * 权限判定。
 */
public final class Perm {

    public static final String REFEREE = "soccer.referee";

    private Perm() {
    }

    /** 裁判权限：OP 或显式授予 soccer.referee。 */
    public static boolean isReferee(CommandSender sender) {
        return sender.isOp() || sender.hasPermission(REFEREE);
    }

    /**
     * 自动化来源：命令方块与控制台。
     * 球门绊线钩连接的命令方块需要能直接调用 /js goal，
     * 而命令方块的 isOp() 通常为 false，所以必须单独放行。
     */
    public static boolean isAutomation(CommandSender sender) {
        return sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender;
    }

    /** 裁判或自动化来源均可执行。 */
    public static boolean isRefereeOrAutomation(CommandSender sender) {
        return isReferee(sender) || isAutomation(sender);
    }
}
