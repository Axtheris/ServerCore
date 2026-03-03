package net.axther.serverCore.particle;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;

public record EmitterData(
        Particle particle,
        EmitterPattern pattern,
        double radius,
        double height,
        double speed,
        int count,
        int interval,
        @Nullable Color color,
        float size,
        @Nullable Material blockMaterial
) {
    public EmitterData {
        if (interval < 1) interval = 1;
        if (count < 1) count = 1;
        if (size <= 0) size = 1.0f;
    }
}
