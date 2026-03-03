package net.axther.serverCore.reactive.condition;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Checks whether the minimum number of other players are within a given radius.
 *
 * Config format:
 * <pre>
 * type: nearby-players
 * min: 3
 * radius: 10
 * </pre>
 */
public class NearbyPlayersCondition implements ReactiveCondition {

    private final int minPlayers;
    private final double radius;

    public NearbyPlayersCondition(int minPlayers, double radius) {
        this.minPlayers = minPlayers;
        this.radius = radius;
    }

    @Override
    public boolean test(Player player, Location location) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && !entity.equals(player)) {
                count++;
                if (count >= minPlayers) {
                    return true;
                }
            }
        }
        return false;
    }

    public static NearbyPlayersCondition parse(ConfigurationSection section) {
        int min = section.getInt("min", 3);
        double radius = section.getDouble("radius", 10.0);
        return new NearbyPlayersCondition(min, radius);
    }
}
