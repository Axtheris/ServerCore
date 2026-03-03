package net.axther.serverCore.particle.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the particle scripting engine.
 * These verify that parsing and evaluation can handle high-throughput
 * scenarios (many emitters, high particle counts, every-tick evaluation).
 */
class ScriptPerformanceTest {

    /**
     * Simulates a single emitter with 100 particles evaluated every tick for 200 ticks.
     * This is 20,000 expression evaluations (x, y, z = 60,000 total expression calls).
     * Must complete in under 200ms to not impact server TPS.
     */
    @Test
    void singleEmitter100Particles200Ticks() {
        ParticleScript script = new ParticleScript(
                "cos(t * 0.1 + i * 2 * pi / n) * 2",
                "t * 0.05 % 3",
                "sin(t * 0.1 + i * 2 * pi / n) * 2",
                "128 + 127 * sin(t * 0.05)",
                "50",
                "200"
        );

        // Warm up the JIT
        ScriptContext warmCtx = new ScriptContext();
        for (int i = 0; i < 1000; i++) {
            warmCtx.set("t", i);
            warmCtx.set("i", 0);
            warmCtx.set("n", 1);
            script.computeOffset(warmCtx);
        }

        long start = System.nanoTime();
        ScriptContext ctx = new ScriptContext();

        for (int tick = 0; tick < 200; tick++) {
            ctx.set("t", tick);
            ctx.set("n", 100);
            for (int i = 0; i < 100; i++) {
                ctx.set("i", i);
                script.computeOffset(ctx);
            }
        }

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        System.out.printf("Single emitter 100p x 200t: %.2f ms (%.0f evals/ms)%n", ms, 20000.0 / ms);
        assertTrue(ms < 200, "Should complete 20k evaluations in <200ms, took " + ms + "ms");
    }

    /**
     * Simulates 50 concurrent emitters each with 20 particles, evaluated for 1 tick.
     * This represents a busy server with many active emitters.
     * Must complete in under 10ms per tick.
     */
    @Test
    void fiftyEmittersOneTick() {
        // Build 50 different scripts (varying complexity)
        ParticleScript[] scripts = new ParticleScript[50];
        for (int e = 0; e < 50; e++) {
            scripts[e] = new ParticleScript(
                    "cos(t * 0.1 + i * 2 * pi / n) * " + (1 + e * 0.1),
                    "sin(t * 0.05 + i) * " + (0.5 + e * 0.02),
                    "sin(t * 0.1 + i * 2 * pi / n) * " + (1 + e * 0.1),
                    null, null, null
            );
        }

        // Warm up
        ScriptContext warmCtx = new ScriptContext();
        for (ParticleScript s : scripts) {
            for (int i = 0; i < 100; i++) {
                warmCtx.set("t", i);
                warmCtx.set("i", 0);
                warmCtx.set("n", 20);
                s.computeOffset(warmCtx);
            }
        }

        long start = System.nanoTime();

        for (ParticleScript script : scripts) {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", 1000);
            ctx.set("n", 20);
            for (int i = 0; i < 20; i++) {
                ctx.set("i", i);
                script.computeOffset(ctx);
            }
        }

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        System.out.printf("50 emitters x 20 particles (1 tick): %.2f ms%n", ms);
        assertTrue(ms < 10, "50 emitters per tick should take <10ms, took " + ms + "ms");
    }

    /**
     * Tests that parsing complex expressions is fast enough for reload scenarios.
     * Parsing 100 expression strings should complete quickly.
     */
    @Test
    void parsingThroughput() {
        String[] expressions = {
                "cos(t * 0.1 + i * 2 * pi / n) * 2",
                "sin(t * 0.1 + i * 2 * pi / n) * 2",
                "128 + 127 * sin(t * 0.05)",
                "t * 0.05 % 3",
                "abs(sin(t * 0.1)) * 3",
                "floor(t / 20) % 5",
                "lerp(0, 10, sin(t * 0.01) * 0.5 + 0.5)",
                "min(max(sin(t * 0.1), -0.5), 0.5) * 4",
                "sqrt(i * i + t * 0.01)",
                "rand() * 2 - 1"
        };

        // Warm up
        for (String expr : expressions) ScriptParser.parse(expr);

        long start = System.nanoTime();
        for (int round = 0; round < 100; round++) {
            for (String expr : expressions) {
                ScriptParser.parse(expr);
            }
        }
        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        System.out.printf("1000 expression parses: %.2f ms (%.0f parses/ms)%n", ms, 1000.0 / ms);
        assertTrue(ms < 100, "1000 parses should take <100ms, took " + ms + "ms");
    }

    /**
     * Tests ScriptContext allocation overhead.
     * Creating and populating contexts is done once per emitter per tick.
     */
    @Test
    void contextAllocationOverhead() {
        long start = System.nanoTime();

        for (int i = 0; i < 10_000; i++) {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", i);
            ctx.set("n", 20);
            ctx.set("i", i % 20);
            ctx.get("pi");
            ctx.get("t");
        }

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        System.out.printf("10k context create+use cycles: %.2f ms%n", ms);
        assertTrue(ms < 50, "10k context cycles should take <50ms, took " + ms + "ms");
    }

    /**
     * Stress test: deeply nested expressions.
     * Ensures the recursive descent parser handles nesting without stack overflow.
     */
    @Test
    void deeplyNestedExpression() {
        // Build: (((((...(1 + 1) + 1) + 1)...)))  -- 50 levels deep
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("(");
        sb.append("1");
        for (int i = 0; i < 50; i++) sb.append(" + 1)");

        ParticleScript.Expression expr = ScriptParser.parse(sb.toString());
        double result = expr.evaluate(new ScriptContext());
        assertEquals(51.0, result);
    }

    /**
     * Stress test: very long expression with many terms.
     * sin(t) + sin(t*2) + sin(t*3) + ... + sin(t*100)
     */
    @Test
    void manyTermsExpression() {
        StringBuilder sb = new StringBuilder("sin(t)");
        for (int i = 2; i <= 100; i++) {
            sb.append(" + sin(t * ").append(i).append(")");
        }

        ParticleScript.Expression expr = ScriptParser.parse(sb.toString());
        ScriptContext ctx = new ScriptContext();

        long start = System.nanoTime();
        for (int tick = 0; tick < 1000; tick++) {
            ctx.set("t", tick * 0.1);
            expr.evaluate(ctx);
        }
        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        System.out.printf("100-term expression x 1000 evals: %.2f ms%n", ms);
        assertTrue(ms < 100, "Should handle 100-term expression in <100ms, took " + ms + "ms");
    }
}
