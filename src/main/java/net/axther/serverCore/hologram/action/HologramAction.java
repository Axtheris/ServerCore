package net.axther.serverCore.hologram.action;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class HologramAction {

    private final String type;
    private final String value;

    private HologramAction(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public static HologramAction parse(String type, String value) {
        return new HologramAction(type, value);
    }

    public void execute(Player player) {
        switch (type) {
            case "command" -> {
                String cmd = value.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            case "player_command" -> {
                player.performCommand(value.replace("%player%", player.getName()));
            }
            case "message" -> {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        value.replace("%player%", player.getName())));
            }
            case "sound" -> {
                try {
                    Sound sound = Sound.valueOf(value.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public String getType() { return type; }
    public String getValue() { return value; }
}
