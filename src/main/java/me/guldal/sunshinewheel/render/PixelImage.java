package me.guldal.sunshinewheel.render;

/** ARGB piksel tamponu. Alfa &lt; 128 olan pikseller şeffaf sayılır. */
public final class PixelImage {

    public final int width;
    public final int height;
    private final int[] argb;

    public PixelImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.argb = new int[width * height];
    }

    public void set(int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            argb[y * width + x] = color;
        }
    }

    public int get(int x, int y) {
        return argb[y * width + x];
    }

    public static boolean isTransparent(int color) {
        return (color >>> 24) < 128;
    }

    public void fillRect(int x, int y, int w, int h, int color) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                set(xx, yy, color);
            }
        }
    }

    /** Başka bir görüntüyü bu görüntünün üstüne yapıştırır; şeffaf pikseller atlanır. */
    public void blit(PixelImage source, int x, int y) {
        for (int sy = 0; sy < source.height; sy++) {
            for (int sx = 0; sx < source.width; sx++) {
                int color = source.get(sx, sy);
                if (!isTransparent(color)) {
                    set(x + sx, y + sy, color);
                }
            }
        }
    }

    /** İçi boş dikdörtgen çerçeve. */
    public void border(int x, int y, int w, int h, int color) {
        fillRect(x, y, w, 1, color);
        fillRect(x, y + h - 1, w, 1, color);
        fillRect(x, y, 1, h, color);
        fillRect(x + w - 1, y, 1, h, color);
    }
}
