package net.axther.serverCore.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.pet.PetInstance;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.data.PetStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PlaceholderAPI expansion for ServerCore.
 * <p>
 * This class is only loaded when PlaceholderAPI is present on the server.
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final PetManager petManager;
    private final CosmeticManager cosmeticManager;
    private final EmitterManager emitterManager;
    private final HologramManager hologramManager;

    public PlaceholderHook(JavaPlugin plugin,
                           PetManager petManager,
                           CosmeticManager cosmeticManager,
                           EmitterManager emitterManager,
                           HologramManager hologramManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.cosmeticManager = cosmeticManager;
        this.emitterManager = emitterManager;
        this.hologramManager = hologramManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "servercore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Axther";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        // Keep the expansion loaded across PlaceholderAPI reloads
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "pet_name": {
                List<PetInstance> pets = petManager.getPets(player.getUniqueId());
                if (pets.isEmpty()) {
                    return "None";
                }
                return pets.getFirst().getProfile().getDisplayName();
            }
            case "pet_type": {
                List<PetInstance> pets = petManager.getPets(player.getUniqueId());
                if (pets.isEmpty()) {
                    return "none";
                }
                return pets.getFirst().getProfile().getId();
            }
            case "pet_count": {
                PetStore store = petManager.getStore();
                if (store == null) {
                    return "0";
                }
                return String.valueOf(store.getOwnedPets(player.getUniqueId()).size());
            }
            case "cosmetic_count": {
                return String.valueOf(cosmeticManager.getActiveCosmetics().values().stream()
                        .mapToInt(List::size)
                        .sum());
            }
            case "emitter_count": {
                return String.valueOf(emitterManager.getAllEmitters().size());
            }
            case "hologram_count": {
                return String.valueOf(hologramManager.getAll().size());
            }
            default:
                return null;
        }
    }
}
