package net.axther.serverCore.command;

import net.axther.serverCore.config.ServerCoreConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ServerCoreCommand implements TabExecutor {

    private final ServerCoreConfig config;
    private final DebugContext debugContext;

    private static final String[] SYSTEM_NAMES = {
            "cosmetics", "emitters", "pets", "holograms",
            "npcs", "timelines", "reactive", "gui"
    };

    public ServerCoreCommand(ServerCoreConfig config, DebugContext debugContext) {
        this.config = config;
        this.debugContext = debugContext;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /servercore <reload|debug>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("servercore.admin.reload")) {
                sender.sendMessage("No permission.");
                return true;
            }
            config.reload();

            sender.sendMessage("ServerCore config reloaded.");
            for (String system : SYSTEM_NAMES) {
                boolean enabled = config.isSystemEnabled(system);
                sender.sendMessage("  " + system + ": " + (enabled ? "enabled" : "disabled"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("servercore.admin.debug")) {
                sender.sendMessage("No permission.");
                return true;
            }
            printDebug(sender);
            return true;
        }

        sender.sendMessage("Unknown sub-command. Usage: /servercore <reload|debug>");
        return true;
    }

    private void printDebug(CommandSender sender) {
        sender.sendMessage("=== ServerCore Debug ===");

        sender.sendMessage("-- Active Instances --");
        sender.sendMessage("  cosmetics:  " + countOrDisabled(debugContext.cosmeticManager(), m -> m.getActiveCosmetics().size()));
        sender.sendMessage("  emitters:   " + countOrDisabled(debugContext.emitterManager(), m -> m.getAllEmitters().size()));
        sender.sendMessage("  pets:       " + countOrDisabled(debugContext.petManager(), m -> m.getActivePetOwnerCount()));
        sender.sendMessage("  holograms:  " + countOrDisabled(debugContext.hologramManager(), m -> m.getAll().size()));
        sender.sendMessage("  npcs:       " + countOrDisabled(debugContext.npcManager(), m -> m.getAll().size()));
        sender.sendMessage("  quests:     " + countOrDisabled(debugContext.questManager(), m -> m.getAllActiveQuests().size() + " players"));
        sender.sendMessage("  timelines:  " + countOrDisabled(debugContext.timelineManager(), m -> m.getActiveCount() + " active / " + m.getRegisteredCount() + " registered"));
        sender.sendMessage("  reactive:   " + countOrDisabled(debugContext.reactiveManager(), m -> m.getRuleCount() + " rules, " + m.getActiveEffectCount() + " players affected"));
        sender.sendMessage("  menus:      " + countOrDisabled(debugContext.menuManager(), m -> m.getOpenMenus().size() + " open"));

        sender.sendMessage("-- Save State --");
        sender.sendMessage("  cosmetic-store: " + storeState(debugContext.cosmeticStore()));
        sender.sendMessage("  pet-store:      " + storeState(debugContext.petStore()));
        sender.sendMessage("  quest-store:    " + storeState(debugContext.questStore()));

        sender.sendMessage("-- Soft Dependencies --");
        sender.sendMessage("  PacketEvents:   " + presence(debugContext.packetEventsPresent()));
        sender.sendMessage("  ModelEngine:    " + presence(debugContext.modelEnginePresent()));
        sender.sendMessage("  PlaceholderAPI: " + presence(debugContext.placeholderApiPresent()));
        sender.sendMessage("  Vault:          " + presence(debugContext.vaultPresent()));

        sender.sendMessage("-- Tick Tasks --");
        sender.sendMessage("  cosmetic-task:   " + taskState(debugContext.cosmeticTask()));
        sender.sendMessage("  emitter-task:    " + taskState(debugContext.emitterTask()));
        sender.sendMessage("  pet-task:        " + taskState(debugContext.petTask()));
        sender.sendMessage("  hologram-task:   " + taskState(debugContext.hologramTask()));
        sender.sendMessage("  npc-task:        " + taskState(debugContext.npcTask()));
        sender.sendMessage("  timeline-task:   " + taskState(debugContext.timelineTask()));
        sender.sendMessage("  reactive-task:   " + taskState(debugContext.reactiveTask()));
        sender.sendMessage("  menu-task:       " + taskState(debugContext.menuTask()));
        sender.sendMessage("  save-flush-task: " + taskState(debugContext.saveFlushTask()));
    }

    private static <T> String countOrDisabled(T manager, Function<T, Object> counter) {
        if (manager == null) return "not loaded";
        return String.valueOf(counter.apply(manager));
    }

    private static String storeState(Object store) {
        if (store == null) return "not loaded";
        if (store instanceof net.axther.serverCore.cosmetic.data.CosmeticStore s) return s.isDirty() ? "dirty" : "clean";
        if (store instanceof net.axther.serverCore.pet.data.PetStore s) return s.isDirty() ? "dirty" : "clean";
        if (store instanceof net.axther.serverCore.quest.data.QuestStore s) return s.isDirty() ? "dirty" : "clean";
        return "unknown";
    }

    private static String taskState(BukkitRunnable task) {
        if (task == null) return "not loaded";
        return task.isCancelled() ? "stopped" : "running";
    }

    private static String presence(boolean flag) {
        return flag ? "present" : "absent";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(prefix) && sender.hasPermission("servercore.admin.reload")) {
                completions.add("reload");
            }
            if ("debug".startsWith(prefix) && sender.hasPermission("servercore.admin.debug")) {
                completions.add("debug");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
