package net.axther.serverCore.api.builder;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramAnimation;
import net.axther.serverCore.hologram.HologramManager;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class HologramBuilder {

    private final String id;
    private final HologramManager manager;
    private Location location;
    private final List<String> lines = new ArrayList<>();
    private HologramAnimation animation = HologramAnimation.NONE;
    private String billboard = "CENTER";
    private String background = null;
    private boolean textShadow = false;
    private float viewRange = 1.0f;

    public HologramBuilder(String id, HologramManager manager) {
        this.id = id;
        this.manager = manager;
    }

    public HologramBuilder at(Location location) { this.location = location; return this; }
    public HologramBuilder lines(String... lines) { this.lines.addAll(List.of(lines)); return this; }
    public HologramBuilder animation(HologramAnimation anim) { this.animation = anim; return this; }
    public HologramBuilder billboard(String billboard) { this.billboard = billboard; return this; }
    public HologramBuilder background(String background) { this.background = background; return this; }
    public HologramBuilder textShadow(boolean shadow) { this.textShadow = shadow; return this; }
    public HologramBuilder viewRange(float range) { this.viewRange = range; return this; }

    public Hologram spawn() {
        if (location == null) throw new IllegalStateException("Location is required");
        Hologram hologram = new Hologram(id, location, lines, animation, 0.1, 0.08);
        hologram.setBillboard(billboard);
        hologram.setBackground(background);
        hologram.setTextShadow(textShadow);
        hologram.setViewRange(viewRange);
        manager.register(hologram);
        hologram.spawn();
        return hologram;
    }
}
