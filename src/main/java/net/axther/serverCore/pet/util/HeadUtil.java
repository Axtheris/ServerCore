package net.axther.serverCore.pet.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public final class HeadUtil {

    private HeadUtil() {}

    public static ItemStack fromTexture(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack fromCustomModelData(Material material, int customModelData) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }
}
