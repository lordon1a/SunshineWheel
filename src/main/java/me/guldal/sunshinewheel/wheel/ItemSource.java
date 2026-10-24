package me.guldal.sunshinewheel.wheel;

import java.util.Locale;

/**
 * Ödülün nasıl teslim edileceği: vanilla malzeme, birebir kopyalanmış eşya (Base64)
 * veya bir eşya eklentisinin kimliği.
 */
public enum ItemSource {

    VANILLA("Vanilla item"),
    EXACT("Captured item"),
    ORAXEN("Oraxen"),
    ITEMSADDER("ItemsAdder"),
    MMOITEMS("MMOItems"),
    NEXO("Nexo"),
    EXECUTABLEITEMS("ExecutableItems");

    private final String label;

    ItemSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Eşya eklentisi kimliği kullanan kaynaklar (EXACT/VANILLA hariç). */
    public boolean isProvider() {
        return this != VANILLA && this != EXACT;
    }

    /** Kimlik öneki; ör. {@code oraxen:ruby}. */
    public String prefix() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ItemSource parse(String value, ItemSource fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
