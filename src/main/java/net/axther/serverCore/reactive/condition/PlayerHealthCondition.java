package net.axther.serverCore.reactive.condition;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Checks whether the player's health is below a threshold.
 * Health is measured in half-hearts (max 20.0 by default, so 6.0 = 3 hearts).
 *
 * Config format:
 * <pre>
 * type: player-health
 * below: 6.0
 * </pre>
 */
public class PlayerHealthCondition implements ReactiveCondition {

    private final double belowThreshold;

    public PlayerHealthCondition(double belowThreshold) {
        this.belowThreshold = belowThreshold;
    }

    @Override
    public boolean test(Player player, Location location) {
        return player.getHealth() < belowThreshold;
    }

    public static PlayerHealthCondition parse(ConfigurationSection section) {
        double below = section.getDouble("below", 6.0);
        return new PlayerHealthCondition(below);
    }
}
