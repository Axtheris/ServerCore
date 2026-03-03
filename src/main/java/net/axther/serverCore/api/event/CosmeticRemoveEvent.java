package net.axther.serverCore.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired before a cosmetic is removed from a mob. Informational only.
 */
public class CosmeticRemoveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID mobUuid;

    public CosmeticRemoveEvent(UUID mobUuid) {
        this.mobUuid = mobUuid;
    }

    public UUID getMobUuid() {
        return mobUuid;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
