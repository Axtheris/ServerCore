package net.axther.serverCore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ReactiveRuleTriggeredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String ruleId;

    public ReactiveRuleTriggeredEvent(Player player, String ruleId) {
        this.player = player;
        this.ruleId = ruleId;
    }

    public Player getPlayer() { return player; }
    public String getRuleId() { return ruleId; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
