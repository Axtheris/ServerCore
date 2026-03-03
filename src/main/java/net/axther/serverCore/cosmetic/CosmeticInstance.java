package net.axther.serverCore.cosmetic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CosmeticInstance {

    private final UUID mobUuid;
    private final UUID standUuid;
    private final MobCosmeticProfile profile;
    private final ItemStack item;

    public CosmeticInstance(UUID mobUuid, UUID standUuid, MobCosmeticProfile profile, ItemStack item) {
        this.mobUuid = mobUuid;
        this.standUuid = standUuid;
        this.profile = profile;
        this.item = item;
    }

    /**
     * Updates the armor stand position to follow the mob.
     * @return true if the instance is still valid, false if it should be removed
     */
    public boolean tick() {
        LivingEntity mob = getMob();
        ArmorStand stand = getStand();

        if (mob == null || mob.isDead() || stand == null || stand.isDead()) {
            destroy();
            return false;
        }

        Location target = profile.computeStandLocation(mob);
        stand.teleport(target);
        return true;
    }

    public void destroy() {
        ArmorStand stand = getStand();
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    public UUID getMobUuid() {
        return mobUuid;
    }

    public UUID getStandUuid() {
        return standUuid;
    }

    public MobCosmeticProfile getProfile() {
        return profile;
    }

    public ItemStack getItem() {
        return item;
    }

    private LivingEntity getMob() {
        var entity = Bukkit.getEntity(mobUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private ArmorStand getStand() {
        var entity = Bukkit.getEntity(standUuid);
        return entity instanceof ArmorStand stand ? stand : null;
    }
}
