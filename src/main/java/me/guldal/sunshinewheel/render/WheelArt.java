package me.guldal.sunshinewheel.render;

import java.util.List;

/** Çark ve ok için piksel-art çizimleri. Açılar üstten başlayıp saat yönünde ölçülür. */
public final class WheelArt {

    private static final int OUTLINE = 0x2B1A0C;
    private static final int FRAME = 0xD08A33;
    private static final int FRAME_LIGHT = 0xF0B35A;
    private static final int FRAME_DARK = 0x9A5F20;
    private static final int HUB = 0xE2A44A;
    private static final int HUB_DARK = 0x9A5F20;
    private static final int RIVET = 0xF5D28A;
    private static final int POINTER = 0xF2B544;
    private static final int POINTER_LIGHT = 0xFFD98A;

    private WheelArt() {
    }

    public static PixelImage wheel(List<Integer> segmentColors, int size) {
        int count = Math.max(1, segmentColors.size());
        PixelImage image = new PixelImage(size, size);
        double center = (size - 1) / 2.0;
        double outer = size / 2.0 - 0.5;
        double frameWidth = Math.max(5, size * 0.075);
        double inner = outer - frameWidth;
        double hub = Math.max(4, size * 0.08);
        double segment = 360.0 / count;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - center;
                double dy = y - center;
                double distance = Math.hypot(dx, dy);
                if (distance > outer + 0.01) {
                    continue;
                }
                int color;
                if (distance > outer - 1.2) {
                    color = OUTLINE;
                } else if (distance > inner) {
                    if (distance < inner + 1.2) {
                        color = OUTLINE;
                    } else if (distance > outer - 2.4) {
                        color = FRAME_DARK;
                    } else {
                        color = (dx + dy) < 0 ? FRAME_LIGHT : FRAME;
                    }
                } else if (distance < hub) {
                    color = distance > hub - 1.2 ? OUTLINE : (distance < hub * 0.35 ? HUB_DARK : HUB);
                } else {
                    double angle = (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0) % 360.0;
                    int index = Math.min(count - 1, (int) (angle / segment));
                    double toBoundary = Math.min(angle - index * segment, (index + 1) * segment - angle);
                    double arcPixels = Math.toRadians(toBoundary) * distance;
                    if (count > 1 && arcPixels < 0.9) {
                        color = OUTLINE;
                    } else {
                        color = segmentColors.isEmpty() ? 0x808080 : segmentColors.get(index);
                        if (distance > inner - 2.2) {
                            color = shade(color, 0.78);
                        } else if (distance < hub + 2.2) {
                            color = shade(color, 0.85);
                        }
                    }
                }
                image.set(x, y, 0xFF000000 | color);
            }
        }

        // Dilim sınırlarında çerçeve perçinleri.
        int rivetRadius = Math.max(1, (int) Math.round(frameWidth / 4.0));
        double rivetRing = inner + frameWidth / 2.0;
        int rivets = count > 1 ? count : 4;
        for (int i = 0; i < rivets; i++) {
            double a = Math.toRadians(i * 360.0 / rivets);
            int px = (int) Math.round(center + Math.sin(a) * rivetRing);
            int py = (int) Math.round(center - Math.cos(a) * rivetRing);
            for (int oy = -rivetRadius; oy <= rivetRadius; oy++) {
                for (int ox = -rivetRadius; ox <= rivetRadius; ox++) {
                    boolean edge = Math.abs(ox) == rivetRadius || Math.abs(oy) == rivetRadius;
                    image.set(px + ox, py + oy, 0xFF000000 | (edge ? OUTLINE : RIVET));
                }
            }
        }
        return image;
    }

    /** Aşağıyı gösteren ok. */
    public static PixelImage pointer(int width, int height) {
        PixelImage image = new PixelImage(width, height);
        double center = (width - 1) / 2.0;
        int capRows = Math.max(2, height / 5);
        for (int y = 0; y < height; y++) {
            double half = y < capRows ? center : (height - 1 - y) * center / (height - 1 - capRows);
            for (int x = 0; x < width; x++) {
                double offset = Math.abs(x - center);
                if (offset > half + 0.5) {
                    continue;
                }
                boolean edge = offset > half - 0.7 || y == 0 || y == height - 1;
                int color = edge ? OUTLINE : (x < center ? POINTER_LIGHT : POINTER);
                image.set(x, y, 0xFF000000 | color);
            }
        }
        return image;
    }

    public static int shade(int rgb, double factor) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * factor);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * factor);
        int b = (int) Math.min(255, (rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }
}
