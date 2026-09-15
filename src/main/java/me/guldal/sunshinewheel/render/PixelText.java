package me.guldal.sunshinewheel.render;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

/**
 * Bir {@link PixelImage}'ı tek bir TextDisplay'de gösterilebilecek metne çevirir.
 *
 * <p>Kaynak paketindeki {@code sunshinewheel:px} fontuna dayanır (pack/generate_pack.py üretir):
 * <ul>
 *   <li>U+E100+r: r. satırın pikseli (1x1 beyaz, ascent = 1 - r). Rengi metin rengiyle verilir.</li>
 *   <li>U+E000+i: -2^i boşluk, U+E020+i: +2^i boşluk (i = 0..10).</li>
 * </ul>
 * Bitmap glifin ilerlemesi genişlik + 1 = 2 olduğu için her pikselden sonra -1 boşluk eklenir.
 * Satırlar glif yüksekliğiyle konumlandığından her satır sonunda imleç satır başına geri çekilir.
 */
public final class PixelText {

    public static final Key FONT = Key.key("sunshinewheel", "px");
    public static final int MAX_ROWS = 256;

    private static final char ROW_GLYPH_BASE = '';
    private static final char NEG_SPACE_BASE = '';
    private static final char POS_SPACE_BASE = '';
    private static final int MAX_SPACE_BITS = 11;

    private PixelText() {
    }

    public static Component encode(PixelImage image) {
        return encode(image, 0, false);
    }

    public static Component encode(PixelImage image, int padLines) {
        return encode(image, padLines, false);
    }

    /**
     * @param padLines görüntünün altına eklenecek boş satır sayısı. TextDisplay metni kökenden yukarı doğru
     *                 büyüdüğü için bu satırlar görüntüyü yukarı iter; görüntü merkezini kökene yaklaştırıp
     *                 dönüşün translation değiştirmeden yapılmasını sağlar.
     * @param centered net genişliği sıfır bırakır: önce yarım genişlik sola kayar, çizer, sonra başlangıca döner.
     *                 Menü ekranlarında gerekir, çünkü oyun satır genişliğini ölçerken geri gidişleri saymayıp
     *                 resmi sağa kaydırır; net genişlik sıfır olunca resim her zaman ortada çizilir.
     */
    public static Component encode(PixelImage image, int padLines, boolean centered) {
        if (image.height > MAX_ROWS) {
            throw new IllegalArgumentException("Görüntü en fazla " + MAX_ROWS + " satır olabilir: " + image.height);
        }
        if (image.width >= (1 << MAX_SPACE_BITS)) {
            throw new IllegalArgumentException("Görüntü çok geniş: " + image.width);
        }

        TextComponent.Builder root = Component.text().font(FONT);
        StringBuilder spaces = new StringBuilder();
        int cursor = 0;

        int half = image.width / 2;
        if (centered) {
            appendSpace(spaces, -half);
        }

        for (int y = 0; y < image.height; y++) {
            char glyph = (char) (ROW_GLYPH_BASE + y);
            int pendingTransparent = 0;
            int x = 0;
            while (x < image.width) {
                int color = image.get(x, y);
                int run = 1;
                while (x + run < image.width && image.get(x + run, y) == color) {
                    run++;
                }
                if (PixelImage.isTransparent(color)) {
                    pendingTransparent += run;
                } else {
                    if (pendingTransparent > 0) {
                        appendSpace(spaces, pendingTransparent);
                        cursor += pendingTransparent;
                        pendingTransparent = 0;
                    }
                    flush(root, spaces);
                    StringBuilder pixels = new StringBuilder(run * 2);
                    for (int i = 0; i < run; i++) {
                        pixels.append(glyph).append(NEG_SPACE_BASE);
                    }
                    root.append(Component.text(pixels.toString(), TextColor.color(color & 0xFFFFFF)));
                    cursor += run;
                }
                x += run;
            }
            if (cursor > 0) {
                appendSpace(spaces, -cursor);
                cursor = 0;
            }
        }

        // TextDisplay'de net genişlik görüntü genişliğine eşit olmalı ki yatayda doğru ortalansın;
        // menü ekranlarında ise başlangıçtaki kaydırma geri alınıp net genişlik sıfır bırakılır.
        appendSpace(spaces, centered ? half : image.width);
        flush(root, spaces);

        // Boş satırların yüksekliği sayılsın diye her satıra net ilerlemesi 0 olan bir boşluk çifti konur.
        for (int i = 0; i < padLines; i++) {
            root.append(Component.text("\n" + (char) POS_SPACE_BASE + NEG_SPACE_BASE));
        }
        return root.build();
    }

    /** Verilen kadar yatay ilerleme sağlayan boşluk karakterleri (negatif değer geri alır). */
    public static String space(int advance) {
        StringBuilder out = new StringBuilder();
        appendSpace(out, advance);
        return out.toString();
    }

    private static void appendSpace(StringBuilder out, int advance) {
        int abs = Math.abs(advance);
        char base = advance < 0 ? NEG_SPACE_BASE : POS_SPACE_BASE;
        for (int bit = 0; bit < MAX_SPACE_BITS; bit++) {
            if (((abs >> bit) & 1) == 1) {
                out.append((char) (base + bit));
            }
        }
    }

    private static void flush(TextComponent.Builder root, StringBuilder spaces) {
        if (!spaces.isEmpty()) {
            root.append(Component.text(spaces.toString()));
            spaces.setLength(0);
        }
    }
}
