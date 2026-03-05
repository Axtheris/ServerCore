package net.axther.serverCore.npc.command;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.config.NPCConfig;
import net.axther.serverCore.npc.listener.NPCListener;
import net.axther.serverCore.npc.render.NPCViewTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class NPCCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "remove", "movehere", "list", "reload"
    );

    private final NPCManager manager;
    private final NPCConfig config;
    private final NPCListener listener;

    public NPCCommand(NPCManager manager, NPCConfig config, NPCListener listener) {
        this.manager = manager;
        this.config = config;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equals("_dialogue")) {
            return handleDialogueInternal(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /npc <create|remove|movehere|list|reload>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!player.hasPermission("servercore.npc.create")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleCreate(player, args);
            }
            case "remove" -> {
                if (!player.hasPermission("servercore.npc.remove")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleRemove(player, args);
            }
            case "movehere" -> {
                if (!player.hasPermission("servercore.npc.movehere")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleMoveHere(player, args);
            }
            case "list" -> {
                if (!player.hasPermission("servercore.npc.list")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleList(player);
            }
            case "reload" -> {
                if (!player.hasPermission("servercore.npc.reload")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleReload(player);
            }
            default -> player.sendMessage(Component.text("Unknown subcommand. Use: create, remove, movehere, list, reload", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleDialogueInternal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 2) return true;

        try {
            int choiceIndex = Integer.parseInt(args[1]);
            listener.handleDialogueChoice(player, choiceIndex);
        } catch (NumberFormatException ignored) {}
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /npc create <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase();
        if (manager.get(id) != null) {
            player.sendMessage(Component.text("An NPC with ID '" + id + "' already exists.", NamedTextColor.RED));
            return;
        }

        Location loc = player.getLocation();
        String displayName = "<white>" + id;

        NPC npc = new NPC(id, displayName, loc, loc.getYaw(), null, null, true, null);
        manager.register(npc);

        NPCViewTracker viewTracker = manager.getViewTracker();
        if (viewTracker != null) {
            viewTracker.spawnForAllViewers(npc);
        }

        config.save(npc);

        player.sendMessage(Component.text("Created NPC '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("' at your location. Edit ", NamedTextColor.GREEN))
                .append(Component.text("plugins/ServerCore/npcs/" + id + ".yml", NamedTextColor.AQUA))
                .append(Component.text(" to configure.", NamedTextColor.GREEN)));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /npc remove <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase();
        if (manager.get(id) == null) {
            player.sendMessage(Component.text("No NPC found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        manager.unregister(id);
        config.deleteFile(id);

        player.sendMessage(Component.text("Removed NPC '", NamedTextColor.YELLOW)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.YELLOW)));
    }

    private void handleMoveHere(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /npc movehere <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase();
        NPC npc = manager.get(id);
        if (npc == null) {
            player.sendMessage(Component.text("No NPC found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        npc.setLocation(player.getLocation());
        config.save(npc);

        NPCViewTracker viewTracker = manager.getViewTracker();
        if (viewTracker != null) {
            viewTracker.teleportForAllViewers(npc);
        }

        player.sendMessage(Component.text("Moved NPC '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("' to your location", NamedTextColor.GREEN)));
    }

    private void handleList(Player player) {
        var all = manager.getAll();
        if (all.isEmpty()) {
            player.sendMessage(Component.text("No NPCs registered.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("--- NPCs (" + all.size() + ") ---", NamedTextColor.GREEN));
        MiniMessage mm = MiniMessage.miniMessage();

        NPCViewTracker viewTracker = manager.getViewTracker();

        for (NPC npc : all) {
            Location loc = npc.getLocation();
            int viewers = viewTracker != null ? viewTracker.getViewerCount(npc.getId()) : 0;
            player.sendMessage(Component.text(" " + npc.getId(), NamedTextColor.WHITE)
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(mm.deserialize(npc.getDisplayName()))
                    .append(Component.text(String.format(" @ %s %.1f, %.1f, %.1f",
                            npc.getWorldName(), loc.getX(), loc.getY(), loc.getZ()), NamedTextColor.GRAY))
                    .append(Component.text(" [" + viewers + " viewers]", NamedTextColor.AQUA)));
        }
    }

    private void handleReload(Player player) {
        listener.clearAllSessions();
        manager.destroyAll();
        config.loadAll(manager, net.axther.serverCore.api.ServerCoreAPI.get().getQuestManager());

        player.sendMessage(Component.text("Reloaded " + manager.getAll().size() + " NPCs.", NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .filter(s -> !(sender instanceof Player p) || p.hasPermission("servercore.npc." + s))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "remove", "movehere" -> {
                    return filter(npcIds(), args[1]);
                }
                case "create" -> {
                    return List.of("<id>");
                }
            }
        }

        return List.of();
    }

    private List<String> npcIds() {
        return manager.getAll().stream().map(NPC::getId).toList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
