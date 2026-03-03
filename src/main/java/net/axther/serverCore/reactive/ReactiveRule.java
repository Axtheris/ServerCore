package net.axther.serverCore.reactive;

import net.axther.serverCore.reactive.condition.ReactiveCondition;
import net.axther.serverCore.reactive.effect.ReactiveEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A reactive rule that ties conditions to effects.
 * When all conditions are met for a player, the effects are applied
 * to that player's pets and/or cosmetics (based on targets).
 */
public class ReactiveRule {

    private final String id;
    private final List<ReactiveCondition> conditions;
    private final List<String> targets; // "pets", "cosmetics"
    private final List<ReactiveEffect> effects;
    private boolean active;

    public ReactiveRule(String id, List<ReactiveCondition> conditions, List<String> targets, List<ReactiveEffect> effects) {
        this.id = id;
        this.conditions = conditions;
        this.targets = targets;
        this.effects = effects;
        this.active = false;
    }

    /**
     * Tests whether all conditions are met for the given player.
     */
    public boolean testConditions(Player player, Location location) {
        for (ReactiveCondition condition : conditions) {
            if (!condition.test(player, location)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies all effects to the given armor stand.
     */
    public void applyEffects(ArmorStand stand, Player owner) {
        for (ReactiveEffect effect : effects) {
            effect.apply(stand, owner);
        }
    }

    /**
     * Removes all effects from the given armor stand.
     */
    public void removeEffects(ArmorStand stand, Player owner) {
        for (ReactiveEffect effect : effects) {
            effect.remove(stand, owner);
        }
    }

    public boolean targetsType(String type) {
        return targets.contains(type.toLowerCase());
    }

    public String getId() {
        return id;
    }

    public List<ReactiveCondition> getConditions() {
        return conditions;
    }

    public List<String> getTargets() {
        return targets;
    }

    public List<ReactiveEffect> getEffects() {
        return effects;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Parses a ReactiveRule from a YAML configuration section.
     *
     * @param id      the rule identifier
     * @param section the config section
     * @param logger  logger for warnings
     * @return the parsed rule, or null if no valid conditions/effects were parsed
     */
    public static ReactiveRule fromConfig(String id, ConfigurationSection section, Logger logger) {
        List<ReactiveCondition> conditions = new ArrayList<>();
        List<ConfigurationSection> condSections = new ArrayList<>();
        if (section.isList("conditions")) {
            var condList = section.getMapList("conditions");
            for (int i = 0; i < condList.size(); i++) {
                ConfigurationSection sub = section.createSection("_cond_" + i, condList.get(i));
                condSections.add(sub);
            }
        }
        for (ConfigurationSection condSec : condSections) {
            ReactiveCondition cond = ReactiveCondition.fromConfig(condSec);
            if (cond != null) {
                conditions.add(cond);
            } else {
                logger.warning("Reactive rule '" + id + "': unknown condition type '" + condSec.getString("type") + "'");
            }
        }

        List<String> targets = section.getStringList("targets");
        if (targets.isEmpty()) {
            targets = List.of("pets", "cosmetics");
        }

        List<ReactiveEffect> effects = new ArrayList<>();
        List<ConfigurationSection> effectSections = new ArrayList<>();
        if (section.isList("effects")) {
            var effectList = section.getMapList("effects");
            for (int i = 0; i < effectList.size(); i++) {
                ConfigurationSection sub = section.createSection("_eff_" + i, effectList.get(i));
                effectSections.add(sub);
            }
        }
        for (ConfigurationSection effSec : effectSections) {
            ReactiveEffect effect = ReactiveEffect.fromConfig(effSec);
            if (effect != null) {
                effects.add(effect);
            } else {
                logger.warning("Reactive rule '" + id + "': unknown effect type '" + effSec.getString("type") + "'");
            }
        }

        if (conditions.isEmpty() || effects.isEmpty()) {
            logger.warning("Reactive rule '" + id + "' skipped: needs at least one condition and one effect");
            return null;
        }

        return new ReactiveRule(id, conditions, targets, effects);
    }
}
