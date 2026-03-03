package net.axther.serverCore.reactive.effect;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

/**
 * A visual effect that can be applied to and removed from an armor stand
 * (used by both the cosmetic and pet systems).
 */
public interface ReactiveEffect {

    /**
     * Applies this effect to the given armor stand.
     *
     * @param stand the armor stand to modify
     * @param owner the player who owns the pet or is near the cosmetic
     */
    void apply(ArmorStand stand, Player owner);

    /**
     * Removes this effect from the given armor stand, restoring its previous state.
     *
     * @param stand the armor stand to restore
     * @param owner the player who owns the pet or is near the cosmetic
     */
    void remove(ArmorStand stand, Player owner);

    /**
     * Parses a ReactiveEffect from a YAML configuration section.
     *
     * @param section the config section to parse
     * @return the parsed effect, or null if the type is unknown
     */
    static ReactiveEffect fromConfig(ConfigurationSection section) {
        String type = section.getString("type", "");
        return switch (type) {
            case "glow" -> GlowToggleEffect.parse(section);
            case "particle" -> AddParticleEffect.parse(section);
            case "swap-item" -> SwapItemEffect.parse(section);
            case "change-color" -> ChangeColorEffect.parse(section);
            default -> null;
        };
    }
}
