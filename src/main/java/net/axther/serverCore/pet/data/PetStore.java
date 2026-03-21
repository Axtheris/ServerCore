package net.axther.serverCore.pet.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class PetStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Set<String>> ownedPets = new HashMap<>();

    // PERS-01: Dirty flag — set on mutation, cleared before flush dispatch.
    private boolean dirty = false;
    // PERS-02: volatile so async write thread can clear it without a memory barrier issue.
    private volatile boolean saving = false;

    public PetStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pet-data.yml");
    }

    // -------------------------------------------------------------------------
    // Dirty-flag API
    // -------------------------------------------------------------------------

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    // -------------------------------------------------------------------------
    // Snapshot helpers
    // -------------------------------------------------------------------------

    /**
     * PERS-02: snapshot is built on main thread (safe to access live collections).
     * Copies the ownedPets map so the async thread only touches detached data.
     */
    private YamlConfiguration buildSnapshot() {
        YamlConfiguration config = new YamlConfiguration();

        for (var entry : ownedPets.entrySet()) {
            UUID playerUuid = entry.getKey();
            Set<String> pets = entry.getValue();
            if (pets.isEmpty()) continue;

            config.set("players." + playerUuid.toString() + ".owned", new ArrayList<>(pets));
        }

        return config;
    }

    // -------------------------------------------------------------------------
    // Write helpers
    // -------------------------------------------------------------------------

    /**
     * PERS-02: writeSnapshot runs on async thread — only touches the detached YamlConfiguration.
     * YamlConfiguration is read-only after buildSnapshot() returns — no concurrent access.
     * Uses atomic file swap for crash safety (PERS-03).
     */
    private void writeSnapshot(YamlConfiguration snapshot) {
        try {
            Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
            snapshot.save(tmp.toFile());
            try {
                Files.move(tmp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save pet data", e);
        } finally {
            saving = false;
        }
    }

    /**
     * Synchronous write — same atomic swap logic as writeSnapshot but without the saving flag
     * management. Only called from onDisable on the main thread when no async write is in flight.
     */
    private void writeSnapshotSync(YamlConfiguration snapshot) {
        try {
            Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
            snapshot.save(tmp.toFile());
            try {
                Files.move(tmp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save pet data", e);
        }
    }

    // -------------------------------------------------------------------------
    // Public persistence API
    // -------------------------------------------------------------------------

    /**
     * PERS-01: Flushes dirty state to disk via snapshot-then-async write.
     * No-op if store is clean or an async write is already in flight.
     */
    public void flushIfDirty() {
        if (!dirty) return;
        if (saving) return;  // async write in flight, skip (PERS-02)
        saving = true;
        dirty = false;  // clear BEFORE dispatch — mutations after snapshot set dirty again (correct)
        YamlConfiguration snapshot = buildSnapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeSnapshot(snapshot));
    }

    /**
     * PERS-01 / onDisable: Synchronous save — guaranteed to complete before process exits.
     */
    public void saveSync() {
        dirty = false;
        YamlConfiguration snapshot = buildSnapshot();
        writeSnapshotSync(snapshot);
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    public void load() {
        ownedPets.clear();

        // PERS-03: Clean up any .tmp file left by a crashed async write.
        Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
        if (Files.exists(tmp)) {
            if (!file.exists()) {
                try {
                    Files.move(tmp, file.toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to recover pet data from .tmp file: " + e.getMessage());
                }
            } else {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to delete orphaned pet .tmp file: " + e.getMessage());
                }
            }
        }

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

    // -------------------------------------------------------------------------
    // Mutation methods — each call markDirty() internally (PERS-01 / D-03)
    // -------------------------------------------------------------------------

    public void addPet(UUID playerUuid, String petId) {
        ownedPets.computeIfAbsent(playerUuid, k -> new LinkedHashSet<>()).add(petId.toLowerCase());
        markDirty();
    }

    public void removePet(UUID playerUuid, String petId) {
        Set<String> pets = ownedPets.get(playerUuid);
        if (pets != null) {
            pets.remove(petId.toLowerCase());
            if (pets.isEmpty()) {
                ownedPets.remove(playerUuid);
            }
            markDirty();
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
