package net.axther.serverCore.api.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a hologram is created. Informational only.
 */
public class HologramCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String hologramId;
    private final Location location;

    public HologramCreateEvent(String hologramId, Location location) {
        this.hologramId = hologramId;
        this.location = location;
    }

    public String getHologramId() {
        return hologramId;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
