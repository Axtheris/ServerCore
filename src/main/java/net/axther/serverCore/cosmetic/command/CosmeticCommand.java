package net.axther.serverCore.cosmetic.command;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.calibrate.CalibrationListener;
import net.axther.serverCore.cosmetic.calibrate.CalibrationSession;
import net.axther.serverCore.cosmetic.config.CosmeticConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

import java.util.List;

public class CosmeticCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("apply", "remove", "clear", "info", "calibrate");
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
            player.sendMessage("Usage: /cosmetic <apply|remove|clear|info|calibrate>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "apply" -> handleApply(player);
            case "remove" -> handleRemove(player);
            case "clear" -> handleClear(player);
            case "info" -> handleInfo(player);
            case "calibrate" -> handleCalibrate(player, args);
            default -> player.sendMessage("Usage: /cosmetic <apply|remove|clear|info|calibrate>");
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
