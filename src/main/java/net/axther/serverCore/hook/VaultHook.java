package net.axther.serverCore.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Isolated helper for Vault economy and permissions API calls.
 * This class is only classloaded when Vault is confirmed present at runtime,
 * keeping all Vault imports in one place to avoid ClassNotFoundException.
 */
public final class VaultHook {

    private static Economy economy;
    private static Permission permission;

    private VaultHook() {}

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return true;
    }

    public static boolean setupPermissions() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp == null) return false;
        permission = rsp.getProvider();
        return true;
    }

    public static boolean hasEconomy() { return economy != null; }
    public static boolean hasPermissions() { return permission != null; }

    public static boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public static boolean addPermission(Player player, String node) {
        if (permission == null) return false;
        return permission.playerAdd(player, node);
    }

    public static boolean removePermission(Player player, String node) {
        if (permission == null) return false;
        return permission.playerRemove(player, node);
    }
}
