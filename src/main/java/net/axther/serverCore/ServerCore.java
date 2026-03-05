package net.axther.serverCore;

import net.axther.serverCore.command.ServerCoreCommand;
import net.axther.serverCore.config.ServerCoreConfig;
import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.calibrate.CalibrationListener;
import net.axther.serverCore.cosmetic.command.CosmeticCommand;
import net.axther.serverCore.cosmetic.config.CosmeticConfig;
import net.axther.serverCore.cosmetic.data.CosmeticStore;
import net.axther.serverCore.cosmetic.listener.CosmeticLifecycleListener;
import net.axther.serverCore.cosmetic.profiles.PandaCosmeticProfile;
import net.axther.serverCore.cosmetic.task.CosmeticTickTask;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.particle.command.EmitterCommand;
import net.axther.serverCore.particle.config.EmitterConfig;
import net.axther.serverCore.particle.listener.EmitterLifecycleListener;
import net.axther.serverCore.particle.task.EmitterTickTask;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.data.PetStore;
import net.axther.serverCore.pet.model.ModelEngineHook;
import net.axther.serverCore.pet.command.PetCommand;
import net.axther.serverCore.pet.config.PetConfig;
import net.axther.serverCore.pet.listener.PetItemListener;
import net.axther.serverCore.pet.listener.PetLifecycleListener;
import net.axther.serverCore.pet.profiles.RatPetProfile;
import net.axther.serverCore.pet.task.PetTickTask;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.command.HologramCommand;
import net.axther.serverCore.hologram.config.HologramConfig;
import net.axther.serverCore.hologram.listener.HologramLifecycleListener;
import net.axther.serverCore.hologram.task.HologramTickTask;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.command.NPCCommand;
import net.axther.serverCore.npc.config.NPCConfig;
import net.axther.serverCore.npc.listener.NPCListener;
import net.axther.serverCore.npc.render.NPCRenderer;
import net.axther.serverCore.npc.render.NPCViewTracker;
import net.axther.serverCore.npc.task.NPCTickTask;
import net.axther.serverCore.reactive.ReactiveManager;
import net.axther.serverCore.reactive.config.ReactiveConfig;
import net.axther.serverCore.reactive.task.ReactiveTickTask;
import net.axther.serverCore.timeline.TimelineManager;
import net.axther.serverCore.timeline.command.TimelineCommand;
import net.axther.serverCore.timeline.config.TimelineConfig;
import net.axther.serverCore.timeline.task.TimelineTickTask;
import net.axther.serverCore.api.ServerCoreAPI;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.command.QuestCommand;
import net.axther.serverCore.quest.config.QuestConfig;
import net.axther.serverCore.quest.data.QuestStore;
import net.axther.serverCore.quest.listener.QuestListener;
import net.axther.serverCore.gui.MenuListener;
import net.axther.serverCore.gui.MenuManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerCore extends JavaPlugin {

    private ServerCoreConfig serverCoreConfig;
    private CosmeticManager cosmeticManager;
    private CosmeticStore cosmeticStore;
    private CosmeticTickTask tickTask;
    private EmitterManager emitterManager;
    private EmitterConfig emitterConfig;
    private EmitterTickTask emitterTickTask;
    private PetManager petManager;
    private PetStore petStore;
    private PetTickTask petTickTask;
    private HologramManager hologramManager;
    private HologramConfig hologramConfig;
    private HologramTickTask hologramTickTask;
    private NPCManager npcManager;
    private NPCConfig npcConfig;
    private NPCTickTask npcTickTask;
    private NPCListener npcListener;
    private QuestManager questManager;
    private QuestConfig questConfig;
    private QuestStore questStore;
    private TimelineManager timelineManager;
    private TimelineConfig timelineConfig;
    private TimelineTickTask timelineTickTask;
    private ReactiveManager reactiveManager;
    private ReactiveTickTask reactiveTickTask;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        // --- Central Config ---
        serverCoreConfig = new ServerCoreConfig(this);

        // --- ServerCore admin command ---
        PluginCommand scCmd = getCommand("servercore");
        if (scCmd != null) {
            ServerCoreCommand scCommand = new ServerCoreCommand(serverCoreConfig);
            scCmd.setExecutor(scCommand);
            scCmd.setTabCompleter(scCommand);
        }

        // --- GUI Framework ---
        if (serverCoreConfig.isSystemEnabled("gui")) {
            menuManager = new MenuManager();
            getServer().getPluginManager().registerEvents(new MenuListener(menuManager), this);
        }

        // --- Cosmetic System ---
        if (serverCoreConfig.isSystemEnabled("cosmetics")) {
            cosmeticManager = new CosmeticManager();

            // Register Java profiles first (these take priority over config)
            cosmeticManager.registerProfile(EntityType.PANDA, new PandaCosmeticProfile());

            // Load config profiles (skips types already registered by Java)
            CosmeticConfig cosmeticConfig = new CosmeticConfig(this);
            cosmeticConfig.loadAndRegister(cosmeticManager);

            // Set up persistence
            cosmeticStore = new CosmeticStore(this);
            cosmeticManager.setStore(cosmeticStore);
            cosmeticStore.load(cosmeticManager);

            // Register event listeners
            CalibrationListener calibrationListener = new CalibrationListener();
            getServer().getPluginManager().registerEvents(new CosmeticLifecycleListener(cosmeticManager, cosmeticStore), this);
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
        }

        // --- Particle Emitter System ---
        if (serverCoreConfig.isSystemEnabled("emitters")) {
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
        }

        // --- Pet System ---
        if (serverCoreConfig.isSystemEnabled("pets")) {
            boolean megEnabled = getServer().getPluginManager().getPlugin("ModelEngine") != null;
            if (megEnabled) {
                getLogger().info("Model Engine detected -- pet models enabled");
            }
            petManager = new PetManager(megEnabled);

            // Register Java profiles first (take priority over config)
            registerJavaPetProfiles();

            // Load config profiles
            PetConfig petConfig = new PetConfig(this);
            petConfig.loadAndRegister(petManager);

            // Set up pet persistence
            petStore = new PetStore(this);
            petManager.setStore(petStore);
            petStore.load();

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
        }

        // --- Hologram System ---
        if (serverCoreConfig.isSystemEnabled("holograms")) {
            hologramManager = new HologramManager();
            hologramConfig = new HologramConfig(this);
            hologramConfig.loadAll(hologramManager);

            getServer().getPluginManager().registerEvents(new HologramLifecycleListener(hologramManager), this);

            PluginCommand hologramCmd = getCommand("hologram");
            if (hologramCmd != null) {
                HologramCommand hologramCommand = new HologramCommand(hologramManager, hologramConfig);
                hologramCmd.setExecutor(hologramCommand);
                hologramCmd.setTabCompleter(hologramCommand);
            }

            hologramTickTask = new HologramTickTask(hologramManager);
            hologramTickTask.runTaskTimer(this, 0L, 1L);

            hologramManager.spawnAll();
        }

        // --- NPC / Dialogue System ---
        if (serverCoreConfig.isSystemEnabled("npcs")) {
            boolean packetEventsPresent = getServer().getPluginManager().getPlugin("packetevents") != null;
            if (!packetEventsPresent) {
                getLogger().warning("PacketEvents not found -- NPC system disabled. Install PacketEvents to enable NPCs.");
            } else {
                npcManager = new NPCManager();
                npcConfig = new NPCConfig(this);

                // Quest system must init before NPC config load (for inline quests)
                questManager = new QuestManager();

                npcConfig.loadAll(npcManager, questManager);

                npcListener = new NPCListener(this, npcManager, npcConfig);
                getServer().getPluginManager().registerEvents(npcListener, this);

                int viewDistance = serverCoreConfig.getNpcViewDistance();
                initNpcPacketSystem(viewDistance);

                PluginCommand npcCmd = getCommand("npc");
                if (npcCmd != null) {
                    NPCCommand npcCommand = new NPCCommand(npcManager, npcConfig, npcListener);
                    npcCmd.setExecutor(npcCommand);
                    npcCmd.setTabCompleter(npcCommand);
                }

                NPCViewTracker viewTracker = npcManager.getViewTracker();
                npcTickTask = new NPCTickTask(npcManager, viewTracker);
                npcTickTask.runTaskTimer(this, 0L, 1L);

                getLogger().info("NPC system enabled with PacketEvents (view distance: " + viewDistance + " blocks)");
            }
        }

        // --- Quest System ---
        if (serverCoreConfig.isSystemEnabled("quests")) {
            if (questManager == null) {
                questManager = new QuestManager();
            }

            // Load standalone quest files
            questConfig = new QuestConfig(this);
            questConfig.loadAll(questManager);

            // Load inline NPC quests if NPC system is active
            if (npcConfig != null) {
                // Inline NPC quests are already loaded during npcConfig.loadAll()
            }

            questStore = new QuestStore(this);
            questManager.setStore(questStore);
            questStore.load(questManager);

            // Read quest config values
            int maxActive = getConfig().getInt("systems.quests.max-active-quests", 0);
            boolean actionBar = getConfig().getBoolean("systems.quests.action-bar-progress", true);
            questManager.setMaxActiveQuests(maxActive);

            QuestListener questListener = new QuestListener(questManager);
            questListener.setActionBarEnabled(actionBar);
            getServer().getPluginManager().registerEvents(questListener, this);

            PluginCommand questCmd = getCommand("quest");
            if (questCmd != null) {
                QuestCommand questCommand = new QuestCommand(questManager, questConfig);
                questCmd.setExecutor(questCommand);
                questCmd.setTabCompleter(questCommand);
            }

            // Set up Vault (soft dependency)
            boolean vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
            if (vaultPresent) {
                setupVault();
            }

            getLogger().info("Quest system loaded with " + questManager.getAllQuests().size() + " quests");
        }

        // Wire pet manager for PET quest rewards
        if (petManager != null && questManager != null) {
            questManager.setPetManager(petManager);
        }

        // --- Timeline / Event Sequencer System ---
        if (serverCoreConfig.isSystemEnabled("timelines")) {
            timelineManager = new TimelineManager();
            timelineConfig = new TimelineConfig(this);
            timelineConfig.loadAll(timelineManager);

            PluginCommand timelineCmd = getCommand("timeline");
            if (timelineCmd != null) {
                TimelineCommand timelineCommand = new TimelineCommand(timelineManager, timelineConfig);
                timelineCmd.setExecutor(timelineCommand);
                timelineCmd.setTabCompleter(timelineCommand);
            }

            timelineTickTask = new TimelineTickTask(timelineManager);
            timelineTickTask.runTaskTimer(this, 0L, 1L);
        }

        // --- Reactive / Context-Aware Cosmetics System ---
        if (serverCoreConfig.isSystemEnabled("reactive")) {
            reactiveManager = new ReactiveManager();
            ReactiveConfig reactiveConfig = new ReactiveConfig(this);
            reactiveConfig.loadAll(reactiveManager);

            reactiveTickTask = new ReactiveTickTask(reactiveManager, cosmeticManager, petManager);
            reactiveTickTask.runTaskTimer(this, 0L, 20L);
        }

        // --- PlaceholderAPI Hook ---
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            registerPlaceholderHook();
            getLogger().info("PlaceholderAPI detected -- placeholders registered");
        }

        // --- Public API ---
        ServerCoreAPI.init(cosmeticManager, emitterManager, petManager,
                hologramManager, npcManager, timelineManager, reactiveManager, menuManager,
                questManager);

        // Build startup summary
        StringBuilder summary = new StringBuilder("ServerCore enabled");
        if (cosmeticManager != null) {
            summary.append(" - cosmetic system loaded with ")
                    .append(cosmeticManager.getSupportedTypes().size())
                    .append(" mob profiles");
        }
        if (petManager != null) {
            summary.append(", pet system loaded with ")
                    .append(petManager.getRegisteredPetIds().size())
                    .append(" pet types");
        }
        getLogger().info(summary.toString());
    }

    /**
     * Registers the PlaceholderAPI expansion. Isolated into its own method so that the
     * PlaceholderHook class is never loaded unless PlaceholderAPI is actually present,
     * avoiding NoClassDefFoundError on servers without PlaceholderAPI.
     */
    private void registerPlaceholderHook() {
        new net.axther.serverCore.hook.PlaceholderHook(
                this,
                petManager,
                cosmeticManager,
                emitterManager,
                hologramManager,
                questManager
        ).register();
    }

    /**
     * Initializes the PacketEvents-based NPC rendering system.
     * Isolated into its own method so PacketEvents classes are never loaded
     * unless the plugin is actually present, avoiding NoClassDefFoundError.
     */
    private void initNpcPacketSystem(int viewDistance) {
        NPCRenderer renderer = new NPCRenderer(this);
        NPCViewTracker viewTracker = new NPCViewTracker(npcManager, renderer, viewDistance);
        npcManager.init(renderer, viewTracker);

        // Register the packet listener for NPC interactions
        net.axther.serverCore.npc.listener.NPCPacketListener packetListener =
                new net.axther.serverCore.npc.listener.NPCPacketListener(this, npcManager, npcListener);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    private void registerJavaPetProfiles() {
        petManager.registerProfile(new RatPetProfile());
    }

    private void setupVault() {
        boolean econ = net.axther.serverCore.hook.VaultHook.setupEconomy();
        boolean perm = net.axther.serverCore.hook.VaultHook.setupPermissions();
        if (econ) getLogger().info("Vault economy hooked for quest rewards");
        if (perm) getLogger().info("Vault permissions hooked for quest rewards");
    }

    @Override
    public void onDisable() {
        ServerCoreAPI.shutdown();
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (cosmeticStore != null && cosmeticManager != null) {
            cosmeticStore.save(cosmeticManager);
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
        if (petStore != null) {
            petStore.save();
        }
        if (petTickTask != null) {
            petTickTask.cancel();
        }
        if (petManager != null) {
            petManager.destroyAll();
        }
        if (hologramConfig != null && hologramManager != null) {
            hologramConfig.saveAll(hologramManager);
        }
        if (hologramTickTask != null) {
            hologramTickTask.cancel();
        }
        if (hologramManager != null) {
            hologramManager.destroyAll();
        }
        if (questStore != null && questManager != null) {
            questStore.save(questManager);
        }
        if (npcTickTask != null) {
            npcTickTask.cancel();
        }
        if (npcListener != null) {
            npcListener.clearAllSessions();
        }
        if (npcManager != null) {
            npcManager.destroyAll();
        }
        if (timelineTickTask != null) {
            timelineTickTask.cancel();
        }
        if (timelineManager != null) {
            timelineManager.stopAll();
        }
        if (reactiveTickTask != null) {
            reactiveTickTask.cancel();
        }
        if (reactiveManager != null) {
            reactiveManager.clearAll(cosmeticManager, petManager);
        }
    }
}
