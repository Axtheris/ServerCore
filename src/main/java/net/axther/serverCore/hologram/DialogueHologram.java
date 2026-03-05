package net.axther.serverCore.hologram;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DialogueHologram {

    private final JavaPlugin plugin;
    private final Player viewer;
    private final Location baseLocation;
    private final double yOffset;
    private UUID entityUuid;

    public DialogueHologram(JavaPlugin plugin, Player viewer, Location npcLocation, double yOffset) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.baseLocation = npcLocation.clone();
        this.yOffset = yOffset;
    }

    public void spawn(String text) {
        despawn();
        World world = baseLocation.getWorld();
        if (world == null) return;

        Location spawnLoc = baseLocation.clone().add(0, yOffset, 0);

        TextDisplay display = world.spawn(spawnLoc, TextDisplay.class, entity -> {
            entity.text(MiniMessage.miniMessage().deserialize(text));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
            entity.setPersistent(false);
            entity.setShadowed(true);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setLineWidth(250);
            entity.setViewRange(0.5f);
        });

        this.entityUuid = display.getUniqueId();

        // Hide from all players except the viewer
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(viewer)) {
                online.hideEntity(plugin, display);
            }
        }
    }

    public void updateText(String text) {
        if (entityUuid == null) return;
        Entity entity = plugin.getServer().getEntity(entityUuid);
        if (entity instanceof TextDisplay display) {
            display.text(MiniMessage.miniMessage().deserialize(text));
        }
    }

    public void despawn() {
        if (entityUuid == null) return;
        Entity entity = plugin.getServer().getEntity(entityUuid);
        if (entity != null) {
            entity.remove();
        }
        entityUuid = null;
    }

    public boolean isSpawned() {
        if (entityUuid == null) return false;
        Entity entity = plugin.getServer().getEntity(entityUuid);
        return entity != null && !entity.isDead();
    }

    public Player getViewer() { return viewer; }
    public UUID getEntityUuid() { return entityUuid; }
}
