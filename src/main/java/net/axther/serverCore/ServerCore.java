package net.axther.serverCore;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.calibrate.CalibrationListener;
import net.axther.serverCore.cosmetic.command.CosmeticCommand;
import net.axther.serverCore.cosmetic.config.CosmeticConfig;
import net.axther.serverCore.cosmetic.listener.CosmeticLifecycleListener;
import net.axther.serverCore.cosmetic.profiles.PandaCosmeticProfile;
import net.axther.serverCore.cosmetic.task.CosmeticTickTask;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.particle.command.EmitterCommand;
import net.axther.serverCore.particle.config.EmitterConfig;
import net.axther.serverCore.particle.listener.EmitterLifecycleListener;
import net.axther.serverCore.particle.task.EmitterTickTask;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.model.ModelEngineHook;
import net.axther.serverCore.pet.command.PetCommand;
import net.axther.serverCore.pet.config.PetConfig;
import net.axther.serverCore.pet.listener.PetItemListener;
import net.axther.serverCore.pet.listener.PetLifecycleListener;
import net.axther.serverCore.pet.profiles.RatPetProfile;
import net.axther.serverCore.pet.task.PetTickTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerCore extends JavaPlugin {

    private CosmeticManager cosmeticManager;
    private CosmeticTickTask tickTask;
    private EmitterManager emitterManager;
    private EmitterConfig emitterConfig;
    private EmitterTickTask emitterTickTask;
    private PetManager petManager;
    private PetTickTask petTickTask;

    @Override
    public void onEnable() {
        cosmeticManager = new CosmeticManager();

        // Register Java profiles first (these take priority over config)
        cosmeticManager.registerProfile(EntityType.PANDA, new PandaCosmeticProfile());

        // Load config profiles (skips types already registered by Java)
        CosmeticConfig cosmeticConfig = new CosmeticConfig(this);
        cosmeticConfig.loadAndRegister(cosmeticManager);

        // Register event listeners
        CalibrationListener calibrationListener = new CalibrationListener();
        getServer().getPluginManager().registerEvents(new CosmeticLifecycleListener(cosmeticManager), this);
        getServer().getPluginManager().registerEvents(calibrationListener, this);

        // Register command
        PluginCommand cmd = getCommand("cosmetic");
        if (cmd != null) {
            CosmeticCommand cosmeticCommand = new CosmeticCommand(cosmeticManager, cosmeticConfig, calibrationListener);
            cmd.setExecutor(cosmeticCommand);
            cmd.setTabCompleter(cosmeticCommand);
        }

        // Start tick task (runs every tick)
        tickTask = new CosmeticTickTask(cosmeticManager);
        tickTask.runTaskTimer(this, 0L, 1L);

        // --- Particle Emitter System ---
        emitterManager = new EmitterManager();
        emitterConfig = new EmitterConfig(this);
        emitterConfig.loadAll(emitterManager);

        getServer().getPluginManager().registerEvents(new EmitterLifecycleListener(emitterManager), this);

        PluginCommand emitterCmd = getCommand("emitter");
        if (emitterCmd != null) {
            EmitterCommand emitterCommand = new EmitterCommand(emitterManager, emitterConfig);
            emitterCmd.setExecutor(emitterCommand);
            emitterCmd.setTabCompleter(emitterCommand);
        }

        emitterTickTask = new EmitterTickTask(emitterManager);
        emitterTickTask.runTaskTimer(this, 0L, 1L);

        // --- Pet System ---
        boolean megEnabled = getServer().getPluginManager().getPlugin("ModelEngine") != null;
        if (megEnabled) {
            getLogger().info("Model Engine detected — pet models enabled");
        }
        petManager = new PetManager(megEnabled);

        // Register Java profiles first (take priority over config)
        registerJavaPetProfiles();

        // Load config profiles
        PetConfig petConfig = new PetConfig(this);
        petConfig.loadAndRegister(petManager);

        getServer().getPluginManager().registerEvents(new PetLifecycleListener(petManager), this);
        getServer().getPluginManager().registerEvents(new PetItemListener(petManager), this);

        PluginCommand petCmd = getCommand("pet");
        if (petCmd != null) {
            PetCommand petCommand = new PetCommand(petManager, petConfig, this::registerJavaPetProfiles);
            petCmd.setExecutor(petCommand);
            petCmd.setTabCompleter(petCommand);
        }

        petTickTask = new PetTickTask(petManager);
        petTickTask.runTaskTimer(this, 0L, 1L);

        getLogger().info("ServerCore enabled - cosmetic system loaded with " + cosmeticManager.getSupportedTypes().size() + " mob profiles, pet system loaded with " + petManager.getRegisteredPetIds().size() + " pet types");
    }

    private void registerJavaPetProfiles() {
        petManager.registerProfile(new RatPetProfile());
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (cosmeticManager != null) {
            cosmeticManager.destroyAll();
        }
        if (emitterConfig != null && emitterManager != null) {
            emitterConfig.saveAll(emitterManager);
        }
        if (emitterTickTask != null) {
            emitterTickTask.cancel();
        }
        if (emitterManager != null) {
            emitterManager.destroyAll();
        }
        if (petTickTask != null) {
            petTickTask.cancel();
        }
        if (petManager != null) {
            petManager.destroyAll();
        }
    }
}
