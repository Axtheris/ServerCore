package net.axther.serverCore.cosmetic.command;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.calibrate.CalibrationListener;
import net.axther.serverCore.cosmetic.calibrate.CalibrationSession;
import net.axther.serverCore.cosmetic.config.CosmeticConfig;
import net.axther.serverCore.gui.ConfirmationMenu;
import net.axther.serverCore.gui.Menu;
import net.axther.serverCore.gui.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CosmeticCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("apply", "remove", "clear", "info", "calibrate", "gui");
    private static final List<String> CALIBRATE_ARGS = List.of("y", "fwd", "side", "small", "step", "save", "cancel");
    private static final List<String> DIRECTION_ARGS = List.of("+", "-");

    private final CosmeticManager manager;
    private final CosmeticConfig config;
    private final CalibrationListener calibrationListener;

    public CosmeticCommand(CosmeticManager manager, CosmeticConfig config, CalibrationListener calibrationListener) {
        this.manager = manager;
        this.config = config;
        this.calibrationListener = calibrationListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /cosmetic <apply|remove|clear|info|calibrate|gui>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "apply" -> handleApply(player);
            case "remove" -> handleRemove(player);
            case "clear" -> handleClear(player);
            case "info" -> handleInfo(player);
            case "calibrate" -> handleCalibrate(player, args);
            case "gui" -> handleGui(player);
            default -> player.sendMessage("Usage: /cosmetic <apply|remove|clear|info|calibrate|gui>");
        }

        return true;
    }

    private void handleApply(Player player) {
        LivingEntity target = raycastTarget(player);
        if (target == null) {
            player.sendMessage("No mob found. Look at a mob and try again.");
            return;
        }

        if (manager.getProfile(target.getType()) == null) {
            player.sendMessage("Cosmetics are not supported for " + target.getType().name().toLowerCase() + ".");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("Hold an item in your main hand to use as a cosmetic.");
            return;
        }

        if (manager.applyCosmetic(target, item)) {
            player.sendMessage("Cosmetic applied to " + target.getType().name().toLowerCase() + ".");
        } else {
            player.sendMessage("Failed to apply cosmetic.");
        }
    }

    private void handleRemove(Player player) {
        LivingEntity target = raycastTarget(player);
        if (target == null) {
            player.sendMessage("No mob found. Look at a mob and try again.");
            return;
        }

        if (!manager.hasCosmetics(target.getUniqueId())) {
            player.sendMessage("This mob has no cosmetics.");
            return;
        }

        manager.removeCosmetics(target.getUniqueId());
        player.sendMessage("Cosmetics removed from " + target.getType().name().toLowerCase() + ".");
    }

    private void handleClear(Player player) {
        manager.destroyAll();
        player.sendMessage("All cosmetics cleared.");
    }

    private void handleInfo(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("Hold an item in your main hand.");
            return;
        }

        player.sendMessage("--- Item Info ---");
        player.sendMessage("Type: " + item.getType().name());

        if (!item.hasItemMeta()) {
            player.sendMessage("No item meta.");
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta.hasDisplayName()) {
            player.sendMessage("Display Name: " + meta.getDisplayName());
        }

        if (meta.hasCustomModelData()) {
            player.sendMessage("Custom Model Data: " + meta.getCustomModelData());
        } else {
            player.sendMessage("Custom Model Data: none");
        }

        if (meta.hasItemModel()) {
            player.sendMessage("Item Model: " + meta.getItemModel());
        } else {
            player.sendMessage("Item Model: none");
        }

        player.sendMessage("Item Meta Type: " + meta.getClass().getSimpleName());
    }

    private void handleGui(Player player) {
        MiniMessage mm = MiniMessage.miniMessage();
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        Menu.Builder builder = Menu.builder("<gradient:#FF6B6B:#FFE66D>Cosmetic Manager</gradient>")
                .rows(6)
                .fillBorder(filler);

        // Fill middle rows with glass panes for clean look
        MenuItem fillerItem = MenuItem.builder(filler).build();
        for (int row = 1; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                builder.item(slot, fillerItem);
            }
        }

        // Show currently held item as preview in slot 13
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.getType().isAir()) {
            ItemStack preview = heldItem.clone();
            ItemMeta previewMeta = preview.getItemMeta();
            List<Component> previewLore = new ArrayList<>();
            previewLore.add(Component.empty());
            previewLore.add(mm.deserialize("<gray>Currently held item"));
            previewLore.add(mm.deserialize("<gray>This will be used as the cosmetic"));
            previewMeta.lore(previewLore);
            preview.setItemMeta(previewMeta);
            builder.item(13, MenuItem.builder(preview).build());
        } else {
            ItemStack noItem = new ItemStack(Material.BARRIER);
            ItemMeta noItemMeta = noItem.getItemMeta();
            noItemMeta.displayName(mm.deserialize("<red>No Item Held"));
            List<Component> noItemLore = new ArrayList<>();
            noItemLore.add(mm.deserialize("<gray>Hold an item in your main hand"));
            noItemLore.add(mm.deserialize("<gray>to use as a cosmetic"));
            noItemMeta.lore(noItemLore);
            noItem.setItemMeta(noItemMeta);
            builder.item(13, MenuItem.builder(noItem).build());
        }

        // Show supported mob types around the preview
        Map<EntityType, Material> mobEggs = Map.ofEntries(
                Map.entry(EntityType.PANDA, Material.PANDA_SPAWN_EGG),
                Map.entry(EntityType.COW, Material.COW_SPAWN_EGG),
                Map.entry(EntityType.SHEEP, Material.SHEEP_SPAWN_EGG),
                Map.entry(EntityType.PIG, Material.PIG_SPAWN_EGG),
                Map.entry(EntityType.CHICKEN, Material.CHICKEN_SPAWN_EGG),
                Map.entry(EntityType.WOLF, Material.WOLF_SPAWN_EGG),
                Map.entry(EntityType.CAT, Material.CAT_SPAWN_EGG),
                Map.entry(EntityType.HORSE, Material.HORSE_SPAWN_EGG),
                Map.entry(EntityType.VILLAGER, Material.VILLAGER_SPAWN_EGG),
                Map.entry(EntityType.IRON_GOLEM, Material.IRON_BLOCK),
                Map.entry(EntityType.SNOW_GOLEM, Material.SNOW_BLOCK),
                Map.entry(EntityType.RABBIT, Material.RABBIT_SPAWN_EGG),
                Map.entry(EntityType.FOX, Material.FOX_SPAWN_EGG),
                Map.entry(EntityType.BEE, Material.BEE_SPAWN_EGG),
                Map.entry(EntityType.FROG, Material.FROG_SPAWN_EGG),
                Map.entry(EntityType.CAMEL, Material.CAMEL_SPAWN_EGG),
                Map.entry(EntityType.SNIFFER, Material.SNIFFER_SPAWN_EGG),
                Map.entry(EntityType.ALLAY, Material.ALLAY_SPAWN_EGG),
                Map.entry(EntityType.AXOLOTL, Material.AXOLOTL_SPAWN_EGG),
                Map.entry(EntityType.PARROT, Material.PARROT_SPAWN_EGG),
                Map.entry(EntityType.DONKEY, Material.DONKEY_SPAWN_EGG),
                Map.entry(EntityType.ZOMBIE, Material.ZOMBIE_SPAWN_EGG),
                Map.entry(EntityType.SKELETON, Material.SKELETON_SPAWN_EGG),
                Map.entry(EntityType.CREEPER, Material.CREEPER_SPAWN_EGG)
        );

        int[] mobSlots = {19, 20, 21, 22, 23, 24, 25};
        var supportedTypes = manager.getSupportedTypes();
        int slotIdx = 0;
        for (EntityType type : supportedTypes) {
            if (slotIdx >= mobSlots.length) break;

            Material eggMat = mobEggs.getOrDefault(type, Material.GHAST_SPAWN_EGG);
            ItemStack mobItem = new ItemStack(eggMat);
            ItemMeta mobMeta = mobItem.getItemMeta();
            String typeName = type.name().charAt(0) + type.name().substring(1).toLowerCase().replace('_', ' ');
            mobMeta.displayName(mm.deserialize("<aqua>" + typeName));
            List<Component> mobLore = new ArrayList<>();
            mobLore.add(mm.deserialize("<gray>Cosmetics supported"));
            mobMeta.lore(mobLore);
            mobItem.setItemMeta(mobMeta);
            builder.item(mobSlots[slotIdx], MenuItem.builder(mobItem).build());
            slotIdx++;
        }

        // Apply to Target button (slot 29)
        ItemStack applyItem = new ItemStack(Material.LIME_DYE);
        ItemMeta applyMeta = applyItem.getItemMeta();
        applyMeta.displayName(mm.deserialize("<green><bold>Apply to Target"));
        List<Component> applyLore = new ArrayList<>();
        applyLore.add(mm.deserialize("<gray>Look at a mob and hold an item"));
        applyLore.add(mm.deserialize("<gray>to apply it as a cosmetic"));
        applyLore.add(Component.empty());
        applyLore.add(mm.deserialize("<yellow>Click to apply"));
        applyMeta.lore(applyLore);
        applyItem.setItemMeta(applyMeta);
        builder.item(29, MenuItem.builder(applyItem)
                .onClick(p -> {
                    p.closeInventory();
                    handleApply(p);
                })
                .build());

        // Clear All button (slot 31)
        ItemStack clearItem = new ItemStack(Material.TNT);
        ItemMeta clearMeta = clearItem.getItemMeta();
        clearMeta.displayName(mm.deserialize("<red><bold>Clear All Cosmetics"));
        List<Component> clearLore = new ArrayList<>();
        clearLore.add(mm.deserialize("<gray>Remove all cosmetics from"));
        clearLore.add(mm.deserialize("<gray>every mob in the world"));
        clearLore.add(Component.empty());
        clearLore.add(mm.deserialize("<red>Click to clear all"));
        clearMeta.lore(clearLore);
        clearItem.setItemMeta(clearMeta);
        builder.item(31, MenuItem.builder(clearItem)
                .onClick(p -> {
                    Menu confirmMenu = ConfirmationMenu.create(
                            "<red>Clear All Cosmetics?",
                            () -> {
                                handleClear(p);
                            },
                            () -> {
                                handleGui(p);
                            },
                            null
                    );
                    confirmMenu.open(p);
                })
                .build());

        // Remove from Target button (slot 33)
        ItemStack removeItem = new ItemStack(Material.RED_DYE);
        ItemMeta removeMeta = removeItem.getItemMeta();
        removeMeta.displayName(mm.deserialize("<red><bold>Remove from Target"));
        List<Component> removeLore = new ArrayList<>();
        removeLore.add(mm.deserialize("<gray>Look at a mob to remove"));
        removeLore.add(mm.deserialize("<gray>its cosmetic"));
        removeLore.add(Component.empty());
        removeLore.add(mm.deserialize("<yellow>Click to remove"));
        removeMeta.lore(removeLore);
        removeItem.setItemMeta(removeMeta);
        builder.item(33, MenuItem.builder(removeItem)
                .onClick(p -> {
                    p.closeInventory();
                    handleRemove(p);
                })
                .build());

        builder.build().open(player);
    }

    private void handleCalibrate(Player player, String[] args) {
        CalibrationSession session = calibrationListener.getSession(player.getUniqueId());

        // No extra args — start a new session
        if (args.length == 1) {
            if (session != null && session.isActive()) {
                player.sendMessage("You are already calibrating. Use save or cancel first.");
                return;
            }

            LivingEntity target = raycastTarget(player);
            if (target == null) {
                player.sendMessage("No mob found. Look at a mob and try again.");
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().isAir()) {
                player.sendMessage("Hold an item in your main hand to use as a preview cosmetic.");
                return;
            }

            CalibrationSession newSession = new CalibrationSession(player, target, manager, config, item);
            calibrationListener.startSession(newSession);
            return;
        }

        // All other sub-args require an active session
        if (session == null || !session.isActive()) {
            player.sendMessage("No active calibration session. Start one with /cosmetic calibrate.");
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "y" -> {
                if (args.length < 3) { player.sendMessage("Usage: /cosmetic calibrate y <+|->"); return; }
                double delta = parseDirection(args[2], session.getStep());
                if (delta == 0) { player.sendMessage("Use + or -."); return; }
                session.adjustY(delta);
            }
            case "fwd" -> {
                if (args.length < 3) { player.sendMessage("Usage: /cosmetic calibrate fwd <+|->"); return; }
                double delta = parseDirection(args[2], session.getStep());
                if (delta == 0) { player.sendMessage("Use + or -."); return; }
                session.adjustForward(delta);
            }
            case "side" -> {
                if (args.length < 3) { player.sendMessage("Usage: /cosmetic calibrate side <+|->"); return; }
                double delta = parseDirection(args[2], session.getStep());
                if (delta == 0) { player.sendMessage("Use + or -."); return; }
                session.adjustSide(delta);
            }
            case "small" -> session.toggleSmallStand();
            case "step" -> {
                if (args.length < 3) { player.sendMessage("Usage: /cosmetic calibrate step <value>"); return; }
                try {
                    double value = Double.parseDouble(args[2]);
                    if (value <= 0) { player.sendMessage("Step must be positive."); return; }
                    session.setStep(value);
                    player.sendMessage("Step size set to " + value);
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid number: " + args[2]);
                }
            }
            case "save" -> {
                session.save();
                calibrationListener.endSession(player.getUniqueId());
            }
            case "cancel" -> {
                session.cancel();
                calibrationListener.endSession(player.getUniqueId());
            }
            default -> player.sendMessage("Unknown calibrate option: " + sub);
        }
    }

    private double parseDirection(String arg, double step) {
        return switch (arg) {
            case "+" -> step;
            case "-" -> -step;
            default -> 0;
        };
    }

    private LivingEntity raycastTarget(Player player) {
        RayTraceResult result = player.rayTraceEntities(5, false);
        if (result == null) return null;

        Entity entity = result.getHitEntity();
        if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
            return living;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("calibrate")) {
            String prefix = args[1].toLowerCase();
            return CALIBRATE_ARGS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("calibrate")) {
            String sub = args[1].toLowerCase();
            if (sub.equals("y") || sub.equals("fwd") || sub.equals("side")) {
                String prefix = args[2];
                return DIRECTION_ARGS.stream().filter(s -> s.startsWith(prefix)).toList();
            }
        }
        return List.of();
    }
}
