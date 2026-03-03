package net.axther.serverCore.particle.script;

import org.bukkit.Color;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * A parsed particle script containing position expressions (x, y, z) and
 * optional colour expressions (r, g, b). Expression strings are parsed once
 * at construction time; evaluation happens per-particle via a
 * {@link ScriptContext}.
 */
public class ParticleScript {

    @FunctionalInterface
    public interface Expression {
        double evaluate(ScriptContext ctx);
    }

    private final Expression xExpr;
    private final Expression yExpr;
    private final Expression zExpr;

    @Nullable private final Expression rExpr;
    @Nullable private final Expression gExpr;
    @Nullable private final Expression bExpr;

    // Keep the raw strings so we can serialise back to YAML
    private final String xRaw;
    private final String yRaw;
    private final String zRaw;
    @Nullable private final String rRaw;
    @Nullable private final String gRaw;
    @Nullable private final String bRaw;

    public ParticleScript(String x, String y, String z,
                          @Nullable String r, @Nullable String g, @Nullable String b) {
        this.xRaw = x;
        this.yRaw = y;
        this.zRaw = z;
        this.rRaw = r;
        this.gRaw = g;
        this.bRaw = b;

        this.xExpr = ScriptParser.parse(x);
        this.yExpr = ScriptParser.parse(y);
        this.zExpr = ScriptParser.parse(z);
        this.rExpr = r != null ? ScriptParser.parse(r) : null;
        this.gExpr = g != null ? ScriptParser.parse(g) : null;
        this.bExpr = b != null ? ScriptParser.parse(b) : null;
    }

    /**
     * Evaluate the x, y, z expressions and return the resulting offset vector.
     */
    public Vector computeOffset(ScriptContext ctx) {
        return new Vector(
                xExpr.evaluate(ctx),
                yExpr.evaluate(ctx),
                zExpr.evaluate(ctx)
        );
    }

    /**
     * Evaluate the r, g, b colour expressions if present.
     * Values are clamped to [0, 255].
     *
     * @return the computed {@link Color}, or {@code null} if no colour expressions are defined.
     */
    @Nullable
    public Color computeColor(ScriptContext ctx) {
        if (rExpr == null || gExpr == null || bExpr == null) return null;
        int r = clamp((int) rExpr.evaluate(ctx), 0, 255);
        int g = clamp((int) gExpr.evaluate(ctx), 0, 255);
        int b = clamp((int) bExpr.evaluate(ctx), 0, 255);
        return Color.fromRGB(r, g, b);
    }

    public boolean hasColor() {
        return rExpr != null && gExpr != null && bExpr != null;
    }

    // --- Raw string accessors for serialisation ---

    public String xRaw() { return xRaw; }
    public String yRaw() { return yRaw; }
    public String zRaw() { return zRaw; }
    @Nullable public String rRaw() { return rRaw; }
    @Nullable public String gRaw() { return gRaw; }
    @Nullable public String bRaw() { return bRaw; }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
