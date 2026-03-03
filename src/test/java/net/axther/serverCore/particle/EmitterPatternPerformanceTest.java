package net.axther.serverCore.particle;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and correctness tests for all 32 EmitterPattern implementations.
 * Verifies each pattern produces valid output and runs within acceptable time budgets.
 */
class EmitterPatternPerformanceTest {

    /**
     * Every pattern must produce at least 1 offset and no NaN/Infinity values.
     */
    @ParameterizedTest
    @EnumSource(EmitterPattern.class)
    void patternProducesValidOutput(EmitterPattern pattern) {
        List<Vector> offsets = pattern.computeOffsets(2.0, 3.0, 0, 20);

        assertFalse(offsets.isEmpty(), pattern.name() + " produced no offsets");

        for (int i = 0; i < offsets.size(); i++) {
            Vector v = offsets.get(i);
            assertFalse(Double.isNaN(v.getX()), pattern.name() + " offset[" + i + "].x is NaN");
            assertFalse(Double.isNaN(v.getY()), pattern.name() + " offset[" + i + "].y is NaN");
            assertFalse(Double.isNaN(v.getZ()), pattern.name() + " offset[" + i + "].z is NaN");
            assertFalse(Double.isInfinite(v.getX()), pattern.name() + " offset[" + i + "].x is Infinite");
            assertFalse(Double.isInfinite(v.getY()), pattern.name() + " offset[" + i + "].y is Infinite");
            assertFalse(Double.isInfinite(v.getZ()), pattern.name() + " offset[" + i + "].z is Infinite");
        }
    }

    /**
     * Every pattern must handle density=0 without crashing.
     */
    @ParameterizedTest
    @EnumSource(EmitterPattern.class)
    void patternHandlesZeroDensity(EmitterPattern pattern) {
        assertDoesNotThrow(() -> pattern.computeOffsets(2.0, 3.0, 0, 0));
    }

    /**
     * Every pattern must handle density=1 without crashing.
     */
    @ParameterizedTest
    @EnumSource(EmitterPattern.class)
    void patternHandlesDensityOne(EmitterPattern pattern) {
        List<Vector> offsets = pattern.computeOffsets(2.0, 3.0, 0, 1);
        assertFalse(offsets.isEmpty(), pattern.name() + " should produce output with density=1");
    }

    /**
     * Tests pattern stability across many ticks (no accumulation errors or crashes).
     */
    @ParameterizedTest
    @EnumSource(EmitterPattern.class)
    void patternStableAcross1000Ticks(EmitterPattern pattern) {
        for (int tick = 0; tick < 1000; tick++) {
            List<Vector> offsets = pattern.computeOffsets(2.0, 3.0, tick, 10);
            assertNotNull(offsets, pattern.name() + " returned null at tick " + tick);
        }
    }

    /**
     * Performance benchmark: each pattern with density=50 for 200 ticks.
     * This simulates a single emitter running for 10 seconds at 20 TPS.
     * Each pattern should complete well under 50ms.
     */
    @ParameterizedTest
    @EnumSource(EmitterPattern.class)
    void patternPerformanceBenchmark(EmitterPattern pattern) {
        // Warm up
        for (int t = 0; t < 50; t++) {
            pattern.computeOffsets(2.0, 3.0, t, 50);
        }

        long start = System.nanoTime();
        for (int tick = 0; tick < 200; tick++) {
            pattern.computeOffsets(2.0, 3.0, tick, 50);
        }
        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        int totalVectors = 0;
        for (int tick = 0; tick < 5; tick++) {
            totalVectors += pattern.computeOffsets(2.0, 3.0, tick, 50).size();
        }

        System.out.printf("%-15s 200t x 50d: %6.2f ms (~%d vectors/tick)%n",
                pattern.name(), ms, totalVectors / 5);
        assertTrue(ms < 50, pattern.name() + " took " + ms + "ms for 200 ticks (limit: 50ms)");
    }

