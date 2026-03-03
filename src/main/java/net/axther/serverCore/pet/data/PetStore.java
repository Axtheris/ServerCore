package net.axther.serverCore.pet.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PetStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Set<String>> ownedPets = new HashMap<>();

    public PetStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pet-data.yml");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();

        for (var entry : ownedPets.entrySet()) {
            UUID playerUuid = entry.getKey();
            Set<String> pets = entry.getValue();
            if (pets.isEmpty()) continue;

            config.set("players." + playerUuid.toString() + ".owned", new ArrayList<>(pets));
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save pet data", e);
        }
    }

    public void load() {
        ownedPets.clear();
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID playerUuid;
            try {
                playerUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in pet data: " + uuidStr);
                continue;
            }

            List<String> owned = players.getStringList(uuidStr + ".owned");
            if (!owned.isEmpty()) {
                ownedPets.put(playerUuid, new LinkedHashSet<>(owned));
            }
        }
    }

    public void addPet(UUID playerUuid, String petId) {
        ownedPets.computeIfAbsent(playerUuid, k -> new LinkedHashSet<>()).add(petId.toLowerCase());
    }

    public void removePet(UUID playerUuid, String petId) {
        Set<String> pets = ownedPets.get(playerUuid);
        if (pets != null) {
            pets.remove(petId.toLowerCase());
            if (pets.isEmpty()) {
                ownedPets.remove(playerUuid);
            }
        }
    }

    public boolean ownsPet(UUID playerUuid, String petId) {
        Set<String> pets = ownedPets.get(playerUuid);
        return pets != null && pets.contains(petId.toLowerCase());
    }

    public Set<String> getOwnedPets(UUID playerUuid) {
        Set<String> pets = ownedPets.get(playerUuid);
        return pets != null ? Collections.unmodifiableSet(pets) : Set.of();
    }
}
