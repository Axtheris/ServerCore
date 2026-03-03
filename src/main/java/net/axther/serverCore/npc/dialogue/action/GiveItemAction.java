package net.axther.serverCore.npc.dialogue.action;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveItemAction implements DialogueAction {

    private final String materialName;

    public GiveItemAction(String materialName) {
        this.materialName = materialName;
    }

    @Override
    public void execute(Player player) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) return;

        ItemStack item = new ItemStack(material, 1);
        player.getInventory().addItem(item);
    }
}
