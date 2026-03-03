package net.axther.serverCore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QuestCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String questId;

    public QuestCompleteEvent(Player player, String questId) {
        this.player = player;
        this.questId = questId;
    }

    public Player getPlayer() { return player; }
    public String getQuestId() { return questId; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
