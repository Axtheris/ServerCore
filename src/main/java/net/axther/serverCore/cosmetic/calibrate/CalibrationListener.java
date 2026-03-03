package net.axther.serverCore.cosmetic.calibrate;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CalibrationListener implements Listener {

    private final Map<UUID, CalibrationSession> sessions = new HashMap<>();

    public void startSession(CalibrationSession session) {
        sessions.put(session.getPlayer().getUniqueId(), session);
    }

    public CalibrationSession getSession(UUID playerUuid) {
        return sessions.get(playerUuid);
    }

    public void endSession(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public boolean hasSession(UUID playerUuid) {
        return sessions.containsKey(playerUuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        CalibrationSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null && session.isActive()) {
            session.cancel();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        cancelSessionsForMob(event.getEntity());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) {
                cancelSessionsForMob(living);
            }
        }
    }

    private void cancelSessionsForMob(LivingEntity mob) {
        UUID mobUuid = mob.getUniqueId();
        var iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            CalibrationSession session = entry.getValue();
            if (session.isActive() && session.getMob().getUniqueId().equals(mobUuid)) {
                session.getPlayer().sendMessage("Calibration cancelled — the mob is no longer available.");
                session.cancel();
                iterator.remove();
            }
        }
    }
}
