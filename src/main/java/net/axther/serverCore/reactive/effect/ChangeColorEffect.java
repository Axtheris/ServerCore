package net.axther.serverCore.reactive.effect;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Spawns colored dust particles around the armor stand.
 * This is intended for use alongside other emitters to add color variation,
 * but works standalone as a colored particle burst.
 *
 * Config format:
 * <pre>
 * type: change-color
 * color: [255, 0, 0]
 * </pre>
 */
public class ChangeColorEffect implements ReactiveEffect {

    private final Color color;

    public ChangeColorEffect(Color color) {
        this.color = color;
    }

    @Override
    public void apply(ArmorStand stand, Player owner) {
        Location loc = stand.getLocation().add(0, 0.5, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
        stand.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.3, 0.3, 0.3, 0, dustOptions);
    }

    @Override
    public void remove(ArmorStand stand, Player owner) {
        // Dust particles are transient; nothing to undo
    }

    public static ChangeColorEffect parse(ConfigurationSection section) {
        List<Integer> rgb = section.getIntegerList("color");
        Color color;
        if (rgb.size() >= 3) {
            color = Color.fromRGB(
                    Math.clamp(rgb.get(0), 0, 255),
                    Math.clamp(rgb.get(1), 0, 255),
                    Math.clamp(rgb.get(2), 0, 255)
            );
        } else {
            color = Color.fromRGB(255, 0, 0);
        }
        return new ChangeColorEffect(color);
    }
}
