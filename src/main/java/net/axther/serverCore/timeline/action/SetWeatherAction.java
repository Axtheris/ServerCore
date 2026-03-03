package net.axther.serverCore.timeline.action;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Sets the world weather at the origin's world.
 * Config: type: weather, value: thunder, duration: 200
 */
public class SetWeatherAction implements TimelineAction {

    private final String weather;
    private final int duration;

    public SetWeatherAction(String weather, int duration) {
        this.weather = weather;
        this.duration = duration;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        World world = origin.getWorld();
        if (world == null) return;

        int durationTicks = duration * 20;

        switch (weather.toLowerCase()) {
            case "clear" -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(durationTicks);
            }
            case "rain", "storm" -> {
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(durationTicks);
            }
            case "thunder" -> {
                world.setStorm(true);
                world.setThundering(true);
                world.setThunderDuration(durationTicks);
                world.setWeatherDuration(durationTicks);
            }
        }
    }

    public static SetWeatherAction fromConfig(Map<?, ?> map) {
        String value = map.containsKey("value") ? String.valueOf(map.get("value")) : "clear";
        int duration = map.containsKey("duration") ? ((Number) map.get("duration")).intValue() : 200;
        return new SetWeatherAction(value, duration);
    }
}
