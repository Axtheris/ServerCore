package net.axther.serverCore.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Factory for a standard 3-row confirmation dialog menu.
 * Slot 11 holds a green wool "Confirm" button and slot 15 holds a red wool "Cancel" button.
 */
public final class ConfirmationMenu {

    private ConfirmationMenu() {
    }

    /**
     * Creates a 3-row confirmation menu with confirm/cancel buttons.
     *
     * @param title     MiniMessage-formatted title
     * @param onConfirm action executed when the player clicks Confirm
     * @param onCancel  action executed when the player clicks Cancel
     * @param parent    optional parent menu to return to (may be null)
     * @return a ready-to-open {@link Menu}
     */
    public static Menu create(String title, Runnable onConfirm, Runnable onCancel, Menu parent) {
        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(MiniMessage.miniMessage().deserialize("<green><bold>Confirm"));
        confirmItem.setItemMeta(confirmMeta);

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>Cancel"));
        cancelItem.setItemMeta(cancelMeta);

        return Menu.builder(title)
                .rows(3)
                .parent(parent)
                .item(11, MenuItem.builder(confirmItem)
                        .onClick(player -> {
                            player.closeInventory();
                            onConfirm.run();
                        })
                        .build())
                .item(15, MenuItem.builder(cancelItem)
                        .onClick(player -> {
                            player.closeInventory();
                            onCancel.run();
                        })
                        .build())
                .build();
    }
}
