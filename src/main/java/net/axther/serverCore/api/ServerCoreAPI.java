package net.axther.serverCore.api;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.gui.MenuManager;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.reactive.ReactiveManager;
import net.axther.serverCore.timeline.TimelineManager;

/**
 * Static accessor to all ServerCore managers.
 * Provides a stable public API for other plugins to interact with ServerCore systems.
 */
public final class ServerCoreAPI {

    private static ServerCoreAPI instance;

    private final CosmeticManager cosmeticManager;
    private final EmitterManager emitterManager;
    private final PetManager petManager;
    private final HologramManager hologramManager;
    private final NPCManager npcManager;
    private final TimelineManager timelineManager;
    private final ReactiveManager reactiveManager;
    private final MenuManager menuManager;

    private ServerCoreAPI(CosmeticManager cosmeticManager,
                          EmitterManager emitterManager,
                          PetManager petManager,
                          HologramManager hologramManager,
                          NPCManager npcManager,
                          TimelineManager timelineManager,
                          ReactiveManager reactiveManager,
                          MenuManager menuManager) {
        this.cosmeticManager = cosmeticManager;
        this.emitterManager = emitterManager;
        this.petManager = petManager;
        this.hologramManager = hologramManager;
        this.npcManager = npcManager;
        this.timelineManager = timelineManager;
        this.reactiveManager = reactiveManager;
        this.menuManager = menuManager;
    }

    /**
     * Returns the ServerCoreAPI instance.
     *
     * @return the API instance
     * @throws IllegalStateException if ServerCore has not been enabled yet
     */
    public static ServerCoreAPI get() {
        if (instance == null) {
            throw new IllegalStateException("ServerCoreAPI is not initialized. Is the ServerCore plugin enabled?");
        }
        return instance;
    }

    /**
     * Initializes the API. Called internally by ServerCore during onEnable().
     */
    public static void init(CosmeticManager cosmeticManager,
                            EmitterManager emitterManager,
                            PetManager petManager,
                            HologramManager hologramManager,
                            NPCManager npcManager,
                            TimelineManager timelineManager,
                            ReactiveManager reactiveManager,
                            MenuManager menuManager) {
        instance = new ServerCoreAPI(cosmeticManager, emitterManager, petManager,
                hologramManager, npcManager, timelineManager, reactiveManager, menuManager);
    }

    /**
     * Shuts down the API. Called internally by ServerCore during onDisable().
     */
    public static void shutdown() {
        instance = null;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public EmitterManager getEmitterManager() {
        return emitterManager;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public TimelineManager getTimelineManager() {
        return timelineManager;
    }

    public ReactiveManager getReactiveManager() {
        return reactiveManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}
