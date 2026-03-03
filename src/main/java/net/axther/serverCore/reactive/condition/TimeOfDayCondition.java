package net.axther.serverCore.reactive.condition;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Checks whether the world time falls within a specified tick range.
 * World time is 0-24000 where 0 is sunrise, 6000 is noon, 13000 is sunset, 18000 is midnight.
 * Supports wrap-around ranges (e.g. [22000, 2000] means late night into early morning).
 *
 * Config format:
 * <pre>
 * type: time-of-day
 * range: [13000, 23000]
 * </pre>
 */
public class TimeOfDayCondition implements ReactiveCondition {

    private final long rangeStart;
    private final long rangeEnd;

    public TimeOfDayCondition(long rangeStart, long rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public boolean test(Player player, Location location) {
        long time = location.getWorld().getTime();
        if (rangeStart <= rangeEnd) {
            return time >= rangeStart && time <= rangeEnd;
        }
        // Wrap-around: e.g. [22000, 2000] means 22000-24000 OR 0-2000
        return time >= rangeStart || time <= rangeEnd;
    }

    public static TimeOfDayCondition parse(ConfigurationSection section) {
        List<Integer> range = section.getIntegerList("range");
        if (range.size() < 2) {
            return new TimeOfDayCondition(0, 24000);
        }
        return new TimeOfDayCondition(range.get(0), range.get(1));
    }
}
