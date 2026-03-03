package net.axther.serverCore.pet;

import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;

import java.util.concurrent.ThreadLocalRandom;

public class PetSound {

    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final int chance; // 1 in N ticks probability

    public PetSound(Sound sound, float volume, float pitch, int chance) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.chance = Math.max(chance, 1);
    }

    /** Attempts to play this sound at the stand's location. Rolls 1-in-chance per call. */
    public void tryPlay(ArmorStand stand, long tickCounter) {
        if (stand == null) return;
        if (ThreadLocalRandom.current().nextInt(chance) == 0) {
            stand.getWorld().playSound(stand.getLocation(), sound, volume, pitch);
        }
    }

    public Sound getSound() { return sound; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public int getChance() { return chance; }
}
