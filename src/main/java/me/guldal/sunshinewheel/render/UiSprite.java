package me.guldal.sunshinewheel.render;

/**
 * Kaynak paketindeki hazır arayüz resimleri.
 *
 * <p>Sıra ve ölçüler {@code pack/generate_pack.py} içindeki UI_SPRITES listesiyle birebir aynı olmalıdır.
 * Her parçanın her yükseklik için ayrı bir karakteri vardır (ascent varyantı); böylece resim satır
 * atlamadan çizilir ve dialog içinde yer kaplamaz, sadece arkada durur.
 */
public enum UiSprite {

    PANEL_R0C0(180, 140),
    PANEL_R0C1(180, 140),
    PANEL_R1C0(180, 140),
    PANEL_R1C1(180, 140),
    PANEL_R2C0(180, 140),
    PANEL_R2C1(180, 140),
    /** Sandık menüsünün arka planı; pencerenin tamamını kaplar. */
    MENU_BG(176, 222);

    /** Panelin tamamının ölçüsü (parçalar birleşince). */
    public static final int PANEL_WIDTH = 360;
    public static final int PANEL_HEIGHT = 420;
    public static final int TILE_WIDTH = 180;
    public static final int TILE_HEIGHT = 140;

    private static final int BASE = 0xE200;
    private static final int STRIDE = 512;
    private static final int Y_MIN = -8;
    private static final int Y_MAX = 420;

    private final int width;
    private final int height;

    UiSprite(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * @param y satırın üst kenarına göre yükseklik (aşağı doğru artar)
     * @return bu parçayı o yükseklikte çizen karakter
     */
    char glyph(int y) {
        int clamped = Math.max(Y_MIN, Math.min(Y_MAX, y));
        return (char) (BASE + ordinal() * STRIDE + (clamped - Y_MIN));
    }
}