    /**
     * High-density stress test: patterns that use nested loops (TORUS, CUBE, WAVE, etc.)
     * can have super-linear scaling with density. Test with density=200.
     */
    @Test
    void highDensityStressTest() {
        EmitterPattern[] heavyPatterns = {
                EmitterPattern.TORUS, EmitterPattern.CUBE, EmitterPattern.WAVE,
                EmitterPattern.ORBIT, EmitterPattern.DNA, EmitterPattern.ATOM
        };

        for (EmitterPattern pattern : heavyPatterns) {
            // Warm up
            for (int t = 0; t < 10; t++) {
                pattern.computeOffsets(2.0, 3.0, t, 200);
            }

            long start = System.nanoTime();
            for (int tick = 0; tick < 100; tick++) {
                pattern.computeOffsets(2.0, 3.0, tick, 200);
            }
            long elapsed = System.nanoTime() - start;
            double ms = elapsed / 1_000_000.0;

            System.out.printf("%-15s 100t x 200d: %6.2f ms%n", pattern.name(), ms);
            assertTrue(ms < 100, pattern.name() + " high-density took " + ms + "ms (limit: 100ms)");
        }
    }

    /**
     * Verify patterns produce output within expected spatial bounds.
     * Offsets should generally stay within a reasonable multiple of the radius/height.
     */
    @ParameterizedTest
    @EnumSource(EmitterPattern.class)
    void patternOutputWithinBounds(EmitterPattern pattern) {
        double radius = 2.0;
        double height = 3.0;
        double maxAllowed = Math.max(radius, height) * 10; // generous bound

        for (int tick = 0; tick < 20; tick++) {
            List<Vector> offsets = pattern.computeOffsets(radius, height, tick, 20);
            for (Vector v : offsets) {
                assertTrue(Math.abs(v.getX()) < maxAllowed,
                        pattern.name() + " X=" + v.getX() + " out of bounds at tick " + tick);
                assertTrue(Math.abs(v.getY()) < maxAllowed,
                        pattern.name() + " Y=" + v.getY() + " out of bounds at tick " + tick);
                assertTrue(Math.abs(v.getZ()) < maxAllowed,
                        pattern.name() + " Z=" + v.getZ() + " out of bounds at tick " + tick);
            }
        }
    }

    /**
     * Deterministic patterns (non-random) should produce identical output for the same inputs.
     */
    @Test
    void deterministicPatternsAreRepeatable() {
        EmitterPattern[] deterministic = {
                EmitterPattern.RING, EmitterPattern.COLUMN, EmitterPattern.HELIX,
                EmitterPattern.SPIRAL, EmitterPattern.HEART, EmitterPattern.STAR,
                EmitterPattern.INFINITY, EmitterPattern.SPHERE, EmitterPattern.PORTAL,
                EmitterPattern.VORTEX, EmitterPattern.SNOWFALL, EmitterPattern.CLOCK,
                EmitterPattern.WINGS, EmitterPattern.CROWN, EmitterPattern.DIAMOND,
                EmitterPattern.PULSE, EmitterPattern.FIREWORK, EmitterPattern.WAVE
        };

        for (EmitterPattern pattern : deterministic) {
            List<Vector> first = pattern.computeOffsets(2.0, 3.0, 42, 15);
            List<Vector> second = pattern.computeOffsets(2.0, 3.0, 42, 15);

            assertEquals(first.size(), second.size(), pattern.name() + " produced different sizes");
            for (int i = 0; i < first.size(); i++) {
                assertEquals(first.get(i).getX(), second.get(i).getX(), 0.0001,
                        pattern.name() + " X differs at index " + i);
                assertEquals(first.get(i).getY(), second.get(i).getY(), 0.0001,
                        pattern.name() + " Y differs at index " + i);
                assertEquals(first.get(i).getZ(), second.get(i).getZ(), 0.0001,
                        pattern.name() + " Z differs at index " + i);
            }
        }
    }
}
