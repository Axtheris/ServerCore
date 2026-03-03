package net.axther.serverCore.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a particle emitter is created. Cancelling prevents the creation.
 */
public class EmitterCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String emitterId;
    private final Location location;
    private boolean cancelled;

    public EmitterCreateEvent(String emitterId, Location location) {
        this.emitterId = emitterId;
        this.location = location;
    }

    public String getEmitterId() {
        return emitterId;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
