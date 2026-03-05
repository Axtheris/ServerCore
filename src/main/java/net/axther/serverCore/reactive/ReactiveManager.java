package net.axther.serverCore.reactive;

import net.axther.serverCore.api.event.ReactiveRuleTriggeredEvent;
import net.axther.serverCore.cosmetic.CosmeticInstance;
import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.pet.PetInstance;
import net.axther.serverCore.pet.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Central manager for the reactive cosmetics system.
 * Evaluates all registered rules against online players every tick cycle
 * and applies/removes effects as conditions change.
 */
public class ReactiveManager {

    private final List<ReactiveRule> rules = new ArrayList<>();
    private final Map<UUID, Set<String>> activeEffects = new HashMap<>();

    /**
     * Registers a reactive rule.
     */
    public void register(ReactiveRule rule) {
        rules.add(rule);
    }

    /**
     * Clears all registered rules (used during reload).
     */
    public void clearRules() {
        rules.clear();
    }

    /**
     * Returns the number of registered rules.
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * Evaluates all rules for all online players.
     * For each player, tests conditions and applies/removes effects
     * to their active pets and nearby cosmetics.
     */
    public void evaluate(CosmeticManager cosmeticManager, PetManager petManager) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location location = player.getLocation();
            UUID playerUuid = player.getUniqueId();
            Set<String> playerActive = activeEffects.computeIfAbsent(playerUuid, k -> new HashSet<>());

            for (ReactiveRule rule : rules) {
                boolean conditionsMet = rule.testConditions(player, location);
                boolean wasActive = playerActive.contains(rule.getId());

                if (conditionsMet && !wasActive) {
                    // Activate: apply effects
                    playerActive.add(rule.getId());
                    Bukkit.getPluginManager().callEvent(new ReactiveRuleTriggeredEvent(player, rule.getId()));
                    applyRuleEffects(rule, player, cosmeticManager, petManager);
                } else if (conditionsMet && wasActive) {
                    // Still active: re-apply transient effects (particles)
                    applyRuleEffects(rule, player, cosmeticManager, petManager);
                } else if (!conditionsMet && wasActive) {
                    // Deactivate: remove effects
                    playerActive.remove(rule.getId());
                    removeRuleEffects(rule, player, cosmeticManager, petManager);
                }
            }

            // Clean up players with no active effects
            if (playerActive.isEmpty()) {
                activeEffects.remove(playerUuid);
            }
        }

        // Clean up offline players
        activeEffects.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    /**
     * Applies a rule's effects to the player's pets and/or cosmetics.
     */
    private void applyRuleEffects(ReactiveRule rule, Player player,
                                  CosmeticManager cosmeticManager, PetManager petManager) {
        if (rule.targetsType("pets")) {
            for (PetInstance pet : petManager.getPets(player.getUniqueId())) {
                ArmorStand stand = pet.getStand();
                if (stand != null && !stand.isDead()) {
                    rule.applyEffects(stand, player);
                }
            }
        }

        if (rule.targetsType("cosmetics")) {
            // Apply to cosmetic stands near the player
            for (var entry : cosmeticManager.getActiveCosmetics().entrySet()) {
                for (CosmeticInstance instance : entry.getValue()) {
                    ArmorStand stand = getStandFromInstance(instance);
                    if (stand != null && !stand.isDead()) {
                        double distSq = stand.getLocation().distanceSquared(player.getLocation());
                        if (distSq <= 256) { // 16 blocks radius
                            rule.applyEffects(stand, player);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes a rule's effects from the player's pets and/or cosmetics.
     */
    private void removeRuleEffects(ReactiveRule rule, Player player,
                                   CosmeticManager cosmeticManager, PetManager petManager) {
        if (rule.targetsType("pets")) {
            for (PetInstance pet : petManager.getPets(player.getUniqueId())) {
                ArmorStand stand = pet.getStand();
                if (stand != null && !stand.isDead()) {
                    rule.removeEffects(stand, player);
                }
            }
        }

        if (rule.targetsType("cosmetics")) {
            for (var entry : cosmeticManager.getActiveCosmetics().entrySet()) {
                for (CosmeticInstance instance : entry.getValue()) {
                    ArmorStand stand = getStandFromInstance(instance);
                    if (stand != null && !stand.isDead()) {
                        double distSq = stand.getLocation().distanceSquared(player.getLocation());
                        if (distSq <= 256) {
                            rule.removeEffects(stand, player);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes all active effects for all players (used on shutdown).
     */
    public void clearAll(CosmeticManager cosmeticManager, PetManager petManager) {
        for (var entry : activeEffects.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            for (String ruleId : entry.getValue()) {
                ReactiveRule rule = rules.stream()
                        .filter(r -> r.getId().equals(ruleId))
                        .findFirst()
                        .orElse(null);
                if (rule != null) {
                    removeRuleEffects(rule, player, cosmeticManager, petManager);
                }
            }
        }
        activeEffects.clear();
    }

    /**
     * Helper to get an ArmorStand from a CosmeticInstance via Bukkit entity lookup.
     */
    private ArmorStand getStandFromInstance(CosmeticInstance instance) {
        var entity = Bukkit.getEntity(instance.getStandUuid());
        return entity instanceof ArmorStand stand ? stand : null;
    }
}
