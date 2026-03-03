package net.axther.serverCore.particle;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.List;

public class EmitterInstance {

    private final String id;
    private final String worldName;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private EmitterData data;
    private int tickCounter;

    public EmitterInstance(String id, String worldName, int blockX, int blockY, int blockZ, EmitterData data) {
        this.id = id;
        this.worldName = worldName;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.data = data;
        this.tickCounter = 0;
    }

    public void tick() {
        tickCounter++;
        if (tickCounter % data.interval() != 0) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        double cx = blockX + 0.5;
        double cy = blockY + 0.5;
        double cz = blockZ + 0.5;

        List<Vector> offsets = data.pattern().computeOffsets(data.radius(), data.height(), tickCounter, data.count());
        Object particleData = resolveParticleData(world);

        for (Vector offset : offsets) {
            Location loc = new Location(world, cx + offset.getX(), cy + offset.getY(), cz + offset.getZ());
            if (particleData != null) {
                world.spawnParticle(data.particle(), loc, 0, 0, 0, 0, data.speed(), particleData, true);
            } else {
                world.spawnParticle(data.particle(), loc, 0, 0, 0, 0, data.speed(), null, true);
            }
        }
    }

    private Object resolveParticleData(World world) {
        Particle p = data.particle();
        org.bukkit.Color color = data.color() != null ? data.color() : org.bukkit.Color.WHITE;

        // DUST — requires DustOptions(color, size)
        if (p == Particle.DUST) {
            return new Particle.DustOptions(color, data.size());
        }
        // DUST_COLOR_TRANSITION — requires DustTransition(from, to, size)
        if (p == Particle.DUST_COLOR_TRANSITION) {
            return new Particle.DustTransition(color, color, data.size());
        }
        // Block-data particles: BLOCK, BLOCK_CRUMBLE, BLOCK_MARKER, DUST_PILLAR, FALLING_DUST
        if (p == Particle.BLOCK || p == Particle.FALLING_DUST || p == Particle.BLOCK_CRUMBLE
                || p == Particle.BLOCK_MARKER || p == Particle.DUST_PILLAR) {
            if (data.blockMaterial() != null && data.blockMaterial().isBlock()) {
                return data.blockMaterial().createBlockData();
            }
            return org.bukkit.Material.STONE.createBlockData();
        }
        // Color-based particles: ENTITY_EFFECT, TINTED_LEAVES
        if (p == Particle.ENTITY_EFFECT || p == Particle.TINTED_LEAVES) {
            return color;
        }
        // All other particles (FLAME, HEART, END_ROD, SOUL_FIRE_FLAME, etc.) need no data
        return null;
    }

    public long getChunkKey() {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public String getId() { return id; }
    public String getWorldName() { return worldName; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public EmitterData getData() { return data; }

    public void setData(EmitterData data) { this.data = data; }

    public boolean isChunkLoaded() {
        World world = Bukkit.getWorld(worldName);
        return world != null && world.isChunkLoaded(blockX >> 4, blockZ >> 4);
    }
}
