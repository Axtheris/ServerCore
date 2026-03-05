package net.axther.serverCore.api.event;

import net.axther.serverCore.hologram.action.HologramAction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class HologramClickEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String hologramId;
    private final List<HologramAction> actions;
    private boolean cancelled;

    public HologramClickEvent(Player player, String hologramId, List<HologramAction> actions) {
        this.player = player;
        this.hologramId = hologramId;
        this.actions = actions;
    }

    public Player getPlayer() { return player; }
    public String getHologramId() { return hologramId; }
    public List<HologramAction> getActions() { return actions; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
