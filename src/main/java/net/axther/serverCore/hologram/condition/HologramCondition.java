package net.axther.serverCore.hologram.condition;

public class HologramCondition {

    private final String type;
    private final String value;
    private final String equals;
    private final String min;
    private final String max;

    private HologramCondition(String type, String value, String equals, String min, String max) {
        this.type = type;
        this.value = value;
        this.equals = equals;
        this.min = min;
        this.max = max;
    }

    public static HologramCondition parse(String type, String value, String equals, String min) {
        return new HologramCondition(type, value, equals, min, null);
    }

    public static HologramCondition parse(String type, String value, String equals, String min, String max) {
        return new HologramCondition(type, value, equals, min, max);
    }

    public String getType() { return type; }
    public String getValue() { return value; }
    public String getEquals() { return equals; }
    public String getMin() { return min; }
    public String getMax() { return max; }
}
