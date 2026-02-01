package com.protectium.protection;

/**
 * Cada categoría de protección vive aquí.
 * Es la única fuente de verdad para nombres de tipo.
 */
public enum ProtectionType {

    AREA("area"),
    SPAWN("spawn"),
    ENTRADA("entrada"),
    FUEGO("fuego"),
    REDSTONE("redstone");

    private final String configKey;

    ProtectionType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    /**
     * Búsqueda case-insensitive. Retorna null si no hay match.
     */
    public static ProtectionType fromString(String raw) {
        if (raw == null) return null;
        for (ProtectionType t : values()) {
            if (t.name().equalsIgnoreCase(raw.trim())
                || t.configKey.equalsIgnoreCase(raw.trim())) {
                return t;
            }
        }
        return null;
    }

    /** Listado para autocompletar en comandos. */
    public static String[] names() {
        String[] out = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            out[i] = values()[i].configKey;
        }
        return out;
    }
}
