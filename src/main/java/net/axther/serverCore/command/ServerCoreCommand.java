package net.axther.serverCore.command;

import net.axther.serverCore.config.ServerCoreConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;

public class ServerCoreCommand implements TabExecutor {

    private final ServerCoreConfig config;

    private static final String[] SYSTEM_NAMES = {
            "cosmetics", "emitters", "pets", "holograms",
            "npcs", "timelines", "reactive", "gui"
    };

    public ServerCoreCommand(ServerCoreConfig config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /servercore reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("servercore.admin.reload")) {
                sender.sendMessage("No permission.");
                return true;
            }
            config.reload();

            sender.sendMessage("ServerCore config reloaded.");
            for (String system : SYSTEM_NAMES) {
                boolean enabled = config.isSystemEnabled(system);
                sender.sendMessage("  " + system + ": " + (enabled ? "enabled" : "disabled"));
            }
            return true;
        }

        sender.sendMessage("Unknown sub-command. Usage: /servercore reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("reload".startsWith(prefix) && sender.hasPermission("servercore.admin.reload")) {
                return List.of("reload");
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
