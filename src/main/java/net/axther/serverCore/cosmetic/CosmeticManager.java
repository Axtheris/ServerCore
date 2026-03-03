package net.axther.serverCore.cosmetic;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CosmeticManager {

    private final Map<EntityType, MobCosmeticProfile> profiles = new EnumMap<>(EntityType.class);
    private final Map<UUID, List<CosmeticInstance>> activeCosmetics = new HashMap<>();
    private final Map<UUID, CosmeticInstance> standIndex = new HashMap<>();

    public void registerProfile(EntityType type, MobCosmeticProfile profile) {
        profiles.put(type, profile);
    }

    public MobCosmeticProfile getProfile(EntityType type) {
        return profiles.get(type);
    }

    public boolean applyCosmetic(LivingEntity mob, ItemStack item) {
        MobCosmeticProfile profile = profiles.get(mob.getType());
        if (profile == null) {
            return false;
        }

        Location standLoc = profile.computeStandLocation(mob);
        ArmorStand stand = mob.getWorld().spawn(standLoc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setSilent(true);
            s.setPersistent(false);
            s.setCanPickupItems(false);
            s.setSmall(profile.useSmallStand());
            s.setItem(EquipmentSlot.HEAD, item.clone());
            // Lock all equipment slots to prevent interaction
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                s.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
        });

        CosmeticInstance instance = new CosmeticInstance(mob.getUniqueId(), stand.getUniqueId(), profile, item.clone());

        activeCosmetics.computeIfAbsent(mob.getUniqueId(), k -> new ArrayList<>()).add(instance);
        standIndex.put(stand.getUniqueId(), instance);

        return true;
    }

    public void removeCosmetics(UUID mobUuid) {
        List<CosmeticInstance> instances = activeCosmetics.remove(mobUuid);
        if (instances == null) return;

        for (CosmeticInstance instance : instances) {
            standIndex.remove(instance.getStandUuid());
            instance.destroy();
        }
    }

    public void tickAll() {
        var iterator = activeCosmetics.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var instances = entry.getValue();
            instances.removeIf(instance -> {
                if (!instance.tick()) {
                    standIndex.remove(instance.getStandUuid());
                    return true;
                }
                return false;
            });
            if (instances.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public void destroyAll() {
        for (List<CosmeticInstance> instances : activeCosmetics.values()) {
            for (CosmeticInstance instance : instances) {
                instance.destroy();
            }
        }
        activeCosmetics.clear();
        standIndex.clear();
    }

    public boolean isCosmeticStand(UUID standUuid) {
        return standIndex.containsKey(standUuid);
    }

    public boolean hasCosmetics(UUID mobUuid) {
        return activeCosmetics.containsKey(mobUuid);
    }

    public Set<EntityType> getSupportedTypes() {
        return Collections.unmodifiableSet(profiles.keySet());
    }
}
