package net.axther.serverCore.quest.gui;

import net.axther.serverCore.gui.ConfirmationMenu;
import net.axther.serverCore.gui.MenuItem;
import net.axther.serverCore.gui.PaginatedMenu;
import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestObjective;
import net.axther.serverCore.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestGUI {

    private final QuestManager manager;

    public QuestGUI(QuestManager manager) {
        this.manager = manager;
    }

    public void openJournal(Player player) {
        List<QuestProgress> active = manager.getActiveQuests(player.getUniqueId());
        MiniMessage mm = MiniMessage.miniMessage();
        List<MenuItem> items = new ArrayList<>();

        for (QuestProgress progress : active) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            ItemStack icon = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(mm.deserialize(quest.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>" + quest.getDescription()));
            lore.add(Component.empty());

            // Objective progress bars
            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                int current = Math.min(progress.getProgress(i), obj.getAmount());
                int total = obj.getAmount();
                String bar = buildProgressBar(current, total);
                String desc = obj.getDescription() != null ? obj.getDescription()
                        : obj.getType().name() + " " + obj.getTarget();
                boolean done = current >= total;
                String prefix = done ? "<green>+" : "<gray>-";
                lore.add(mm.deserialize(prefix + " " + desc + " " + bar));
            }

            // Time remaining
            if (quest.getTimeLimit() > 0) {
                long elapsed = (System.currentTimeMillis() - progress.getStartedAt()) / 1000;
                long remaining = quest.getTimeLimit() - elapsed;
                lore.add(Component.empty());
                if (remaining > 0) {
                    lore.add(mm.deserialize("<gray>Time: <yellow>" + formatTime(remaining)));
                } else {
                    lore.add(mm.deserialize("<red>EXPIRED"));
                }
            }

            lore.add(Component.empty());
            lore.add(mm.deserialize("<dark_gray>Right-click to abandon"));

            meta.lore(lore);
            icon.setItemMeta(meta);

            String questId = quest.getId();
            items.add(MenuItem.builder(icon)
                    .onRightClick(p -> {
                        ConfirmationMenu.create(
                                "<red>Abandon " + questId + "?",
                                () -> {
                                    manager.abandonQuest(p.getUniqueId(), questId);
                                    p.sendMessage(mm.deserialize("<yellow>Abandoned quest '" + questId + "'."));
                                    openJournal(p); // re-open
                                },
                                () -> openJournal(p),
                                null
                        ).open(p);
                    })
                    .build());
        }

        if (items.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(mm.deserialize("<gray>No active quests"));
            empty.setItemMeta(meta);
            items.add(MenuItem.builder(empty).build());
        }

        PaginatedMenu.paginatedBuilder("<green>Quest Journal")
                .contentItems(items)
                .build()
                .open(player);
    }

    private String buildProgressBar(int current, int total) {
        int filled = total > 0 ? (int) ((double) current / total * 8) : 0;
        int empty = 8 - filled;
        return "<dark_green>" + "#".repeat(filled) + "<dark_gray>" + "-".repeat(empty)
                + " <white>" + current + "/" + total;
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
