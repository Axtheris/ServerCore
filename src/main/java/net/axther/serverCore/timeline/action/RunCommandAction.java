package net.axther.serverCore.timeline.action;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Executes a command as the console. Supports %x%, %y%, %z% placeholders
 * that are replaced with the origin coordinates.
 * Config: type: command, value: "summon minecraft:warden %x% %y% %z%"
 */
public class RunCommandAction implements TimelineAction {

    private final String command;

    public RunCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        String resolved = command
                .replace("%x%", String.valueOf(origin.getBlockX()))
                .replace("%y%", String.valueOf(origin.getBlockY()))
                .replace("%z%", String.valueOf(origin.getBlockZ()));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
    }

    public static RunCommandAction fromConfig(Map<?, ?> map) {
        String value = map.containsKey("value") ? String.valueOf(map.get("value")) : "";
        return new RunCommandAction(value);
    }
}
