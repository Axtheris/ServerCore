package net.axther.serverCore.reactive.effect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns particles around the armor stand each time apply is called.
 * Apply is called every evaluation tick (once per second), so "chance"
 * controls the probability of spawning on each evaluation.
 *
 * Config format:
 * <pre>
 * type: particle
 * particle: END_ROD
 * count: 2
 * chance: 100
 * </pre>
 */
public class AddParticleEffect implements ReactiveEffect {

    private final Particle particle;
    private final int count;
    private final int chance; // percentage 0-100

    public AddParticleEffect(Particle particle, int count, int chance) {
        this.particle = particle;
        this.count = count;
        this.chance = chance;
    }

    @Override
    public void apply(ArmorStand stand, Player owner) {
        if (chance < 100 && ThreadLocalRandom.current().nextInt(100) >= chance) {
            return;
        }

        Location loc = stand.getLocation().add(0, 0.5, 0);
        stand.getWorld().spawnParticle(particle, loc, count, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public void remove(ArmorStand stand, Player owner) {
        // Particles are transient; nothing to undo
    }

    public static AddParticleEffect parse(ConfigurationSection section) {
        String particleName = section.getString("particle", "END_ROD").toUpperCase();
        Particle particle;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException e) {
            particle = Particle.END_ROD;
        }
        int count = section.getInt("count", 2);
        int chance = section.getInt("chance", 100);
        return new AddParticleEffect(particle, count, Math.clamp(chance, 0, 100));
    }
}
