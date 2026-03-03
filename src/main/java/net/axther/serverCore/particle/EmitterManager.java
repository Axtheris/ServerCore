package net.axther.serverCore.particle;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;

import java.util.*;

public class EmitterManager {

    private final Map<String, EmitterInstance> emittersById = new LinkedHashMap<>();
    private final Map<Long, List<EmitterInstance>> chunkIndex = new HashMap<>();

    public EmitterInstance createEmitter(String id, Location location, EmitterData data) {
        if (emittersById.containsKey(id)) return null;

        Block block = location.getBlock();
        if (block.getType().isSolid()) return null;

        // Place light block at level 0 (invisible, walkthrough)
        block.setType(Material.LIGHT);
        Light lightData = (Light) block.getBlockData();
        lightData.setLevel(0);
        block.setBlockData(lightData);

        EmitterInstance instance = new EmitterInstance(
                id, location.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), data
        );

        registerInstance(instance);
        return instance;
    }

    public boolean removeEmitter(String id) {
        EmitterInstance instance = emittersById.remove(id);
        if (instance == null) return false;

        unindexChunk(instance);

        World world = Bukkit.getWorld(instance.getWorldName());
        if (world != null) {
            world.getBlockAt(instance.getBlockX(), instance.getBlockY(), instance.getBlockZ()).setType(Material.AIR);
        }
        return true;
    }

    public EmitterInstance getEmitterAt(Location location) {
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        String worldName = location.getWorld().getName();

        int chunkX = bx >> 4;
        int chunkZ = bz >> 4;
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

        List<EmitterInstance> inChunk = chunkIndex.get(key);
        if (inChunk == null) return null;

        for (EmitterInstance inst : inChunk) {
            if (inst.getBlockX() == bx && inst.getBlockY() == by && inst.getBlockZ() == bz
                    && inst.getWorldName().equals(worldName)) {
                return inst;
            }
        }
        return null;
    }

    public boolean replaceEmitterData(String id, EmitterData newData) {
        EmitterInstance instance = emittersById.get(id);
        if (instance == null) return false;
        instance.setData(newData);
        return true;
    }

    public void registerExisting(EmitterInstance instance) {
        registerInstance(instance);
    }

    public void tickAll() {
        for (EmitterInstance instance : emittersById.values()) {
            if (instance.isChunkLoaded()) {
                instance.tick();
            }
        }
    }

    public void destroyAll() {
        for (EmitterInstance instance : emittersById.values()) {
            World world = Bukkit.getWorld(instance.getWorldName());
            if (world != null) {
                world.getBlockAt(instance.getBlockX(), instance.getBlockY(), instance.getBlockZ()).setType(Material.AIR);
            }
        }
        emittersById.clear();
        chunkIndex.clear();
    }

    public EmitterInstance getEmitter(String id) {
        return emittersById.get(id);
    }

    public Collection<EmitterInstance> getAllEmitters() {
        return Collections.unmodifiableCollection(emittersById.values());
    }

    private void registerInstance(EmitterInstance instance) {
        emittersById.put(instance.getId(), instance);
        long key = instance.getChunkKey();
        chunkIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
    }

    private void unindexChunk(EmitterInstance instance) {
        long key = instance.getChunkKey();
        List<EmitterInstance> list = chunkIndex.get(key);
        if (list != null) {
            list.remove(instance);
            if (list.isEmpty()) chunkIndex.remove(key);
        }
    }
}
