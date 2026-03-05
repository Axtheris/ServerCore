package net.axther.serverCore.hologram.command;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramAnimation;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.config.HologramConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HologramCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "remove", "addline", "removeline", "edit", "list", "near", "movehere", "setanimation"
    );

    private final HologramManager manager;
    private final HologramConfig config;

    public HologramCommand(HologramManager manager, HologramConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /hologram <create|remove|addline|removeline|edit|list|near|movehere|setanimation>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!player.hasPermission("servercore.hologram.create")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleCreate(player, args);
            }
            case "remove" -> {
                if (!player.hasPermission("servercore.hologram.remove")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleRemove(player, args);
            }
            case "addline" -> {
                if (!player.hasPermission("servercore.hologram.addline")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleAddLine(player, args);
            }
            case "removeline" -> {
                if (!player.hasPermission("servercore.hologram.removeline")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleRemoveLine(player, args);
            }
            case "edit" -> {
                if (!player.hasPermission("servercore.hologram.edit")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleEdit(player, args);
            }
            case "list" -> {
                if (!player.hasPermission("servercore.hologram.list")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleList(player);
            }
            case "near" -> {
                if (!player.hasPermission("servercore.hologram.near")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleNear(player, args);
            }
            case "movehere" -> {
                if (!player.hasPermission("servercore.hologram.movehere")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleMoveHere(player, args);
            }
            case "setanimation" -> {
                if (!player.hasPermission("servercore.hologram.setanimation")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleSetAnimation(player, args);
            }
            default -> player.sendMessage(Component.text("Unknown subcommand. Use: create, remove, addline, removeline, edit, list, near, movehere, setanimation", NamedTextColor.RED));
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hologram create <id> <text...>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        if (manager.get(id) != null) {
            player.sendMessage(Component.text("A hologram with ID '" + id + "' already exists.", NamedTextColor.RED));
            return;
        }

        String text = joinArgs(args, 2);
        Location loc = player.getLocation().clone().add(0, 2, 0);

        Hologram hologram = new Hologram(id, loc, List.of(text));
        manager.register(hologram);
        hologram.spawn();
        config.saveAll(manager);

        player.sendMessage(Component.text("Created hologram '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.GREEN)));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /hologram remove <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        if (manager.get(id) == null) {
            player.sendMessage(Component.text("No hologram found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        manager.unregister(id);
        config.saveAll(manager);
        player.sendMessage(Component.text("Removed hologram '", NamedTextColor.YELLOW)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.YELLOW)));
    }

    private void handleAddLine(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hologram addline <id> <text...>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        Hologram hologram = manager.get(id);
        if (hologram == null) {
            player.sendMessage(Component.text("No hologram found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        String text = joinArgs(args, 2);
        hologram.getLines().add(text);
        hologram.updateText();
        config.saveAll(manager);

        player.sendMessage(Component.text("Added line to hologram '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("' (line " + hologram.getLines().size() + ")", NamedTextColor.GREEN)));
    }

    private void handleRemoveLine(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hologram removeline <id> <line#>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        Hologram hologram = manager.get(id);
        if (hologram == null) {
            player.sendMessage(Component.text("No hologram found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid line number.", NamedTextColor.RED));
            return;
        }

        if (lineNum < 1 || lineNum > hologram.getLines().size()) {
            player.sendMessage(Component.text("Line number out of range (1-" + hologram.getLines().size() + ").", NamedTextColor.RED));
            return;
        }

        hologram.getLines().remove(lineNum - 1);
        hologram.updateText();
        config.saveAll(manager);

        player.sendMessage(Component.text("Removed line " + lineNum + " from hologram '", NamedTextColor.YELLOW)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.YELLOW)));
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("Usage: /hologram edit <id> <line#> <text...>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        Hologram hologram = manager.get(id);
        if (hologram == null) {
            player.sendMessage(Component.text("No hologram found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid line number.", NamedTextColor.RED));
            return;
        }

        if (lineNum < 1 || lineNum > hologram.getLines().size()) {
            player.sendMessage(Component.text("Line number out of range (1-" + hologram.getLines().size() + ").", NamedTextColor.RED));
            return;
        }

        String text = joinArgs(args, 3);
        hologram.getLines().set(lineNum - 1, text);
        hologram.updateText();
        config.saveAll(manager);

        player.sendMessage(Component.text("Updated line " + lineNum + " of hologram '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.GREEN)));
    }

    private void handleList(Player player) {
        var all = manager.getAll();
        if (all.isEmpty()) {
            player.sendMessage(Component.text("No holograms registered.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("--- Holograms (" + all.size() + ") ---", NamedTextColor.GREEN));
        for (Hologram h : all) {
            Location loc = h.getLocation();
            player.sendMessage(Component.text(" " + h.getId(), NamedTextColor.WHITE)
                    .append(Component.text(String.format(" @ %s %.1f, %.1f, %.1f", h.getWorldName(), loc.getX(), loc.getY(), loc.getZ()), NamedTextColor.GRAY))
                    .append(Component.text(" | " + h.getAnimation().name() + " | " + h.getLines().size() + " lines", NamedTextColor.AQUA))
                    .append(Component.text(h.isSpawned() ? " [spawned]" : " [unloaded]", h.isSpawned() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }
    }

    private void handleNear(Player player, String[] args) {
        double radius = 10.0;
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid radius.", NamedTextColor.RED));
                return;
            }
        }

        List<Hologram> nearby = manager.getNearby(player.getLocation(), radius);
        if (nearby.isEmpty()) {
            player.sendMessage(Component.text("No holograms within " + radius + " blocks.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("--- Nearby Holograms (" + nearby.size() + ") ---", NamedTextColor.GREEN));
        for (Hologram h : nearby) {
            double dist = player.getLocation().distance(h.getLocation());
            player.sendMessage(Component.text(" " + h.getId(), NamedTextColor.WHITE)
                    .append(Component.text(String.format(" (%.1f blocks away)", dist), NamedTextColor.GRAY)));
        }
    }

    private void handleMoveHere(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /hologram movehere <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        Hologram hologram = manager.get(id);
        if (hologram == null) {
            player.sendMessage(Component.text("No hologram found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        Location newLoc = player.getLocation().clone().add(0, 2, 0);
        hologram.setLocation(newLoc);
        config.saveAll(manager);

        player.sendMessage(Component.text("Moved hologram '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("' to your location", NamedTextColor.GREEN)));
    }

    private void handleSetAnimation(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hologram setanimation <id> <animation>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        Hologram hologram = manager.get(id);
        if (hologram == null) {
            player.sendMessage(Component.text("No hologram found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        HologramAnimation animation;
        try {
            animation = HologramAnimation.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown animation: " + args[2] + ". Options: " +
                    String.join(", ", Arrays.stream(HologramAnimation.values()).map(Enum::name).toList()), NamedTextColor.RED));
            return;
        }

        hologram.setAnimation(animation);
        config.saveAll(manager);

        player.sendMessage(Component.text("Set animation of '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("' to ", NamedTextColor.GREEN))
                .append(Component.text(animation.name(), NamedTextColor.AQUA)));
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .filter(s -> !(sender instanceof Player p) || p.hasPermission("servercore.hologram." + s))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "remove", "addline", "removeline", "edit", "movehere", "setanimation" -> {
                    return filter(hologramIds(), args[1]);
                }
                case "create" -> {
                    return List.of("<id>");
                }
                case "near" -> {
                    return List.of("10", "25", "50");
                }
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "removeline", "edit" -> {
                    Hologram h = manager.get(args[1]);
                    if (h != null) {
                        return h.getLines().stream()
                                .map(l -> String.valueOf(h.getLines().indexOf(l) + 1))
                                .collect(Collectors.toList());
                    }
                }
                case "setanimation" -> {
                    return filter(animationNames(), args[2]);
                }
                case "create" -> {
                    return List.of("<text...>");
                }
            }
        }

        return List.of();
    }

    private List<String> hologramIds() {
        return manager.getAll().stream().map(Hologram::getId).toList();
    }

    private List<String> animationNames() {
        return Arrays.stream(HologramAnimation.values()).map(a -> a.name().toLowerCase()).toList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
