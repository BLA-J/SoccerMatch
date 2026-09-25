package mcsc.plugin.soccer.team;

/**
 * 队伍方。
 */
public enum TeamSide {
    RED,
    BLUE;

    public static TeamSide parse(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase()) {
            case "red", "r", "红", "红队" -> RED;
            case "blue", "b", "蓝", "蓝队" -> BLUE;
            default -> null;
        };
    }
}
