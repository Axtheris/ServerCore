package net.axther.serverCore.hologram.condition;

public class HologramCondition {

    private final String type;
    private final String value;
    private final String equals;
    private final String min;

    private HologramCondition(String type, String value, String equals, String min) {
        this.type = type;
        this.value = value;
        this.equals = equals;
        this.min = min;
    }

    public static HologramCondition parse(String type, String value, String equals, String min) {
        return new HologramCondition(type, value, equals, min);
    }

    public String getType() { return type; }
    public String getValue() { return value; }
    public String getEquals() { return equals; }
    public String getMin() { return min; }
}
