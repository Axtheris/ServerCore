package net.axther.serverCore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QuestProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String questId;
    private final int objectiveIndex;
    private final int newProgress;

    public QuestProgressEvent(Player player, String questId, int objectiveIndex, int newProgress) {
        this.player = player;
        this.questId = questId;
        this.objectiveIndex = objectiveIndex;
        this.newProgress = newProgress;
    }

    public Player getPlayer() { return player; }
    public String getQuestId() { return questId; }
    public int getObjectiveIndex() { return objectiveIndex; }
    public int getNewProgress() { return newProgress; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
