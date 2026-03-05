package net.axther.serverCore.quest.command;

import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestObjective;
import net.axther.serverCore.quest.QuestProgress;
import net.axther.serverCore.quest.config.QuestConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("active", "completed", "abandon", "reload");

    private final QuestManager manager;
    private final QuestConfig config;

    public QuestCommand(QuestManager manager, QuestConfig config) {
        this.manager = manager;
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
            player.sendMessage(Component.text("Usage: /quest <active|completed|abandon|reload>",
                    NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "active" -> {
                if (!player.hasPermission("servercore.quest.active")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleActive(player);
            }
            case "completed" -> {
                if (!player.hasPermission("servercore.quest.completed")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleCompleted(player);
            }
            case "abandon" -> {
                if (!player.hasPermission("servercore.quest.abandon")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleAbandon(player, args);
            }
            case "reload" -> {
                if (!player.hasPermission("servercore.quest.reload")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleReload(player);
            }
            default -> player.sendMessage(Component.text(
                    "Unknown subcommand. Use: active, completed, abandon, reload", NamedTextColor.RED));
        }
        return true;
    }

    private void handleActive(Player player) {
        List<QuestProgress> active = manager.getActiveQuests(player.getUniqueId());
        if (active.isEmpty()) {
            player.sendMessage(Component.text("No active quests.", NamedTextColor.GRAY));
            return;
        }

        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(Component.text("--- Active Quests (" + active.size() + ") ---", NamedTextColor.GREEN));

        // Group by category
        Map<String, List<QuestProgress>> byCategory = new LinkedHashMap<>();
        for (QuestProgress progress : active) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;
            byCategory.computeIfAbsent(quest.getCategory(), k -> new ArrayList<>()).add(progress);
        }

        for (Map.Entry<String, List<QuestProgress>> entry : byCategory.entrySet()) {
            player.sendMessage(Component.text(" [" + entry.getKey() + "]", NamedTextColor.GOLD));

            for (QuestProgress progress : entry.getValue()) {
                Quest quest = manager.getQuest(progress.getQuestId());
                if (quest == null) continue;

                String timeInfo = "";
                if (quest.getTimeLimit() > 0) {
                    long elapsed = (System.currentTimeMillis() - progress.getStartedAt()) / 1000;
                    long remaining = quest.getTimeLimit() - elapsed;
                    if (remaining > 0) {
                        timeInfo = " <gray>(" + formatTime(remaining) + " remaining)";
                    } else {
                        timeInfo = " <red>(EXPIRED)";
                    }
                }

                player.sendMessage(Component.text("  ").append(mm.deserialize(quest.getDisplayName() + timeInfo)));

                List<QuestObjective> objectives = quest.getObjectives();
                for (int i = 0; i < objectives.size(); i++) {
                    QuestObjective obj = objectives.get(i);
                    int current = progress.getProgress(i);

                    if (obj.getType() == QuestObjective.Type.FETCH) {
                        current = manager.countMaterial(player, obj.getTarget());
                    }

                    String desc = obj.getDescription() != null ? obj.getDescription()
                            : generateDescription(obj);

                    boolean done = current >= obj.getAmount();
                    boolean locked = quest.isSequentialObjectives() && i > getFirstIncomplete(progress, objectives);
                    String prefix = done ? "+" : locked ? "x" : "-";
                    NamedTextColor color = done ? NamedTextColor.GREEN : locked ? NamedTextColor.DARK_GRAY : NamedTextColor.GRAY;

                    player.sendMessage(Component.text("   " + prefix + " " + desc
                                    + " (" + Math.min(current, obj.getAmount()) + "/" + obj.getAmount() + ")",
                            color));
                }
            }
        }
    }

    private String generateDescription(QuestObjective obj) {
        return switch (obj.getType()) {
            case FETCH -> "Collect " + obj.getAmount() + " " + obj.getTarget();
            case KILL -> "Kill " + obj.getAmount() + " " + obj.getTarget();
            case TALK -> "Talk to " + obj.getTarget();
            case CRAFT -> "Craft " + obj.getAmount() + " " + obj.getTarget();
            case MINE -> "Mine " + obj.getAmount() + " " + obj.getTarget();
            case PLACE -> "Place " + obj.getAmount() + " " + obj.getTarget();
            case FISH -> obj.getTarget().equals("ANY") ? "Catch fish" : "Catch " + obj.getTarget();
            case BREED -> "Breed " + obj.getAmount() + " " + obj.getTarget();
            case SMELT -> "Smelt " + obj.getAmount() + " " + obj.getTarget();
            case EXPLORE -> "Explore location";
            case INTERACT -> "Interact with " + obj.getTarget();
        };
    }

    private int getFirstIncomplete(QuestProgress progress, List<QuestObjective> objectives) {
        for (int i = 0; i < objectives.size(); i++) {
            if (progress.getProgress(i) < objectives.get(i).getAmount()) return i;
        }
        return objectives.size();
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private void handleCompleted(Player player) {
        var completed = manager.getCompletedQuests(player.getUniqueId());
        if (completed.isEmpty()) {
            player.sendMessage(Component.text("No completed quests.", NamedTextColor.GRAY));
            return;
        }

        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(Component.text("--- Completed Quests (" + completed.size() + ") ---",
                NamedTextColor.GOLD));

        for (String questId : completed.keySet()) {
            Quest quest = manager.getQuest(questId);
            String name = quest != null ? quest.getDisplayName() : questId;
            player.sendMessage(Component.text(" ").append(mm.deserialize(name)));
        }
    }

    private void handleAbandon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /quest abandon <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase();
        if (!manager.isActive(player.getUniqueId(), id)) {
            player.sendMessage(Component.text("You don't have an active quest with ID '" + id + "'.",
                    NamedTextColor.RED));
            return;
        }

        manager.abandonQuest(player.getUniqueId(), id);
        player.sendMessage(Component.text("Abandoned quest '" + id + "'.", NamedTextColor.YELLOW));
    }

    private void handleReload(Player player) {
        manager.destroyAll();
        config.loadAll(manager);
        player.sendMessage(Component.text("Reloaded " + manager.getAllQuests().size() + " quests.",
                NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .filter(s -> !(sender instanceof Player p) || p.hasPermission("servercore.quest." + s))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("abandon") && sender instanceof Player player) {
            List<String> activeIds = manager.getActiveQuests(player.getUniqueId()).stream()
                    .map(QuestProgress::getQuestId).toList();
            return filter(activeIds, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
