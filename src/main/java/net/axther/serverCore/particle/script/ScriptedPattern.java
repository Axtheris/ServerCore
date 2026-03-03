package net.axther.serverCore.particle.script;

import org.bukkit.util.Vector;

import java.util.List;

/**
 * An {@link net.axther.serverCore.particle.EmitterPattern}-like wrapper around a
 * {@link ParticleScript}. Provides the same {@code computeOffsets} signature so it
 * can be used as a drop-in replacement for built-in patterns.
 */
public class ScriptedPattern {

    private final ParticleScript script;

    public ScriptedPattern(ParticleScript script) {
        this.script = script;
    }

    /**
     * Compute particle offsets using the wrapped script.
     *
     * @param radius  ignored (available to script via other means if needed)
     * @param height  ignored
     * @param tick    tick counter, passed as variable {@code t}
     * @param density particle count, passed as variable {@code n}
     * @return list of offset vectors
     */
    public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
        return ScriptEngine.computeOffsets(script, tick, density);
    }

    public ParticleScript getScript() {
        return script;
    }
}
