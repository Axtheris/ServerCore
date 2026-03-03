package net.axther.serverCore.npc;

import org.bukkit.configuration.ConfigurationSection;

public record NPCSkin(String texture, String signature) {

    public static NPCSkin fromConfig(ConfigurationSection section) {
        String texture = section.getString("skin-texture");
        String signature = section.getString("skin-signature");
        if (texture == null || texture.isBlank()) return null;
        return new NPCSkin(texture, signature);
    }
}
