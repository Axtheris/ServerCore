package net.axther.serverCore.particle.script;

import org.bukkit.Color;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScriptEngine -- the bridge between parsed scripts and computed particle data.
 * Focuses on the compute pipeline correctness and allocation behavior.
 */
class ScriptEngineTest {

    @Test
    void computeOffsetsProducesCorrectCount() {
        ParticleScript script = new ParticleScript("i", "0", "0", null, null, null);
        List<Vector> offsets = ScriptEngine.computeOffsets(script, 0, 20);
        assertEquals(20, offsets.size());
    }

    @Test
    void computeOffsetsZeroCount() {
        ParticleScript script = new ParticleScript("1", "2", "3", null, null, null);
        List<Vector> offsets = ScriptEngine.computeOffsets(script, 0, 0);
        assertTrue(offsets.isEmpty());
    }

    @Test
    void computeOffsetsIndexValues() {
        // x = i, y = 0, z = 0 -- each particle's x should equal its index
        ParticleScript script = new ParticleScript("i", "0", "0", null, null, null);
        List<Vector> offsets = ScriptEngine.computeOffsets(script, 0, 5);

        for (int i = 0; i < 5; i++) {
            assertEquals(i, offsets.get(i).getX(), 0.0001, "Offset " + i + " x should equal index");
        }
    }

    @Test
    void computeOffsetsTickAffectsOutput() {
        // x = t, y = 0, z = 0
        ParticleScript script = new ParticleScript("t", "0", "0", null, null, null);

        List<Vector> tick0 = ScriptEngine.computeOffsets(script, 0, 1);
        List<Vector> tick100 = ScriptEngine.computeOffsets(script, 100, 1);

        assertEquals(0.0, tick0.get(0).getX(), 0.0001);
        assertEquals(100.0, tick100.get(0).getX(), 0.0001);
    }

    @Test
    void computeColorReturnsNullWithoutColorExpressions() {
        ParticleScript script = new ParticleScript("0", "0", "0", null, null, null);
        assertNull(ScriptEngine.computeColor(script, 0));
    }

    @Test
    void computeColorReturnsValidColor() {
        ParticleScript script = new ParticleScript("0", "0", "0", "128", "64", "200");
        Color color = ScriptEngine.computeColor(script, 0);
        assertNotNull(color);
        assertEquals(128, color.getRed());
        assertEquals(64, color.getGreen());
        assertEquals(200, color.getBlue());
    }

    @Test
    void computeColorClampsValues() {
        // r = 300 (clamped to 255), g = -50 (clamped to 0), b = 100
        ParticleScript script = new ParticleScript("0", "0", "0", "300", "-50", "100");
        Color color = ScriptEngine.computeColor(script, 0);
        assertNotNull(color);
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(100, color.getBlue());
    }

    @Test
    void computeColorVariesWithTick() {
        // r = sin(t) * 127 + 128  -- varies with tick
        ParticleScript script = new ParticleScript("0", "0", "0",
                "sin(t) * 127 + 128", "0", "0");

        Color at0 = ScriptEngine.computeColor(script, 0);
        Color at100 = ScriptEngine.computeColor(script, 100);

        assertNotNull(at0);
        assertNotNull(at100);
        // sin(0) = 0 -> r = 128, sin(100) != 0 -> different value
        assertEquals(128, at0.getRed());
        assertNotEquals(128, at100.getRed());
    }

    /**
     * Performance: compute offsets for a realistic spiral script, 100 particles, 1000 ticks.
     * This is the hot path for scripted emitters.
     */
    @Test
    void computeOffsetsThroughput() {
        ParticleScript script = new ParticleScript(
                "cos(t * 0.1 + i * 2 * pi / n) * 2",
                "t * 0.05 % 3",
                "sin(t * 0.1 + i * 2 * pi / n) * 2",
                null, null, null
        );

        // Warm up
        for (int t = 0; t < 100; t++) {
            ScriptEngine.computeOffsets(script, t, 100);
        }

        long start = System.nanoTime();
        for (int tick = 0; tick < 1000; tick++) {
            ScriptEngine.computeOffsets(script, tick, 100);
        }
        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;

        System.out.printf("ScriptEngine 1000t x 100p: %.2f ms (%.0f vectors/ms)%n",
                ms, 100_000.0 / ms);
        // 1000 ticks at 50ms/tick would be 50 seconds -- we need this under 1s total
        assertTrue(ms < 1000, "1000 ticks of 100 particles should take <1s, took " + ms + "ms");
    }
}
