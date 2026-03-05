package net.axther.serverCore.api.builder;

import net.axther.serverCore.particle.EmitterData;
import net.axther.serverCore.particle.EmitterInstance;
import net.axther.serverCore.particle.EmitterManager;
import net.axther.serverCore.particle.EmitterPattern;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;

public class EmitterBuilder {

    private final String id;
    private final EmitterManager manager;
    private Location location;
    private Particle particle = Particle.FLAME;
    private EmitterPattern pattern = EmitterPattern.POINT;
    private double radius = 1.0;
    private double height = 2.0;
    private double speed = 0.0;
    private int count = 10;
    private int interval = 2;
    private Color color = null;
    private float size = 1.0f;
    private Material blockMaterial = null;

    public EmitterBuilder(String id, EmitterManager manager) {
        this.id = id;
        this.manager = manager;
    }

    public EmitterBuilder at(Location location) { this.location = location; return this; }

    public EmitterBuilder particle(Particle particle) { this.particle = particle; return this; }

    public EmitterBuilder particle(String particleName) {
        this.particle = Particle.valueOf(particleName.toUpperCase());
        return this;
    }

    public EmitterBuilder pattern(String patternName) {
        this.pattern = EmitterPattern.valueOf(patternName.toUpperCase());
        return this;
    }

    public EmitterBuilder pattern(EmitterPattern pattern) { this.pattern = pattern; return this; }
    public EmitterBuilder radius(double radius) { this.radius = radius; return this; }
    public EmitterBuilder height(double height) { this.height = height; return this; }
    public EmitterBuilder speed(double speed) { this.speed = speed; return this; }
    public EmitterBuilder count(int count) { this.count = count; return this; }
    public EmitterBuilder interval(int interval) { this.interval = interval; return this; }
    public EmitterBuilder color(Color color) { this.color = color; return this; }
    public EmitterBuilder size(float size) { this.size = size; return this; }
    public EmitterBuilder blockMaterial(Material material) { this.blockMaterial = material; return this; }

    public EmitterInstance spawn() {
        if (location == null) throw new IllegalStateException("Location is required");
        EmitterData data = new EmitterData(particle, pattern, radius, height, speed, count, interval, color, size, blockMaterial);
        EmitterInstance instance = manager.createEmitter(id, location, data);
        if (instance == null) {
            throw new IllegalStateException("Failed to create emitter '" + id + "' — id may already exist or block is solid");
        }
        return instance;
    }
}
