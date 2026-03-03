package net.axther.serverCore.npc.dialogue.action;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlaySoundAction implements DialogueAction {

    private final String soundName;

    public PlaySoundAction(String soundName) {
        this.soundName = soundName;
    }

    @Override
    public void execute(Player player) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            // Unknown sound, silently ignore
        }
    }
}
