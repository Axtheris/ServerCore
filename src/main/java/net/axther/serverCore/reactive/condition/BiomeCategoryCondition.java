package net.axther.serverCore.reactive.condition;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Checks whether the player is in a biome whose key name contains
 * the configured value. This allows matching biome categories such as
 * "nether", "ocean", "desert", "forest", etc.
 *
 * Config format:
 * <pre>
 * type: biome-category
 * value: nether
 * </pre>
 */
public class BiomeCategoryCondition implements ReactiveCondition {

    private final String biomeMatch;

    public BiomeCategoryCondition(String biomeMatch) {
        this.biomeMatch = biomeMatch.toLowerCase();
    }

    @Override
    public boolean test(Player player, Location location) {
        Biome biome = location.getWorld().getBiome(location);
        String biomeName = biome.getKey().getKey().toLowerCase();
        return biomeName.contains(biomeMatch);
    }

    public static BiomeCategoryCondition parse(ConfigurationSection section) {
        String value = section.getString("value", "plains");
        return new BiomeCategoryCondition(value);
    }
}
