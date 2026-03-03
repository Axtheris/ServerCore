package net.axther.serverCore.reactive.condition;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * A condition that tests world state around a player to determine
 * whether reactive effects should be active.
 */
public interface ReactiveCondition {

    /**
     * Tests whether this condition is currently met.
     *
     * @param player   the player to evaluate against
     * @param location the player's current location
     * @return true if the condition is satisfied
     */
    boolean test(Player player, Location location);

    /**
     * Parses a ReactiveCondition from a YAML configuration section.
     * The section must contain a "type" key that maps to a known condition type.
     *
     * @param section the config section to parse
     * @return the parsed condition, or null if the type is unknown
     */
    static ReactiveCondition fromConfig(ConfigurationSection section) {
        String type = section.getString("type", "");
        return switch (type) {
            case "time-of-day" -> TimeOfDayCondition.parse(section);
            case "weather" -> WeatherCondition.parse(section);
            case "biome-category" -> BiomeCategoryCondition.parse(section);
            case "player-health" -> PlayerHealthCondition.parse(section);
            case "nearby-players" -> NearbyPlayersCondition.parse(section);
            default -> null;
        };
    }
}
