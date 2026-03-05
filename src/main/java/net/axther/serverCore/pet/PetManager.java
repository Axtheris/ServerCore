package net.axther.serverCore.pet;

import net.axther.serverCore.api.event.PetSummonEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import net.axther.serverCore.pet.data.PetStore;
import net.axther.serverCore.pet.model.ModelEngineHook;

import java.util.*;

public class PetManager {

    private final boolean modelEngineEnabled;
    private final Map<String, PetProfile> profiles = new LinkedHashMap<>();
    private final Map<UUID, List<PetInstance>> activePets = new HashMap<>();
    private final Map<UUID, PetInstance> standIndex = new HashMap<>();
    private PetStore store;

    public PetManager(boolean modelEngineEnabled) {
        this.modelEngineEnabled = modelEngineEnabled;
    }

    public void setStore(PetStore store) {
        this.store = store;
    }

    public PetStore getStore() {
        return store;
    }

    public void registerProfile(PetProfile profile) {
        profiles.put(profile.getId().toLowerCase(), profile);
    }

    public PetProfile getProfile(String id) {
        return profiles.get(id.toLowerCase());
    }

    public boolean hasProfile(String id) {
        return profiles.containsKey(id.toLowerCase());
    }

    public PetInstance summonPet(Player player, PetProfile profile) {
        PetSummonEvent event = new PetSummonEvent(player, profile);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        Location spawnLoc = player.getLocation().add(0, profile.getHoverHeight(), 0);
        boolean useModelEngine = modelEngineEnabled && profile.getModelId() != null;

        ArmorStand stand = player.getWorld().spawn(spawnLoc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setSilent(true);
            s.setPersistent(false);
            s.setCanPickupItems(false);
            s.setSmall(profile.useSmallStand());
            if (!useModelEngine) {
                s.setItem(EquipmentSlot.HEAD, profile.getHeadItem().clone());
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                s.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
        });

        PetInstance instance = new PetInstance(player.getUniqueId(), stand.getUniqueId(), profile);

        if (useModelEngine) {
            boolean applied = ModelEngineHook.applyModel(stand, profile.getModelId());
            if (applied) {
                instance.setUsingModelEngine(true);
            } else {
                // Fallback to head item if model not found
                stand.setItem(EquipmentSlot.HEAD, profile.getHeadItem().clone());
            }
        }

        activePets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(instance);
        standIndex.put(stand.getUniqueId(), instance);

        return instance;
    }

    public void dismissPet(Player player) {
        List<PetInstance> instances = activePets.remove(player.getUniqueId());
        if (instances == null) return;

        for (PetInstance instance : instances) {
            standIndex.remove(instance.getStandUuid());
            instance.destroy();
        }
    }

    public void dismissAll(UUID playerUuid) {
        List<PetInstance> instances = activePets.remove(playerUuid);
        if (instances == null) return;

        for (PetInstance instance : instances) {
            standIndex.remove(instance.getStandUuid());
            instance.destroy();
        }
    }

    public void tickAll() {
        var iterator = activePets.entrySet().iterator();
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
        for (List<PetInstance> instances : activePets.values()) {
            for (PetInstance instance : instances) {
                instance.destroy();
            }
        }
        activePets.clear();
        standIndex.clear();
    }

    public boolean isPetStand(UUID standUuid) {
        return standIndex.containsKey(standUuid);
    }

    public PetInstance getPetByStand(UUID standUuid) {
        return standIndex.get(standUuid);
    }

    public List<PetInstance> getPets(UUID playerUuid) {
        return activePets.getOrDefault(playerUuid, List.of());
    }

    public boolean hasPets(UUID playerUuid) {
        return activePets.containsKey(playerUuid);
    }

    public boolean hasPetType(UUID playerUuid, String petId) {
        List<PetInstance> pets = activePets.get(playerUuid);
        if (pets == null) return false;
        return pets.stream().anyMatch(p -> p.getProfile().getId().equalsIgnoreCase(petId));
    }

    public void dismissPetType(UUID playerUuid, String petId) {
        List<PetInstance> pets = activePets.get(playerUuid);
        if (pets == null) return;
        pets.removeIf(instance -> {
            if (instance.getProfile().getId().equalsIgnoreCase(petId)) {
                standIndex.remove(instance.getStandUuid());
                instance.destroy();
                return true;
            }
            return false;
        });
        if (pets.isEmpty()) {
            activePets.remove(playerUuid);
        }
    }

    public void clearProfiles() {
        profiles.clear();
    }

    public Set<String> getRegisteredPetIds() {
        return Collections.unmodifiableSet(profiles.keySet());
    }

    public void removeStandFromIndex(UUID standUuid) {
        PetInstance instance = standIndex.remove(standUuid);
        if (instance != null) {
            List<PetInstance> list = activePets.get(instance.getOwnerUuid());
            if (list != null) {
                list.remove(instance);
                if (list.isEmpty()) {
                    activePets.remove(instance.getOwnerUuid());
                }
            }
        }
    }
}
