package mcsc.plugin.soccer.util;

/**
 * 时间格式化工具。
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * 把秒数格式化为 mm:ss。
     * 超过 60 分钟时自动扩展为 hh:mm:ss。
     */
    public static String format(int totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
