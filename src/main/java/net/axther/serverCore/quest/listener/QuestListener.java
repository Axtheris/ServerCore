package net.axther.serverCore.quest.listener;

import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestObjective;
import net.axther.serverCore.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class QuestListener implements Listener {

    private final QuestManager manager;
    private boolean actionBarEnabled = true;

    public QuestListener(QuestManager manager) {
        this.manager = manager;
    }

    public void setActionBarEnabled(boolean enabled) {
        this.actionBarEnabled = enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String entityTypeName = event.getEntityType().name();
        manager.handleKill(killer.getUniqueId(), entityTypeName);
        sendProgressBar(killer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String materialName = event.getRecipe().getResult().getType().name();
        int amount;
        if (event.isShiftClick()) {
            amount = estimateShiftCraftAmount(event);
        } else {
            amount = event.getRecipe().getResult().getAmount();
        }

        manager.handleCraft(player.getUniqueId(), materialName, amount);
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        manager.handleMine(player.getUniqueId(), event.getBlock().getType().name());
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        manager.handlePlace(player.getUniqueId(), event.getBlock().getType().name());
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        String material;
        if (event.getCaught() instanceof Item item) {
            material = item.getItemStack().getType().name();
        } else {
            material = "ANY";
        }

        manager.handleFish(player.getUniqueId(), material);
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;

        manager.handleBreed(player.getUniqueId(), event.getEntityType().name());
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        manager.handleSmelt(player.getUniqueId(), event.getItemType().name(), event.getItemAmount());
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        manager.handleInteract(player.getUniqueId(), event.getClickedBlock().getType().name());
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        manager.handleInteract(player.getUniqueId(), event.getRightClicked().getType().name());
        sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        manager.handleExplore(player.getUniqueId(), event.getTo());
        manager.checkExpiredQuests(player);
    }

    // --- Action bar progress display ---

    private void sendProgressBar(Player player) {
        if (!actionBarEnabled) return;
        var active = manager.getActiveQuests(player.getUniqueId());
        if (active.isEmpty()) return;

        for (QuestProgress progress : active) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            var objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                int current = progress.getProgress(i);
                if (obj.getType() == QuestObjective.Type.FETCH) {
                    current = manager.countMaterial(player, obj.getTarget());
                }
                if (current < obj.getAmount()) {
                    String desc = obj.getDescription() != null ? obj.getDescription() : generateDescription(obj);
                    player.sendActionBar(Component.text(
                            quest.getDisplayName() + ": " + desc + " (" + Math.min(current, obj.getAmount()) + "/" + obj.getAmount() + ")",
                            NamedTextColor.AQUA));
                    return;
                }
            }
        }
    }

    private String generateDescription(QuestObjective obj) {
        return switch (obj.getType()) {
            case FETCH -> "Collect " + obj.getTarget();
            case KILL -> "Kill " + obj.getTarget();
            case TALK -> "Talk to " + obj.getTarget();
            case CRAFT -> "Craft " + obj.getTarget();
            case MINE -> "Mine " + obj.getTarget();
            case PLACE -> "Place " + obj.getTarget();
            case FISH -> obj.getTarget().equals("ANY") ? "Catch fish" : "Catch " + obj.getTarget();
            case BREED -> "Breed " + obj.getTarget();
            case SMELT -> "Smelt " + obj.getTarget();
            case EXPLORE -> "Explore location";
            case INTERACT -> "Interact with " + obj.getTarget();
        };
    }

    private int estimateShiftCraftAmount(CraftItemEvent event) {
        int resultAmount = event.getRecipe().getResult().getAmount();
        int min = 64;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                min = Math.min(min, item.getAmount());
            }
        }
        return min * resultAmount;
    }
}
