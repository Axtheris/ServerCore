package net.axther.serverCore.particle.script;

import org.bukkit.Color;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a {@link ParticleScript} to produce particle offsets and colours.
 */
public final class ScriptEngine {

    private ScriptEngine() {}

    /**
     * Compute offset vectors for every particle index in the range [0, count).
     *
     * @param script the parsed script to evaluate
     * @param tick   the current tick counter (set as variable {@code t})
     * @param count  the total number of particles (set as variable {@code n})
     * @return list of offset vectors, one per particle
     */
    public static List<Vector> computeOffsets(ParticleScript script, int tick, int count) {
        List<Vector> offsets = new ArrayList<>(count);
        ScriptContext ctx = new ScriptContext();
        ctx.set("t", tick);
        ctx.set("n", count);

        for (int i = 0; i < count; i++) {
            ctx.set("i", i);
            offsets.add(script.computeOffset(ctx));
        }
        return offsets;
    }

    /**
     * Evaluate the colour expressions of a script at the given tick.
     * Uses {@code i = 0} and {@code n = 1} for a single sample.
     *
     * @return the computed colour, or {@code null} if the script has no colour expressions
     */
    @Nullable
    public static Color computeColor(ParticleScript script, int tick) {
        if (!script.hasColor()) return null;
        ScriptContext ctx = new ScriptContext();
        ctx.set("t", tick);
        ctx.set("i", 0);
        ctx.set("n", 1);
        return script.computeColor(ctx);
    }
}
