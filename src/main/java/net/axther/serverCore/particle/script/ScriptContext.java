package net.axther.serverCore.particle.script;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime variable container for particle script evaluation.
 * Holds built-in variables (t, i, n) and constants (pi, e),
 * plus any user-defined variables.
 */
public class ScriptContext {

    private final Map<String, Double> variables = new HashMap<>();

    public ScriptContext() {
        variables.put("pi", Math.PI);
        variables.put("e", Math.E);
        variables.put("t", 0.0);
        variables.put("i", 0.0);
        variables.put("n", 0.0);
    }

    public void set(String name, double value) {
        variables.put(name, value);
    }

    public double get(String name) {
        Double val = variables.get(name);
        return val != null ? val : 0.0;
    }
}
