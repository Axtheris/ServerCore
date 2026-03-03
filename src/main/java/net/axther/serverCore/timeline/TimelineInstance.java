package net.axther.serverCore.timeline;

import net.axther.serverCore.timeline.action.TimelineAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A running instance of a timeline, tracking the current tick and executing
 * keyframe actions as the timeline advances.
 */
public class TimelineInstance {

    private final Timeline timeline;
    private final Location origin;
    private int currentTick;
    private boolean stopped;

    public TimelineInstance(Timeline timeline, Location origin) {
        this.timeline = timeline;
        this.origin = origin.clone();
        this.currentTick = 0;
        this.stopped = false;
    }

    /**
     * Advance one tick. Executes any keyframe actions at the current tick,
     * then increments. Returns false when the timeline is finished.
     *
     * @return true if the instance is still running, false if it should be removed
     */
    public boolean tick() {
        if (stopped) {
            return false;
        }

        // Execute any keyframe at the current tick
        Keyframe keyframe = timeline.getKeyframes().get(currentTick);
        if (keyframe != null) {
            Collection<Player> audience = getAudience();
            for (TimelineAction action : keyframe.getActions()) {
                action.execute(origin, audience);
            }
        }

        currentTick++;

        // Check if we've reached the end
        if (currentTick >= timeline.getDuration()) {
            if (timeline.isLoop()) {
                currentTick = 0;
                return true;
            }
            return false;
        }

        return true;
    }

    /**
     * Get the audience for this timeline instance based on the timeline's
     * audience setting.
     *
     * @return collection of players that should receive timeline actions
     */
    public Collection<Player> getAudience() {
        if ("all".equalsIgnoreCase(timeline.getAudience())) {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }

        // Default: "nearby" -- players within radius of the origin
        if (origin.getWorld() == null) {
            return List.of();
        }

        double radiusSq = timeline.getRadius() * timeline.getRadius();
        List<Player> nearby = new ArrayList<>();
        for (Player player : origin.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(origin) <= radiusSq) {
                nearby.add(player);
            }
        }
        return nearby;
    }

    /**
     * Stop this instance immediately.
     */
    public void stop() {
        this.stopped = true;
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public Location getOrigin() {
        return origin;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public boolean isStopped() {
        return stopped;
    }
}
