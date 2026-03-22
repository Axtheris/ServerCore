package net.axther.serverCore.command;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.data.CosmeticStore;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.data.PetStore;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.data.QuestStore;
import net.axther.serverCore.timeline.TimelineManager;
import net.axther.serverCore.reactive.ReactiveManager;
import net.axther.serverCore.gui.MenuManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Bundles live references for the /servercore debug command.
 * All fields are nullable — null means the system is disabled.
 */
public record DebugContext(
    // Managers (null if system disabled)
    CosmeticManager cosmeticManager,
    EmitterManager emitterManager,
    PetManager petManager,
    HologramManager hologramManager,
    NPCManager npcManager,
    QuestManager questManager,
    TimelineManager timelineManager,
    ReactiveManager reactiveManager,
    MenuManager menuManager,
    // Stores (null if system disabled)
    CosmeticStore cosmeticStore,
    PetStore petStore,
    QuestStore questStore,
    // Tick tasks (null if system disabled)
    BukkitRunnable cosmeticTask,
    BukkitRunnable emitterTask,
    BukkitRunnable petTask,
    BukkitRunnable hologramTask,
    BukkitRunnable npcTask,
    BukkitRunnable timelineTask,
    BukkitRunnable reactiveTask,
    BukkitRunnable menuTask,
    BukkitRunnable saveFlushTask,
    // Hook presence (booleans set at startup)
    boolean packetEventsPresent,
    boolean modelEnginePresent,
    boolean placeholderApiPresent,
    boolean vaultPresent
) {}
