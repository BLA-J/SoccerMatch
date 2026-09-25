package mcsc.plugin.soccer.positioning;

/**
 * 站位模板中的一个槽位偏移。
 *
 * 坐标系以裁判面朝方向为基准：
 *   x     = 右侧偏移（正=裁判右手边，负=左手边）
 *   y     = 垂直偏移（正=上方，负=下方）
 *   z     = 前方偏移（正=裁判前方，负=后方）
 *   yaw   = 球员面朝偏转（0=同裁判朝向，180=面向裁判）
 *   pitch = 俯仰角（0=平视，90=低头看地）
 */
public record SlotOffset(double x, double y, double z, float yaw, float pitch) {
}
