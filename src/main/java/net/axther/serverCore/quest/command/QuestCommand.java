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

import java.util.List;
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
            case "active" -> handleActive(player);
            case "completed" -> handleCompleted(player);
            case "abandon" -> handleAbandon(player, args);
            case "reload" -> handleReload(player);
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

        for (QuestProgress progress : active) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            player.sendMessage(Component.text(" ").append(mm.deserialize(quest.getDisplayName())));

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                int current = progress.getProgress(i);

                // For fetch objectives, show inventory count dynamically
                if (obj.getType() == QuestObjective.Type.FETCH) {
                    current = manager.countMaterial(player, obj.getTarget());
                }

                String desc = switch (obj.getType()) {
                    case FETCH -> "Collect " + obj.getAmount() + " " + obj.getTarget();
                    case KILL -> "Kill " + obj.getAmount() + " " + obj.getTarget();
                    case TALK -> "Talk to " + obj.getTarget();
                };

                boolean done = current >= obj.getAmount();
                player.sendMessage(Component.text("   " + (done ? "+" : "-") + " " + desc
                                + " (" + Math.min(current, obj.getAmount()) + "/" + obj.getAmount() + ")",
                        done ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            }
        }
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
            return filter(SUBCOMMANDS, args[0]);
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
