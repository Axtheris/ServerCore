package net.axther.serverCore.npc.dialogue.condition;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HasItemCondition implements DialogueCondition {

    private final String materialName;

    public HasItemCondition(String materialName) {
        this.materialName = materialName;
    }

    @Override
    public boolean test(Player player) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) return false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                return true;
            }
        }
        return false;
    }
}
