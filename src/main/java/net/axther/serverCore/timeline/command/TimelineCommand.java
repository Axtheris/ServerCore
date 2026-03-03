package net.axther.serverCore.timeline.command;

import net.axther.serverCore.timeline.Timeline;
import net.axther.serverCore.timeline.TimelineInstance;
import net.axther.serverCore.timeline.TimelineManager;
import net.axther.serverCore.timeline.config.TimelineConfig;
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

/**
 * Command handler for /timeline.
 * Subcommands: play, stop, list, reload
 */
public class TimelineCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("play", "stop", "list", "reload");

    private final TimelineManager manager;
    private final TimelineConfig config;

    public TimelineCommand(TimelineManager manager, TimelineConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text(
                    "Usage: /timeline <play|stop|list|reload>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "play" -> handlePlay(sender, args);
            case "stop" -> handleStop(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(Component.text(
                    "Unknown subcommand. Use: play, stop, list, reload", NamedTextColor.RED));
        }

        return true;
    }

    private void handlePlay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "This command can only be used by players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /timeline play <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        Timeline timeline = manager.get(id);
        if (timeline == null) {
            player.sendMessage(Component.text(
                    "Unknown timeline: " + id, NamedTextColor.RED));
            return;
        }

        TimelineInstance instance = manager.play(id, player.getLocation());
        if (instance != null) {
            player.sendMessage(Component.text("Playing timeline '", NamedTextColor.GREEN)
                    .append(Component.text(id, NamedTextColor.WHITE))
                    .append(Component.text("' at your location (" + timeline.getDuration() +
                            " ticks, " + (timeline.isLoop() ? "looping" : "once") + ")",
                            NamedTextColor.GREEN)));
        }
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Stop all
            int count = manager.getActiveCount();
            manager.stopAll();
            sender.sendMessage(Component.text(
                    "Stopped all timelines (" + count + " instance(s))", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        if (manager.get(id) == null) {
            sender.sendMessage(Component.text(
                    "Unknown timeline: " + id, NamedTextColor.RED));
            return;
        }

        manager.stop(id);
        sender.sendMessage(Component.text("Stopped all instances of '", NamedTextColor.YELLOW)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.YELLOW)));
    }

    private void handleList(CommandSender sender) {
        var ids = manager.getTimelineIds();
        if (ids.isEmpty()) {
            sender.sendMessage(Component.text(
                    "No timelines registered.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text(
                "--- Timelines (" + ids.size() + ") ---", NamedTextColor.GREEN));

        for (String id : ids) {
            Timeline t = manager.get(id);
            if (t == null) continue;

            sender.sendMessage(Component.text(" " + id, NamedTextColor.WHITE)
                    .append(Component.text(" | " + t.getKeyframes().size() + " keyframe(s)",
                            NamedTextColor.GRAY))
                    .append(Component.text(" | " + t.getDuration() + " ticks",
                            NamedTextColor.GRAY))
                    .append(Component.text(" | audience: " + t.getAudience(),
                            NamedTextColor.GRAY))
                    .append(Component.text(t.isLoop() ? " [loop]" : " [once]",
                            t.isLoop() ? NamedTextColor.AQUA : NamedTextColor.GRAY)));
        }

        int active = manager.getActiveCount();
        if (active > 0) {
            sender.sendMessage(Component.text(
                    active + " instance(s) currently playing", NamedTextColor.AQUA));
        }
    }

    private void handleReload(CommandSender sender) {
        manager.clearAll();
        config.loadAll(manager);
        sender.sendMessage(Component.text(
                "Reloaded " + manager.getRegisteredCount() + " timeline(s)", NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("play".equals(sub) || "stop".equals(sub)) {
                return filter(List.copyOf(manager.getTimelineIds()), args[1]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
