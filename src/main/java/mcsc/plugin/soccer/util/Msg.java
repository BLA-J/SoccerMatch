package mcsc.plugin.soccer.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 文本与消息工具。
 * 统一把配置文件里的 & 颜色码转换为 Adventure Component。
 */
public final class Msg {

    private static final LegacyComponentSerializer AMP =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexCharacter('#')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private Msg() {
    }

    /** 把带 & 颜色码的字符串转成 Component。 */
    public static Component color(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        return AMP.deserialize(raw);
    }

    /** 批量替换占位符，成对传入 key/value。 */
    public static String replace(String raw, String... pairs) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out = out.replace(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return out;
    }

    /** 发送一条带前缀的消息给指定接收者。 */
    public static void send(CommandSender to, String prefix, String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        to.sendMessage(color(prefix + raw));
    }

    /** 向全服所有在线玩家与控制台广播。 */
    public static void broadcast(String prefix, String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        Component msg = color(prefix + raw);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}
