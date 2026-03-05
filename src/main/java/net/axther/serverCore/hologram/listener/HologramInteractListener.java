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
    private long currentTick = 0;

    public HologramInteractListener(HologramManager manager) {
        this.manager = manager;
    }

    public void tick() {
        currentTick++;
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        UUID entityUuid = event.getRightClicked().getUniqueId();

        Hologram hologram = manager.getByEntityUuid(entityUuid);
        if (hologram == null) return;
        if (hologram.getActions().isEmpty()) return;

        String cooldownKey = player.getUniqueId() + ":" + hologram.getId();
        Long expiresAt = cooldowns.get(cooldownKey);
        if (expiresAt != null && currentTick < expiresAt) return;

        cooldowns.put(cooldownKey, currentTick + hologram.getClickCooldown());

        for (HologramAction action : hologram.getActions()) {
            action.execute(player);
        }

        event.setCancelled(true);
    }
}
