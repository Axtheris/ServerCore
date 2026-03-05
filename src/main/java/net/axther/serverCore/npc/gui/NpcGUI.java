package net.axther.serverCore.npc.gui;

import net.axther.serverCore.gui.MenuItem;
import net.axther.serverCore.gui.PaginatedMenu;
import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class NpcGUI {

    private final NPCManager manager;

    public NpcGUI(NPCManager manager) {
        this.manager = manager;
    }

    public void openBrowser(Player player) {
        MiniMessage mm = MiniMessage.miniMessage();
        List<MenuItem> items = new ArrayList<>();

        for (NPC npc : manager.getAll()) {
            ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(mm.deserialize(npc.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>ID: <white>" + npc.getId()));

            Location loc = npc.getLocation();
            if (loc != null) {
                lore.add(mm.deserialize("<gray>World: <white>" + (loc.getWorld() != null ? loc.getWorld().getName() : "?")));
                lore.add(mm.deserialize("<gray>Location: <white>" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));
            }

            String dialogueStatus = npc.getDialogueId() != null ? "<green>Has dialogue" : "<gray>No dialogue";
            lore.add(mm.deserialize("<gray>Dialogue: " + dialogueStatus));

            boolean hasSkin = npc.getSkinTexture() != null && !npc.getSkinTexture().isEmpty();
            lore.add(mm.deserialize("<gray>Skin: " + (hasSkin ? "<green>Custom" : "<gray>Default")));

            lore.add(Component.empty());
            lore.add(mm.deserialize("<yellow>Click to teleport"));

            meta.lore(lore);
            icon.setItemMeta(meta);

            items.add(MenuItem.builder(icon)
                    .onClick(p -> {
                        if (loc != null) {
                            p.teleport(loc);
                            p.sendMessage(mm.deserialize("<green>Teleported to NPC '" + npc.getId() + "'."));
                        }
                        p.closeInventory();
                    })
                    .build());
        }

        if (items.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(mm.deserialize("<gray>No NPCs"));
            empty.setItemMeta(meta);
            items.add(MenuItem.builder(empty).build());
        }

        PaginatedMenu.paginatedBuilder("<gold>NPC Browser")
                .contentItems(items)
                .build()
                .open(player);
    }
}
