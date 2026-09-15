package me.guldal.sunshinewheel.wheel;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;

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

    /** Kazanma kutlaması: havai fişek, roket ya da sarmal. */
    public static void playWinEffect(Plugin plugin, Location location, WinEffect effect, String particleSpec) {
        if (location == null || location.getWorld() == null || effect == null || effect == WinEffect.NONE) {
            return;
        }
        switch (effect) {
            case FIREWORK -> spawnFireworks(location, 3, 0);
            case ROCKET -> spawnFireworks(location, 1, 1);
            case SPIRAL -> spiral(plugin, location, particleSpec);
            default -> {
            }
        }
    }

    private static void spawnFireworks(Location location, int count, int power) {
        Color[] palette = {Color.fromRGB(0xF2B544), Color.fromRGB(0xE0463C), Color.fromRGB(0xFFF2B0)};
        for (int i = 0; i < count; i++) {
            Location at = location.clone().add(ThreadLocalRandom.current().nextDouble(-1, 1),
                    i * 0.6, ThreadLocalRandom.current().nextDouble(-1, 1));
            location.getWorld().spawn(at, Firework.class, firework -> {
                FireworkMeta meta = firework.getFireworkMeta();
                meta.setPower(power);
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(palette[ThreadLocalRandom.current().nextInt(palette.length)])
                        .trail(true)
                        .flicker(true)
                        .build());
                firework.setFireworkMeta(meta);
            });
        }
    }

    private static void spiral(Plugin plugin, Location location, String particleSpec) {
        Particle parsed = parseParticle(particleSpec);
        Particle type = parsed == null ? Particle.TOTEM_OF_UNDYING : parsed;
        Location center = location.clone();
        int[] tick = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (tick[0] >= 40 || !center.getWorld().isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
                task.cancel();
                return;
            }
            double t = tick[0]++;
            double radius = 1.4;
            double height = t / 40.0 * 2.2;
            for (double offset : new double[] {0, Math.PI}) {
                double angle = t * 0.45 + offset;
                center.getWorld().spawnParticle(type,
                        center.getX() + Math.cos(angle) * radius, center.getY() + height,
                        center.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }, 1L, 1L);
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
