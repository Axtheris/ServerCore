package net.axther.serverCore.hologram.listener;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.action.HologramAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramInteractListener implements Listener {

    private final HologramManager manager;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public HologramInteractListener(HologramManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        UUID entityUuid = event.getRightClicked().getUniqueId();

        Hologram hologram = manager.getByEntityUuid(entityUuid);
        if (hologram == null) return;
        if (hologram.getActions().isEmpty()) return;

        String cooldownKey = player.getUniqueId() + ":" + hologram.getId();
        long now = System.currentTimeMillis();
        Long expiresAt = cooldowns.get(cooldownKey);
        if (expiresAt != null && now < expiresAt) return;

        // Convert ticks to millis (1 tick = 50ms)
        cooldowns.put(cooldownKey, now + (hologram.getClickCooldown() * 50L));

        for (HologramAction action : hologram.getActions()) {
            action.execute(player);
        }

        event.setCancelled(true);
    }
}
