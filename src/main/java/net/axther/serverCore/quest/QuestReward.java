package net.axther.serverCore.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class QuestReward {

    public enum Type { ITEM, XP, COMMAND, MONEY, PERMISSION, PET }

    private final Type type;
    private final String value;  // material name, xp amount, or command string
    private final int amount;

    public QuestReward(Type type, String value, int amount) {
        this.type = type;
        this.value = value;
        this.amount = amount;
    }

    public static QuestReward fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "item");
        return switch (typeStr.toLowerCase()) {
            case "xp" -> new QuestReward(Type.XP, "", section.getInt("amount", 0));
            case "command" -> new QuestReward(Type.COMMAND, section.getString("value", ""), 1);
            case "money" -> new QuestReward(Type.MONEY, "", section.getInt("amount", 0));
            case "permission" -> new QuestReward(Type.PERMISSION,
                    section.getString("value", ""), section.getInt("duration", 0));
            case "pet" -> new QuestReward(Type.PET,
                    section.getString("value", ""), section.getInt("amount", 1));
            default -> new QuestReward(Type.ITEM,
                    section.getString("material", "DIRT"), section.getInt("amount", 1));
        };
    }

    public void give(Player player) {
        switch (type) {
            case ITEM -> {
                Material material = Material.matchMaterial(value);
                if (material != null) {
                    player.getInventory().addItem(new ItemStack(material, amount));
                }
            }
            case XP -> player.giveExp(amount);
            case COMMAND -> {
                String cmd = value.replace("%player%", player.getName());
                player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
            }
            case MONEY -> {
                try {
                    Class<?> vaultHook = Class.forName("net.axther.serverCore.hook.VaultHook");
                    Method hasEconomy = vaultHook.getMethod("hasEconomy");
                    if ((boolean) hasEconomy.invoke(null)) {
                        Method deposit = vaultHook.getMethod("deposit", Player.class, double.class);
                        deposit.invoke(null, player, (double) amount);
                    } else {
                        Logger.getLogger("ServerCore").warning("Vault economy not available for MONEY reward");
                    }
                } catch (Exception e) {
                    Logger.getLogger("ServerCore").warning("Vault not available for MONEY reward: " + e.getMessage());
                }
            }
            case PERMISSION -> {
                try {
                    Class<?> vaultHook = Class.forName("net.axther.serverCore.hook.VaultHook");
                    Method hasPermissions = vaultHook.getMethod("hasPermissions");
                    if ((boolean) hasPermissions.invoke(null)) {
                        Method addPermission = vaultHook.getMethod("addPermission", Player.class, String.class);
                        addPermission.invoke(null, player, value);
                        if (amount > 0) {
                            // Schedule permission removal after duration (amount in seconds)
                            Bukkit.getScheduler().runTaskLater(
                                    Bukkit.getPluginManager().getPlugin("ServerCore"),
                                    () -> {
                                        try {
                                            Method removePermission = vaultHook.getMethod("removePermission", Player.class, String.class);
                                            removePermission.invoke(null, player, value);
                                        } catch (Exception ex) {
                                            Logger.getLogger("ServerCore").warning("Failed to remove timed permission: " + ex.getMessage());
                                        }
                                    },
                                    amount * 20L
                            );
                        }
                    } else {
                        Logger.getLogger("ServerCore").warning("Vault permissions not available for PERMISSION reward");
                    }
                } catch (Exception e) {
                    Logger.getLogger("ServerCore").warning("Vault not available for PERMISSION reward: " + e.getMessage());
                }
            }
            case PET -> {} // Handled by QuestManager which has access to PetManager
        }
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public int getAmount() { return amount; }
}
