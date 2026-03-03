package net.axther.serverCore.reactive.effect;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Swaps the head item on the armor stand to a different material.
 * Saves the original item per-stand so it can be restored when the effect is removed.
 *
 * Config format:
 * <pre>
 * type: swap-item
 * slot: head
 * material: RED_MUSHROOM
 * </pre>
 */
public class SwapItemEffect implements ReactiveEffect {

    private final EquipmentSlot slot;
    private final Material material;
    private final Map<UUID, ItemStack> originalItems = new HashMap<>();

    public SwapItemEffect(EquipmentSlot slot, Material material) {
        this.slot = slot;
        this.material = material;
    }

    @Override
    public void apply(ArmorStand stand, Player owner) {
        UUID standUuid = stand.getUniqueId();
        if (!originalItems.containsKey(standUuid)) {
            ItemStack original = stand.getItem(slot);
            originalItems.put(standUuid, original != null ? original.clone() : null);
        }
        stand.setItem(slot, new ItemStack(material));
    }

    @Override
    public void remove(ArmorStand stand, Player owner) {
        UUID standUuid = stand.getUniqueId();
        ItemStack original = originalItems.remove(standUuid);
        if (original != null) {
            stand.setItem(slot, original);
        }
    }

    public static SwapItemEffect parse(ConfigurationSection section) {
        String slotName = section.getString("slot", "head").toUpperCase();
        EquipmentSlot slot;
        try {
            slot = EquipmentSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            slot = EquipmentSlot.HEAD;
        }

        String materialName = section.getString("material", "RED_MUSHROOM").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.RED_MUSHROOM;
        }

        return new SwapItemEffect(slot, material);
    }
}
