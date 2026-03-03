package net.axther.serverCore.reactive.condition;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Checks the current weather state of the player's world.
 * Supported values: "rain", "thunder", "clear".
 *
 * Config format:
 * <pre>
 * type: weather
 * value: rain
 * </pre>
 */
public class WeatherCondition implements ReactiveCondition {

    private final String weatherValue;

    public WeatherCondition(String weatherValue) {
        this.weatherValue = weatherValue.toLowerCase();
    }

    @Override
    public boolean test(Player player, Location location) {
        World world = location.getWorld();
        return switch (weatherValue) {
            case "thunder" -> world.isThundering();
            case "rain" -> world.hasStorm() && !world.isThundering();
            case "clear" -> !world.hasStorm();
            default -> false;
        };
    }

    public static WeatherCondition parse(ConfigurationSection section) {
        String value = section.getString("value", "clear");
        return new WeatherCondition(value);
    }
}
