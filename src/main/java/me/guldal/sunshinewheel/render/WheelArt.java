package me.guldal.sunshinewheel.render;

import java.util.List;

/** Çark ve ok için piksel-art çizimleri. Açılar üstten başlayıp saat yönünde ölçülür. */
public final class WheelArt {

    private static final int OUTLINE = 0x2B1A0C;

    private WheelArt() {
    }

    /**
     * Dilimleri verilen sınırlara göre çizer. {@code boundaries} artan sırada, 0 ile başlar ve
     * 360 ile biter; {@code boundaries.length == segmentColors.size() + 1} olmalıdır.
     * Sınır listesi yoksa dilimler eşit kabul edilir (eski davranış).
     */
    public static PixelImage wheel(List<Integer> segmentColors, double[] boundaries, int size,
            int frameColor, int hubColor) {
        int count = Math.max(1, segmentColors.size());
        double[] bounds = boundaries != null && boundaries.length == count + 1
                ? boundaries
                : equalBoundaries(count);
        PixelImage image = new PixelImage(size, size);
        int frame = frameColor & 0xFFFFFF;
        int frameLight = shade(frame, 1.3);
        int frameDark = shade(frame, 0.68);
        int hub = hubColor & 0xFFFFFF;
        int hubDark = shade(hub, 0.62);
        int rivet = shade(frame, 1.65);
        double center = (size - 1) / 2.0;
        double outer = size / 2.0 - 0.5;
        double frameWidth = Math.max(5, size * 0.075);
        double inner = outer - frameWidth;
        double hubRadius = Math.max(4, size * 0.08);

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
                        color = frameDark;
                    } else {
                        color = (dx + dy) < 0 ? frameLight : frame;
                    }
                } else if (distance < hubRadius) {
                    color = distance > hubRadius - 1.2 ? OUTLINE : (distance < hubRadius * 0.35 ? hubDark : hub);
                } else {
                    double angle = (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0) % 360.0;
                    int index = segmentIndex(bounds, count, angle);
                    double start = bounds[index];
                    double end = bounds[index + 1];
                    double toBoundary = Math.min(angle - start, end - angle);
                    double arcPixels = Math.toRadians(toBoundary) * distance;
                    if (count > 1 && end - start > 1e-6 && arcPixels < 0.9) {
                        color = OUTLINE;
                    } else {
                        color = segmentColors.isEmpty() ? 0x808080 : segmentColors.get(index);
                        if (distance > inner - 2.2) {
                            color = shade(color, 0.78);
                        } else if (distance < hubRadius + 2.2) {
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
        double[] rivetAngles = count > 1 ? java.util.Arrays.copyOf(bounds, count) : equalBoundaries(4);
        for (double degrees : rivetAngles) {
            double a = Math.toRadians(degrees);
            int px = (int) Math.round(center + Math.sin(a) * rivetRing);
            int py = (int) Math.round(center - Math.cos(a) * rivetRing);
            for (int oy = -rivetRadius; oy <= rivetRadius; oy++) {
                for (int ox = -rivetRadius; ox <= rivetRadius; ox++) {
                    boolean edge = Math.abs(ox) == rivetRadius || Math.abs(oy) == rivetRadius;
                    image.set(px + ox, py + oy, 0xFF000000 | (edge ? OUTLINE : rivet));
                }
            }
        }
        return image;
    }

    /** Ağırlıklardan bağımsız eşit dilim sınırları (0..360). */
    public static double[] equalBoundaries(int count) {
        int segments = Math.max(1, count);
        double[] bounds = new double[segments + 1];
        for (int i = 0; i <= segments; i++) {
            bounds[i] = i * 360.0 / segments;
        }
        return bounds;
    }

    /** Görüntü açısını (0..360) kapsayan dilim indeksi; sıfır genişlikli dilimler atlanır. */
    public static int segmentIndex(double[] boundaries, int count, double angle) {
        int lo = 0;
        int hi = Math.max(0, count - 1);
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (boundaries[mid] <= angle) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    /** Aşağıyı gösteren ok. */
    public static PixelImage pointer(int width, int height, int color) {
        PixelImage image = new PixelImage(width, height);
        int base = color & 0xFFFFFF;
        int light = shade(base, 1.35);
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
                int pixel = edge ? OUTLINE : (x < center ? light : base);
                image.set(x, y, 0xFF000000 | pixel);
            }
        }
        return image;
    }

    // ------------------------------------------------------------------ deluxe stil

    private static final int SEPARATOR = 0xF2C45A;
    private static final int GEM = 0xD8342C;
    private static final int GEM_HI = 0xFF9A8A;
    private static final int BULB_ON = 0xFFF6C8;
    private static final int BULB_GLOW = 0xFFD45A;
    private static final int BULB_OFF = 0xB0701C;
    private static final int BULB_OFF_DARK = 0x7A4A14;
    private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Süslü çark: bombeli altın çerçeve + ampuller, gradyanlı dilimler, altın ayraçlar,
     * güneş ışınlı mücevher göbek. Parametreler {@link #wheel} ile aynıdır.
     */
    public static PixelImage wheelDeluxe(List<Integer> segmentColors, double[] boundaries, int size,
            int frameColor, int hubColor) {
        int count = Math.max(1, segmentColors.size());
        double[] bounds = boundaries != null && boundaries.length == count + 1
                ? boundaries
                : equalBoundaries(count);
        PixelImage image = new PixelImage(size, size);
        int gold = frameColor & 0xFFFFFF;
        int goldHi = mix(gold, 0xFFFFFF, 0.45);
        int goldDark = shade(gold, 0.55);
        int goldMid = shade(gold, 0.82);
        double c = (size - 1) / 2.0;
        double outer = size / 2.0 - 0.5;
        double rimWidth = Math.max(6.0, size * 0.105);
        double inner = outer - rimWidth;
        double band = Math.max(1.6, size * 0.018);
        double hubRadius = Math.max(5.0, size * 0.13);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - c;
                double dy = y - c;
                double d = Math.hypot(dx, dy);
                if (d > outer + 0.01 || d < hubRadius) {
                    continue;
                }
                int color;
                if (d > outer - 1.1) {
                    color = OUTLINE;
                } else if (d > inner) {
                    if (d < inner + 1.1) {
                        color = OUTLINE;
                    } else if (d > outer - 2.1) {
                        color = goldDark;
                    } else {
                        // 4 tonlu bantlı bombe: az renk = az metin parçası (istemci NBT sınırı).
                        double t = (d - inner) / rimWidth;
                        double light = -(dx + dy) / (d + 1e-6);
                        double bevel = 1 - Math.abs(t - 0.45) * 2.1;
                        if (bevel < 0 || light < -0.3) {
                            color = goldMid;
                        } else if (light > 0.3) {
                            color = goldHi;
                        } else {
                            color = gold;
                        }
                    }
                } else {
                    double angle = (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0) % 360.0;
                    int index = segmentIndex(bounds, count, angle);
                    int base = segmentColors.isEmpty() ? 0x808080 : segmentColors.get(index) & 0xFFFFFF;
                    if (d > inner - band) {
                        color = shade(base, 0.45);
                    } else {
                        // 4 bantlı radyal ton (yumuşak gradyan her pikseli ayrı parça yapıp çarkı görünmez kılıyordu).
                        double t = (d - hubRadius) / Math.max(1.0, inner - band - hubRadius);
                        if (t < 0.3) {
                            color = mix(base, 0xFFFFFF, 0.25);
                        } else if (t < 0.62) {
                            color = mix(base, 0xFFFFFF, 0.10);
                        } else if (t < 0.88) {
                            color = base;
                        } else {
                            color = shade(base, 0.8);
                        }
                    }
                }
                image.set(x, y, 0xFF000000 | color);
            }
        }

        // Dilim ayraçları: merkezden dışa fırça; her satırda aynı kalınlık.
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                double a = Math.toRadians(bounds[i]);
                double sx = Math.sin(a);
                double sy = -Math.cos(a);
                boolean vertical = Math.abs(sy) >= Math.abs(sx);
                boolean[] line = new boolean[size * size];
                java.util.List<int[]> points = new java.util.ArrayList<>();
                for (double r = hubRadius; r <= inner - band + 0.5; r += 0.25) {
                    int px = (int) Math.floor(c + sx * r + 0.5);
                    int py = (int) Math.floor(c + sy * r + 0.5);
                    if (px >= 0 && py >= 0 && px < size && py < size && !line[py * size + px]) {
                        line[py * size + px] = true;
                        points.add(new int[] {px, py});
                    }
                }
                int ox = vertical ? 1 : 0;
                int oy = vertical ? 0 : 1;
                for (int[] p : points) {
                    for (int sign = -1; sign <= 1; sign += 2) {
                        int qx = p[0] + ox * sign;
                        int qy = p[1] + oy * sign;
                        if (qx >= 0 && qy >= 0 && qx < size && qy < size && !line[qy * size + qx]) {
                            image.set(qx, qy, 0xFF000000 | OUTLINE);
                        }
                    }
                }
                for (int[] p : points) {
                    image.set(p[0], p[1], 0xFF000000 | SEPARATOR);
                }
            }
        }

        // Göbek: 8 ışınlı güneş (maske + kesintisiz kenar).
        int rays = 8;
        double tip = hubRadius * 1.45;
        double step = 2 * Math.PI / rays;
        boolean[] rayMask = new boolean[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - c;
                double dy = y - c;
                double d = Math.hypot(dx, dy);
                if (d <= hubRadius - 0.5 || d > tip) {
                    continue;
                }
                double angle = Math.atan2(dx, -dy);
                double off = Math.abs(floorMod(angle + step / 2, step) - step / 2);
                double half = (step * 0.34) * (1 - (d - hubRadius) / (tip - hubRadius)) * hubRadius / Math.max(d, 1e-6);
                if (off <= half) {
                    rayMask[y * size + x] = true;
                }
            }
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!rayMask[y * size + x]) {
                    continue;
                }
                boolean edge = false;
                for (int[] n : NEIGHBORS) {
                    int qx = x + n[0];
                    int qy = y + n[1];
                    boolean inMask = qx >= 0 && qy >= 0 && qx < size && qy < size && rayMask[qy * size + qx];
                    if (!inMask && Math.hypot(qx - c, qy - c) > hubRadius - 0.5) {
                        edge = true;
                        break;
                    }
                }
                double dxy = (x - c) + (y - c);
                image.set(x, y, 0xFF000000 | (edge ? OUTLINE : (dxy < 0 ? goldHi : gold)));
            }
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - c;
                double dy = y - c;
                double d = Math.hypot(dx, dy);
                if (d > hubRadius) {
                    continue;
                }
                int color;
                if (d > hubRadius - 1.1) {
                    color = OUTLINE;
                } else if (d > hubRadius * 0.62) {
                    color = dx + dy < 0 ? mix(gold, goldHi, 0.6) : goldMid;
                } else if (d > hubRadius * 0.62 - 1.0) {
                    color = goldDark;
                } else if (d > hubRadius * 0.38) {
                    color = dx + dy > 0 ? mix(GEM, 0x000000, 0.15) : GEM;
                } else {
                    color = dx < 0 && dy < 0 ? GEM_HI : GEM;
                }
                image.set(x, y, 0xFF000000 | color);
            }
        }

        // Çerçeve ampulleri: merkezler piksel ızgarasına oturtulur, hepsi aynı şablonla çizilir.
        int bulbs = Math.max(12, (int) (size * 0.19));
        double ring = inner + rimWidth * 0.47;
        double bulbRadius = Math.max(1.4, rimWidth * 0.24);
        int reach = (int) Math.ceil(bulbRadius + 0.6);
        for (int k = 0; k < bulbs; k++) {
            double a = 2 * Math.PI * k / bulbs;
            int bx = (int) Math.floor(c + Math.sin(a) * ring + 0.5);
            int by = (int) Math.floor(c - Math.cos(a) * ring + 0.5);
            boolean on = k % 2 == 0;
            for (int oy = -reach; oy <= reach; oy++) {
                for (int ox = -reach; ox <= reach; ox++) {
                    double dd = Math.hypot(ox, oy);
                    if (dd > bulbRadius + 0.5) {
                        continue;
                    }
                    int color;
                    if (dd > bulbRadius - 0.5) {
                        color = OUTLINE;
                    } else if (ox + oy < 0) {
                        color = on ? BULB_ON : BULB_OFF;
                    } else {
                        color = on ? BULB_GLOW : BULB_OFF_DARK;
                    }
                    image.set(bx + ox, by + oy, 0xFF000000 | color);
                }
            }
        }
        return image;
    }

    /** Süslü ok: mücevherli yuvarlak kapak + uzun sivri uç, aşağıyı gösterir. */
    public static PixelImage pointerDeluxe(int width, int height, int color) {
        PixelImage image = new PixelImage(width, height);
        int base = color & 0xFFFFFF;
        int light = mix(base, 0xFFFFFF, 0.45);
        int dark = shade(base, 0.62);
        double c = (width - 1) / 2.0;
        double capRadius = Math.min(width / 2.0 - 0.5, height * 0.3);
        double capY = capRadius;
        boolean[] mask = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double dx = x - c;
                boolean inside;
                if (Math.hypot(dx, y - capY) <= capRadius + 0.2) {
                    inside = true;
                } else if (y >= capY) {
                    double half = capRadius * (1 - (y - capY) / (height - 1 - capY));
                    inside = Math.abs(dx) <= half + 0.35;
                } else {
                    inside = false;
                }
                mask[y * width + x] = inside;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y * width + x]) {
                    continue;
                }
                boolean edge = false;
                for (int[] n : NEIGHBORS) {
                    int qx = x + n[0];
                    int qy = y + n[1];
                    if (qx < 0 || qy < 0 || qx >= width || qy >= height || !mask[qy * width + qx]) {
                        edge = true;
                        break;
                    }
                }
                double dx = x - c;
                int pixel;
                if (edge) {
                    pixel = OUTLINE;
                } else if (Math.hypot(dx, y - capY) < capRadius * 0.5) {
                    pixel = dx < 0 && y < capY ? GEM_HI : GEM;
                } else {
                    pixel = dx < 0 ? light : (dx < 1 ? base : dark);
                }
                image.set(x, y, 0xFF000000 | pixel);
            }
        }
        return image;
    }

    /**
     * PixelText'in üreteceği renkli parça sayısı (satır başına ardışık aynı renk = 1 parça).
     * Çok yüksekse istemci TextDisplay'i NBT boyut sınırı yüzünden çizmez.
     */
    public static int runCount(PixelImage image) {
        int runs = 0;
        for (int y = 0; y < image.height; y++) {
            int previous = 0;
            for (int x = 0; x < image.width; x++) {
                int color = image.get(x, y);
                if (!PixelImage.isTransparent(color) && color != previous) {
                    runs++;
                }
                previous = color;
            }
        }
        return runs;
    }

    private static double floorMod(double value, double modulus) {
        return value - Math.floor(value / modulus) * modulus;
    }

    public static int mix(int a, int b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) (((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
        int g = (int) (((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
        int bl = (int) ((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
        return (r << 16) | (g << 8) | bl;
    }

    public static int shade(int rgb, double factor) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * factor);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * factor);
        int b = (int) Math.min(255, (rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }
}
