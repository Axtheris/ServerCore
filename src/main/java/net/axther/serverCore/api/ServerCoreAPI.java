package net.axther.serverCore.api;

import net.axther.serverCore.api.builder.EmitterBuilder;
import net.axther.serverCore.api.builder.HologramBuilder;
import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.gui.Menu;
import net.axther.serverCore.gui.MenuConfig;
import net.axther.serverCore.gui.MenuManager;
import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.pet.PetInstance;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.gui.QuestGUI;
import net.axther.serverCore.reactive.ReactiveManager;
import net.axther.serverCore.timeline.TimelineManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

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
    private final QuestManager questManager;
    private final MenuConfig menuConfig;

    private ServerCoreAPI(CosmeticManager cosmeticManager,
                          EmitterManager emitterManager,
                          PetManager petManager,
                          HologramManager hologramManager,
                          NPCManager npcManager,
                          TimelineManager timelineManager,
                          ReactiveManager reactiveManager,
                          MenuManager menuManager,
                          QuestManager questManager,
                          MenuConfig menuConfig) {
        this.cosmeticManager = cosmeticManager;
        this.emitterManager = emitterManager;
        this.petManager = petManager;
        this.hologramManager = hologramManager;
        this.npcManager = npcManager;
        this.timelineManager = timelineManager;
        this.reactiveManager = reactiveManager;
        this.menuManager = menuManager;
        this.questManager = questManager;
        this.menuConfig = menuConfig;
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
                            MenuManager menuManager,
                            QuestManager questManager,
                            MenuConfig menuConfig) {
        instance = new ServerCoreAPI(cosmeticManager, emitterManager, petManager,
                hologramManager, npcManager, timelineManager, reactiveManager, menuManager,
                questManager, menuConfig);
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

    public QuestManager getQuestManager() {
        return questManager;
    }

    // ── Fluent builders ──────────────────────────────────────────────

    public HologramBuilder hologram(String id) {
        return new HologramBuilder(id, hologramManager);
    }

    public EmitterBuilder emitter(String id) {
        return new EmitterBuilder(id, emitterManager);
    }

    public Menu.Builder menu(String title) {
        return Menu.builder(title);
    }

    // ── Quest convenience ───────────────────────────────────────────────

    /**
     * Opens the quest journal GUI for the given player.
     */
    public void openQuestJournal(Player player) {
        if (questManager == null) return;
        new QuestGUI(questManager).openJournal(player);
    }

    /**
     * Returns whether a specific quest is currently active for the player.
     */
    public boolean isQuestActive(Player player, String questId) {
        return questManager != null && questManager.isActive(player.getUniqueId(), questId);
    }

    /**
     * Returns the objective progress array for a player's active quest,
     * or an empty array if the quest is not active.
     */
    public int[] getQuestProgress(Player player, String questId) {
        if (questManager == null) return new int[0];
        var progress = questManager.getActiveQuests(player.getUniqueId()).stream()
                .filter(p -> p.getQuestId().equals(questId)).findFirst().orElse(null);
        return progress != null ? progress.getObjectiveProgress() : new int[0];
    }

    // ── Hologram convenience ────────────────────────────────────────────

    /**
     * Makes a hologram visible to a specific player (reverses a previous hide).
     */
    public void showHologramTo(Player player, String hologramId) {
        if (hologramManager == null) return;
        Hologram hologram = hologramManager.get(hologramId);
        if (hologram == null || !hologram.isSpawned() || hologram.getEntityUuid() == null) return;
        Entity entity = Bukkit.getEntity(hologram.getEntityUuid());
        if (entity != null) {
            player.showEntity(Bukkit.getPluginManager().getPlugin("ServerCore"), entity);
        }
    }

    /**
     * Hides a hologram from a specific player.
     */
    public void hideHologramFrom(Player player, String hologramId) {
        if (hologramManager == null) return;
        Hologram hologram = hologramManager.get(hologramId);
        if (hologram == null || !hologram.isSpawned() || hologram.getEntityUuid() == null) return;
        Entity entity = Bukkit.getEntity(hologram.getEntityUuid());
        if (entity != null) {
            player.hideEntity(Bukkit.getPluginManager().getPlugin("ServerCore"), entity);
        }
    }

    // ── Menu convenience ────────────────────────────────────────────────

    /**
     * Opens a config-defined menu for the given player.
     */
    public void openMenu(Player player, String menuId) {
        if (menuConfig == null) return;
        Menu menu = menuConfig.buildMenu(menuId);
        if (menu != null) menu.open(player);
    }

    // ── Pet convenience ─────────────────────────────────────────────────

    /**
     * Returns whether the player currently has any pet summoned.
     */
    public boolean hasPetSummoned(Player player) {
        return petManager != null && petManager.hasPets(player.getUniqueId());
    }

    /**
     * Returns the profile ID of the player's first active pet, or null if none.
     */
    public String getPetName(Player player) {
        if (petManager == null) return null;
        List<PetInstance> pets = petManager.getPets(player.getUniqueId());
        if (pets.isEmpty()) return null;
        return pets.get(0).getProfile().getId();
    }
}
