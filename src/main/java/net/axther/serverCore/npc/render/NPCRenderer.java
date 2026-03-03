package net.axther.serverCore.npc.render;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.github.retrooper.packetevents.util.Vector3d;
import net.axther.serverCore.npc.NPC;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class NPCRenderer {

    private final JavaPlugin plugin;

    public NPCRenderer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendSpawn(Player player, NPC npc) {
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        if (channel == null) return;

        Location loc = npc.getLocation();
        UUID uuid = npc.getEntityUuid();
        int entityId = npc.getEntityId();

        // 1. PlayerInfoUpdate -- add to tab list with skin
        UserProfile profile = new UserProfile(uuid, npc.getId());
        if (npc.getSkinTexture() != null && !npc.getSkinTexture().isBlank()) {
            List<TextureProperty> properties = new ArrayList<>();
            properties.add(new TextureProperty("textures", npc.getSkinTexture(),
                    npc.getSkinSignature() != null ? npc.getSkinSignature() : ""));
            profile.setTextureProperties(properties);
        }

        WrapperPlayServerPlayerInfoUpdate infoPacket = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED
                ),
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        profile, false, 0, null,
                        MiniMessage.miniMessage().deserialize(npc.getDisplayName()), null
                )
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, infoPacket);

        // 2. SpawnEntity -- spawn the player entity
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId, java.util.Optional.of(uuid), EntityTypes.PLAYER,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getPitch(), npc.getYaw(), npc.getYaw(),
                0, java.util.Optional.empty()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, spawnPacket);

        // 3. EntityMetadata -- enable all skin layers
        byte skinLayersMask = (byte) 0x7F; // all layers visible
        List<EntityData> metadata = new ArrayList<>();
        metadata.add(new EntityData(17, EntityDataTypes.BYTE, skinLayersMask));
        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, metaPacket);

        // 4. HeadRotation
        sendHeadRotation(player, npc, npc.getYaw(), 0f);

        // 5. Delayed PlayerInfoRemove (2 ticks) to hide from tab list
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(uuid);
                Object ch = PacketEvents.getAPI().getPlayerManager().getChannel(player);
                if (ch != null) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(ch, removePacket);
                }
            }
        }, 2L);
    }

    public void sendDespawn(Player player, NPC npc) {
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        if (channel == null) return;

        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(npc.getEntityId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, destroyPacket);

        WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(npc.getEntityUuid());
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, removePacket);
    }

    public void sendHeadRotation(Player player, NPC npc, float yaw, float pitch) {
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        if (channel == null) return;

        WrapperPlayServerEntityHeadLook headLookPacket = new WrapperPlayServerEntityHeadLook(npc.getEntityId(), yaw);
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, headLookPacket);

        WrapperPlayServerEntityRotation rotationPacket = new WrapperPlayServerEntityRotation(
                npc.getEntityId(), yaw, pitch, true
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, rotationPacket);
    }

    public void sendTeleport(Player player, NPC npc) {
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        if (channel == null) return;

        Location loc = npc.getLocation();
        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                npc.getEntityId(),
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                npc.getYaw(), 0f, true
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(channel, teleportPacket);

        sendHeadRotation(player, npc, npc.getYaw(), 0f);
    }
}
