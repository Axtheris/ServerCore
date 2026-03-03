package net.axther.serverCore.timeline.action;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Sets the world time at the origin's world.
 * Config: type: time, value: 18000
 */
public class SetTimeAction implements TimelineAction {

    private final long time;

    public SetTimeAction(long time) {
        this.time = time;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        if (origin.getWorld() == null) return;
        origin.getWorld().setTime(time);
    }

    public static SetTimeAction fromConfig(Map<?, ?> map) {
        long value = map.containsKey("value") ? ((Number) map.get("value")).longValue() : 0L;
        return new SetTimeAction(value);
    }
}
