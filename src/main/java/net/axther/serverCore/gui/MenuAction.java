package net.axther.serverCore.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Represents a click action that can be triggered by a menu item.
 * <p>
 * Supported action types:
 * <ul>
 *   <li>{@code command} — runs a console command (supports {@code %player%} placeholder)</li>
 *   <li>{@code player_command} — runs a command as the player</li>
 *   <li>{@code message} — sends a MiniMessage-formatted message to the player</li>
 *   <li>{@code sound} — plays a sound to the player</li>
 *   <li>{@code close} — closes the player's inventory</li>
 *   <li>{@code open_menu} — opens another menu by config ID</li>
 *   <li>{@code back} — returns to the parent menu, or closes if none</li>
 * </ul>
 */
public class MenuAction {

    private final String type;
    private final String value;

    private MenuAction(String type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a new MenuAction from a type string and value.
     *
     * @param type  the action type (e.g. "command", "close", "open_menu")
     * @param value the action value (interpretation depends on type)
     * @return a new MenuAction instance
     */
    public static MenuAction parse(String type, String value) {
        return new MenuAction(type, value);
    }

    /**
     * Executes this action for the given player.
     *
     * @param player      the player who triggered the action
     * @param currentMenu the menu that was open when the action was triggered
     * @param menuConfig  the menu configuration (used for open_menu actions)
     */
    public void execute(Player player, Menu currentMenu, MenuConfig menuConfig) {
        switch (type) {
            case "command" -> {
                String cmd = value.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            case "player_command" -> {
                player.performCommand(value.replace("%player%", player.getName()));
            }
            case "message" -> {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        value.replace("%player%", player.getName())));
            }
            case "sound" -> {
                try {
                    Sound sound = Sound.valueOf(value.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    Logger.getLogger("ServerCore").warning("Unknown sound in menu action: " + value);
                }
            }
            case "close" -> player.closeInventory();
            case "open_menu" -> {
                if (menuConfig != null) {
                    Menu menu = menuConfig.buildMenu(value);
                    if (menu != null) {
                        menu.open(player);
                    }
                }
            }
            case "back" -> {
                if (currentMenu != null && currentMenu.getParent() != null) {
                    currentMenu.getParent().open(player);
                } else {
                    player.closeInventory();
                }
            }
            default -> Logger.getLogger("ServerCore").warning("Unknown menu action type: " + type);
        }
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
