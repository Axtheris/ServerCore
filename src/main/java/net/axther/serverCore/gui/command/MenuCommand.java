package net.axther.serverCore.gui.command;

import net.axther.serverCore.gui.Menu;
import net.axther.serverCore.gui.MenuConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class MenuCommand implements TabExecutor {

    private final MenuConfig config;

    public MenuCommand(MenuConfig config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /menu <id>", NamedTextColor.YELLOW));
            return true;
        }

        String menuId = args[0];

        if (!player.hasPermission("servercore.menu." + menuId)) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        Menu menu = config.buildMenu(menuId);
        if (menu == null) {
            player.sendMessage(Component.text("Unknown menu: " + menuId, NamedTextColor.RED));
            return true;
        }

        menu.open(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            return config.getLayoutIds().stream()
                    .filter(id -> id.toLowerCase().startsWith(lower))
                    .filter(id -> !(sender instanceof Player p) || p.hasPermission("servercore.menu." + id))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
