package net.axther.serverCore.timeline;

import org.bukkit.Location;

import java.util.*;

/**
 * Central registry and lifecycle manager for timelines. Stores timeline
 * definitions and tracks active running instances.
 */
public class TimelineManager {

    private final Map<String, Timeline> timelines = new LinkedHashMap<>();
    private final List<TimelineInstance> activeInstances = new ArrayList<>();

    /**
     * Register a timeline definition.
     */
    public void register(Timeline timeline) {
        timelines.put(timeline.getId(), timeline);
    }

    /**
     * Get a timeline definition by ID.
     */
    public Timeline get(String id) {
        return timelines.get(id);
    }

    /**
     * Get all registered timeline IDs.
     */
    public Collection<String> getTimelineIds() {
        return Collections.unmodifiableSet(timelines.keySet());
    }

    /**
     * Get the count of registered timelines.
     */
    public int getRegisteredCount() {
        return timelines.size();
    }

    /**
     * Start playing a timeline at the given origin location.
     *
     * @param id     the timeline ID
     * @param origin where the timeline plays from
     * @return the new TimelineInstance, or null if the timeline ID is unknown
     */
    public TimelineInstance play(String id, Location origin) {
        Timeline timeline = timelines.get(id);
        if (timeline == null) {
            return null;
        }

        TimelineInstance instance = new TimelineInstance(timeline, origin);
        activeInstances.add(instance);
        return instance;
    }

    /**
     * Stop all running instances of a specific timeline.
     *
     * @param id the timeline ID to stop
     */
    public void stop(String id) {
        activeInstances.removeIf(instance -> {
            if (instance.getTimeline().getId().equals(id)) {
                instance.stop();
                return true;
            }
            return false;
        });
    }

    /**
     * Stop all running timeline instances.
     */
    public void stopAll() {
        for (TimelineInstance instance : activeInstances) {
            instance.stop();
        }
        activeInstances.clear();
    }

    /**
     * Advance all active instances by one tick. Removes instances that
     * have completed.
     */
    public void tickAll() {
        activeInstances.removeIf(instance -> !instance.tick());
    }

    /**
     * Get the number of currently active instances.
     */
    public int getActiveCount() {
        return activeInstances.size();
    }

    /**
     * Clear all timeline definitions and stop all instances.
     */
    public void clearAll() {
        stopAll();
        timelines.clear();
    }
}
