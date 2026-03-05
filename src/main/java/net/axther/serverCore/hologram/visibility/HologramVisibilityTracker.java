package net.axther.serverCore.hologram.visibility;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.condition.HologramCondition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class HologramVisibilityTracker {

    private final JavaPlugin plugin;
    private final HologramManager manager;
    private final double viewDistance;
    private final Map<UUID, Set<String>> hiddenHolograms = new HashMap<>();

    public HologramVisibilityTracker(JavaPlugin plugin, HologramManager manager, double viewDistance) {
        this.plugin = plugin;
        this.manager = manager;
        this.viewDistance = viewDistance;
    }

    public void update(int tickCount) {
        for (Hologram hologram : manager.getAll()) {
            if (!hologram.isSpawned() || !hologram.hasConditions()) continue;
            if (tickCount % hologram.getUpdateInterval() != 0) continue;

            Entity entity = Bukkit.getEntity(hologram.getEntityUuid());
            if (entity == null) continue;

            Location holoLoc = hologram.getLocation();
            double viewDistSq = viewDistance * viewDistance;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().getName().equals(hologram.getWorldName())) continue;
                if (player.getLocation().distanceSquared(holoLoc) > viewDistSq) continue;

                boolean passes = evaluateConditions(player, hologram.getConditions());
                Set<String> hidden = hiddenHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

                if (passes && hidden.contains(hologram.getId())) {
                    player.showEntity(plugin, entity);
                    hidden.remove(hologram.getId());
                } else if (!passes && !hidden.contains(hologram.getId())) {
                    player.hideEntity(plugin, entity);
                    hidden.add(hologram.getId());
                }
            }
        }
    }

    private boolean evaluateConditions(Player player, List<HologramCondition> conditions) {
        for (HologramCondition condition : conditions) {
            if (!evaluateCondition(player, condition)) return false;
        }
        return true;
    }

    private boolean evaluateCondition(Player player, HologramCondition condition) {
        return switch (condition.getType()) {
            case "permission" -> player.hasPermission(condition.getValue());
            case "world" -> player.getWorld().getName().equals(condition.getValue());
            case "quest_active" -> {
                var api = net.axther.serverCore.api.ServerCoreAPI.get();
                var qm = api != null ? api.getQuestManager() : null;
                yield qm != null && qm.isActive(player.getUniqueId(), condition.getValue());
            }
            case "quest_complete" -> {
                var api = net.axther.serverCore.api.ServerCoreAPI.get();
                var qm = api != null ? api.getQuestManager() : null;
                yield qm != null && qm.getCompletedQuests(player.getUniqueId()).containsKey(condition.getValue());
            }
            case "placeholder" -> evaluatePlaceholderCondition(player, condition);
            default -> true;
        };
    }

    private boolean evaluatePlaceholderCondition(Player player, HologramCondition condition) {
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders",
                    org.bukkit.OfflinePlayer.class, String.class);
            String resolved = (String) setPlaceholders.invoke(null, player, condition.getValue());

            if (condition.getEquals() != null) {
                return resolved.equals(condition.getEquals());
            }
            if (condition.getMin() != null) {
                try {
                    double val = Double.parseDouble(resolved);
                    double min = Double.parseDouble(condition.getMin());
                    return val >= min;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true; // PlaceholderAPI not present — pass by default
        }
    }

    public void handlePlayerQuit(UUID playerId) {
        hiddenHolograms.remove(playerId);
    }

    public void clearAll() {
        hiddenHolograms.clear();
    }
}
