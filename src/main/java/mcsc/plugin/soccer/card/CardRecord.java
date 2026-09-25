package mcsc.plugin.soccer.card;

import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.UUID;

/**
 * 一次出牌记录，用于 /y undo 回滚。
 */
public class CardRecord {

    private final UUID uuid;
    private final String playerName;

    /** 这张黄牌是否触发了累计罚下。 */
    private boolean triggeredRed;

    /** 罚下前的位置，用于撤销时传送回去。 */
    private Location previousLocation;

    /** 罚下前的游戏模式，用于撤销时还原。 */
    private GameMode previousGameMode;

    /** 罚下时被移除的队伍 tag，用于撤销时补回。 */
    private String removedTag;

    /**
     * 该记录是否已被撤销。
     * 累计罚下是延迟执行的，若在延迟窗口内裁判撤销了这张牌，
     * 延迟任务必须据此放弃执行，否则会出现"撤销了还是被罚下"。
     */
    private boolean cancelled;

    public CardRecord(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    public UUID uuid() {
        return uuid;
    }

    public String playerName() {
        return playerName;
    }

    public boolean triggeredRed() {
        return triggeredRed;
    }

    public void markTriggeredRed(Location previousLocation, GameMode previousGameMode, String removedTag) {
        this.triggeredRed = true;
        this.previousLocation = previousLocation;
        this.previousGameMode = previousGameMode;
        this.removedTag = removedTag;
    }

    public Location previousLocation() {
        return previousLocation;
    }

    public GameMode previousGameMode() {
        return previousGameMode;
    }

    public String removedTag() {
        return removedTag;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
