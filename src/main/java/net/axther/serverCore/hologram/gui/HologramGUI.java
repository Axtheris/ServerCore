package net.axther.serverCore.hologram.gui;

import net.axther.serverCore.gui.ConfirmationMenu;
import net.axther.serverCore.gui.MenuItem;
import net.axther.serverCore.gui.PaginatedMenu;
import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.config.HologramConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HologramGUI {

    private final HologramManager manager;
    private final HologramConfig config;

    public HologramGUI(HologramManager manager, HologramConfig config) {
        this.manager = manager;
        this.config = config;
    }

    public void openBrowser(Player player) {
        MiniMessage mm = MiniMessage.miniMessage();
        List<MenuItem> items = new ArrayList<>();

        for (Hologram hologram : manager.getAll()) {
            ItemStack icon = new ItemStack(Material.OAK_SIGN);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(mm.deserialize("<aqua>" + hologram.getId()));

            List<Component> lore = new ArrayList<>();
            Location loc = hologram.getLocation();
            if (loc != null) {
                lore.add(mm.deserialize("<gray>World: <white>" + (loc.getWorld() != null ? loc.getWorld().getName() : "?")));
                lore.add(mm.deserialize("<gray>Location: <white>" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));
            }
            lore.add(mm.deserialize("<gray>Animation: <white>" + hologram.getAnimation().name()));
            lore.add(mm.deserialize("<gray>Lines: <white>" + hologram.getLines().size()));
            lore.add(mm.deserialize("<gray>Spawned: " + (hologram.isSpawned() ? "<green>Yes" : "<red>No")));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<yellow>Left-click to teleport"));
            lore.add(mm.deserialize("<red>Right-click to delete"));

            meta.lore(lore);
            icon.setItemMeta(meta);

            String holoId = hologram.getId();
            items.add(MenuItem.builder(icon)
                    .onClick(p -> {
                        if (loc != null) {
                            p.teleport(loc);
                            p.sendMessage(mm.deserialize("<green>Teleported to hologram '" + holoId + "'."));
                        }
                        p.closeInventory();
                    })
                    .onRightClick(p -> {
                        ConfirmationMenu.create(
                                "<red>Delete hologram '" + holoId + "'?",
                                () -> {
                                    manager.unregister(holoId);
                                    config.saveAll(manager);
                                    p.sendMessage(mm.deserialize("<yellow>Deleted hologram '" + holoId + "'."));
                                    openBrowser(p);
                                },
                                () -> openBrowser(p),
                                null
                        ).open(p);
                    })
                    .build());
        }

        if (items.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(mm.deserialize("<gray>No holograms"));
            empty.setItemMeta(meta);
            items.add(MenuItem.builder(empty).build());
        }

        PaginatedMenu.paginatedBuilder("<gold>Hologram Manager")
                .contentItems(items)
                .build()
                .open(player);
    }
}
