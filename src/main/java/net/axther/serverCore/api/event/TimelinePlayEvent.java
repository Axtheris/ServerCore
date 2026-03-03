package net.axther.serverCore.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a timeline sequence begins playing. Cancelling prevents playback.
 */
public class TimelinePlayEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String timelineId;
    private final Location origin;
    private boolean cancelled;

    public TimelinePlayEvent(String timelineId, Location origin) {
        this.timelineId = timelineId;
        this.origin = origin;
    }

    public String getTimelineId() {
        return timelineId;
    }

    public Location getOrigin() {
        return origin;
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
