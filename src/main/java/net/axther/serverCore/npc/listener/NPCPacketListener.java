package net.axther.serverCore.npc.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NPCPacketListener extends PacketListenerAbstract {

    private static final long INTERACTION_COOLDOWN_MS = 200;

    private final JavaPlugin plugin;
    private final NPCManager manager;
    private final NPCListener npcListener;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public NPCPacketListener(JavaPlugin plugin, NPCManager manager, NPCListener npcListener) {
        this.plugin = plugin;
        this.manager = manager;
        this.npcListener = npcListener;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        int entityId = wrapper.getEntityId();

        NPC npc = manager.getByEntityId(entityId);
        if (npc == null) return;

        // Cancel the packet to prevent server-side "invalid entity" warnings
        event.setCancelled(true);

        // Only handle INTERACT actions (right-click)
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT
                && wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (player == null) return;

        UUID playerId = player.getUniqueId();

        // Per-player cooldown
        long now = System.currentTimeMillis();
        Long lastInteraction = cooldowns.get(playerId);
        if (lastInteraction != null && now - lastInteraction < INTERACTION_COOLDOWN_MS) return;
        cooldowns.put(playerId, now);

        // Dispatch to main thread
        Bukkit.getScheduler().runTask(plugin, () -> npcListener.handleInteraction(player, npc));
    }
}
