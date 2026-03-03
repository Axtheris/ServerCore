package net.axther.serverCore.timeline;

import net.axther.serverCore.timeline.action.TimelineAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A keyframe represents a collection of actions that fire at a specific tick
 * within a timeline.
 */
public class Keyframe {

    private final int tick;
    private final List<TimelineAction> actions;

    public Keyframe(int tick, List<TimelineAction> actions) {
        this.tick = tick;
        this.actions = Collections.unmodifiableList(actions);
    }

    public int getTick() {
        return tick;
    }

    public List<TimelineAction> getActions() {
        return actions;
    }

    /**
     * Parse a keyframe from the YAML list of action maps.
     *
     * @param tick          the tick number this keyframe fires at
     * @param actionConfigs the list of action configuration maps from YAML
     * @return the parsed Keyframe
     */
    public static Keyframe fromConfig(int tick, List<Map<?, ?>> actionConfigs) {
        List<TimelineAction> actions = new ArrayList<>();
        for (Map<?, ?> map : actionConfigs) {
            TimelineAction action = TimelineAction.fromConfig(map);
            if (action != null) {
                actions.add(action);
            }
        }
        return new Keyframe(tick, actions);
    }
}
