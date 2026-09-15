package me.guldal.sunshinewheel.wheel;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;

/**
 * Ses ve parçacık ayarlarını metinden okur.
 *
 * <p>Ses biçimi: {@code minecraft:block.note_block.pling 1 1.4} (anahtar, ses düzeyi, perde).
 * Boş bırakmak ya da "yok" yazmak efekti kapatır.
 */
public final class Effects {

    private Effects() {
    }

    public static boolean disabled(String spec) {
        return spec == null || spec.isBlank() || spec.trim().equalsIgnoreCase("yok");
    }

    public static void playSound(Location location, String spec) {
        if (location == null || location.getWorld() == null || disabled(spec)) {
            return;
        }
        String[] parts = spec.trim().split("\\s+");
        float volume = parts.length > 1 ? parseFloat(parts[1], 1f) : 1f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1f) : 1f;
        location.getWorld().playSound(location, parts[0], SoundCategory.MASTER, volume, pitch);
    }

    public static void spawnParticle(Location location, String spec, int count, double spread) {
        if (location == null || location.getWorld() == null || disabled(spec) || count <= 0) {
            return;
        }
        Particle particle = parseParticle(spec);
        if (particle == null) {
            return;
        }
        location.getWorld().spawnParticle(particle, location, count, spread, spread, spread * 0.4, 0.3);
    }

    public static Particle parseParticle(String name) {
        if (disabled(name)) {
            return null;
        }
        String cleaned = name.trim().toUpperCase().replace("MINECRAFT:", "");
        try {
            return Particle.valueOf(cleaned);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static float parseFloat(String text, float fallback) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
