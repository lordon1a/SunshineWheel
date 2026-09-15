package me.guldal.sunshinewheel.wheel;

/** Kazanma anındaki kutlama efekti. */
public enum WinEffect {
    NONE("None"),
    FIREWORK("Firework"),
    ROCKET("Rocket"),
    SPIRAL("Spiral");

    private final String label;

    WinEffect(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
