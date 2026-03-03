package net.axther.serverCore.timeline.action;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Plays a sound at the timeline origin for the audience.
 * Config: type: sound, sound: ENTITY_WITHER_SPAWN, volume: 1.0, pitch: 0.5
 */
public class PlaySoundAction implements TimelineAction {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public PlaySoundAction(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        for (Player player : audience) {
            player.playSound(origin, sound, volume, pitch);
        }
    }

    public static PlaySoundAction fromConfig(Map<?, ?> map) {
        String soundName = String.valueOf(map.get("sound")).toLowerCase().replace("_", ".");
        // Also try the raw key format (e.g., entity.wither.spawn)
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName));
        if (sound == null) {
            // Try converting ENUM_STYLE to key.style (ENTITY_WITHER_SPAWN -> entity.wither.spawn)
            String enumStyle = String.valueOf(map.get("sound")).toUpperCase();
            String keyStyle = enumStyle.toLowerCase().replace("_", ".");
            sound = Registry.SOUNDS.get(NamespacedKey.minecraft(keyStyle));
        }
        if (sound == null) {
            sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        float volume = map.containsKey("volume") ? ((Number) map.get("volume")).floatValue() : 1.0f;
        float pitch = map.containsKey("pitch") ? ((Number) map.get("pitch")).floatValue() : 1.0f;
        return new PlaySoundAction(sound, volume, pitch);
    }
}
