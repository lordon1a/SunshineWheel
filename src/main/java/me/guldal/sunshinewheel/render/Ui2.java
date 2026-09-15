package me.guldal.sunshinewheel.render;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

/**
 * Editör panelinin yeni çizim katmanı: tek tek piksel yerine <b>hazır sprite + gerçek font</b>.
 *
 * <p>Eski yol ({@link PixelText}) 280x170 panel için kare başına ~96.000 karakter (~290 KB) üretiyordu.
 * Bu sınıf aynı paneli ~300 karakterde çizer: arka plan 2 parça, widget'lar tek glif, yazılar 6x10
 * monospace glifler.
 *
 * <p>Karakter aralıkları ve dikey konum listeleri {@code pack/generate_pack2.py} ile birebir aynı olmalı:
 * <ul>
 *   <li>U+E000+i / U+E020+i : ±2^i boşluk (px ve ui fontlarıyla aynı)</li>
 *   <li>U+E900 + slot*64 + harf : metin glifi (slot = {@link #TEXT_Y} sırası, ascent = 1 - y)</li>
 *   <li>U+F100 + sprite*32 + slot : widget (slot = {@link #SPRITE_Y} sırası, ascent = 1 - y)</li>
 * </ul>
 */
public final class Ui2 {

    public static final Key FONT = Key.key("sunshinewheel", "ui2");

    /** Panel ölçüsü (pack/art/generate_ui2.py ile aynı). */
    public static final int PANEL_W = 360;
    public static final int PANEL_H = 220;

    /** Metin gliflerinin üretildiği dikey konumlar. Sadece bu satırlara yazı yazılabilir. */
    public static final int[] TEXT_Y = {6, 30, 36, 48, 66, 72, 84, 102, 108, 120, 126, 138, 156, 192};

    /** Metin gliflerinde kullanılabilen karakter kümesi (küçük harfler büyüğe çevrilir). */
    private static final String CHARS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,:-+%()/!<>#=_*";

    /** Metin glifi ilerlemesi: hücre 6 px + 1 px boşluk. */
    public static final int TEXT_ADVANCE = 7;

    private static final int TEXT_BASE = 0xE900;
    private static final int TEXT_STRIDE = 64;
    private static final int ART_BASE = 0xF100;
    private static final int ART_STRIDE = 32;

    /** Pack'teki widget'lar; sıra generate_pack2.py SPRITES listesiyle aynı olmalı. */
    public enum Sprite {
        PANEL_L(180, 220, 0),
        PANEL_R(180, 220, 0),
        TAB_OFF(112, 18, 24, 42, 60, 78, 96),
        TAB_ON(112, 18, 24, 42, 60, 78, 96),
        TAB_HOVER(112, 18, 24, 42, 60, 78, 96),
        WIDE_OFF(112, 20, 120),
        WIDE_HOVER(112, 20, 120),
        SMALL_OFF(56, 20, 186),
        SMALL_HOVER(56, 20, 186),
        SAVE_OFF(112, 20, 186),
        SAVE_HOVER(112, 20, 186),
        STEP_OFF(14, 14, 42, 78, 114),
        STEP_HOVER(14, 14, 42, 78, 114),
        TRACK(120, 6, 150),
        KNOB(8, 10, 148),
        KNOB_HOVER(8, 10, 148);

        private final int width;
        private final int height;
        private final int[] slots;

        Sprite(int width, int height, int... slots) {
            this.width = width;
            this.height = height;
            this.slots = slots;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        /** Panel y konumuna karşılık gelen glif; üretilmemiş konumlar için en yakın slot kullanılır. */
        char glyph(int y) {
            int best = 0;
            for (int i = 1; i < slots.length; i++) {
                if (Math.abs(slots[i] - y) < Math.abs(slots[best] - y)) {
                    best = i;
                }
            }
            return (char) (ART_BASE + ordinal() * ART_STRIDE + best);
        }
    }

    private Ui2() {
    }

    public static char textGlyph(char raw, int y) {
        int slot = nearestSlot(y);
        int index = CHARS.indexOf(upper(raw));
        return (char) (TEXT_BASE + slot * TEXT_STRIDE + Math.max(0, index));
    }

    private static int nearestSlot(int y) {
        int best = 0;
        for (int i = 1; i < TEXT_Y.length; i++) {
            if (Math.abs(TEXT_Y[i] - y) < Math.abs(TEXT_Y[best] - y)) {
                best = i;
            }
        }
        return best;
    }

    private static char upper(char c) {
        return switch (c) {
            case 'ı', 'İ', 'i' -> 'I';
            case 'ğ', 'Ğ' -> 'G';
            case 'ü', 'Ü' -> 'U';
            case 'ş', 'Ş' -> 'S';
            case 'ö', 'Ö' -> 'O';
            case 'ç', 'Ç' -> 'C';
            case 'â' -> 'A';
            default -> Character.toUpperCase(c);
        };
    }

    /**
     * Tek satırlık panel tuvali: sprite'lar, yazılar ve çalışma anında üretilen görüntüler aynı satıra
     * mutlak x/y ile yerleştirilir. Satırın net ilerlemesi sıfır bırakılır; oyun satırı ortalarken
     * panel kaymaz.
     */
    public static final class Canvas {

        private final TextComponent.Builder root = Component.text().font(FONT);
        private final StringBuilder buffer = new StringBuilder();
        private Key bufferFont = FONT;
        private int cursor;

        /** İmleci mutlak x konumuna taşır. */
        public Canvas at(int x) {
            write(PixelText.space(x - cursor), FONT);
            cursor = x;
            return this;
        }

        public Canvas sprite(Sprite sprite, int x, int y) {
            at(x);
            write(String.valueOf(sprite.glyph(y)), FONT);
            cursor = x + sprite.width() + 1;   // bitmap glifin ilerlemesi: genişlik + 1
            return this;
        }

        /** y, {@link #TEXT_Y} listesindeki en yakın konuma yuvarlanır. */
        public Canvas text(int x, int y, String value) {
            at(x);
            String ascii = ascii(value);
            StringBuilder out = new StringBuilder(ascii.length());
            for (char c : ascii.toCharArray()) {
                out.append(textGlyph(c, y));
            }
            write(out.toString(), FONT);
            cursor = x + ascii.length() * TEXT_ADVANCE;
            return this;
        }

        public Canvas textRight(int right, int y, String value) {
            return text(right - ascii(value).length() * TEXT_ADVANCE, y, value);
        }

        public Canvas textCentered(int centerX, int y, String value) {
            return text(centerX - ascii(value).length() * TEXT_ADVANCE / 2, y, value);
        }

        /**
         * Çalışma anında üretilmiş görüntüyü (çark önizlemesi) panel içinde verilen konuma koyar.
         * Net ilerlemesi sıfırdır; yalnızca değer değişince yeniden üretilmelidir.
         */
        public Canvas image(PixelImage image, int x, int y) {
            flush();
            root.append(PixelText.encodeAt(image, x, y));
            return this;
        }

        /** Önceden kodlanmış bir bileşeni (ör. cache'lenmiş çark önizlemesi) satıra ekler. */
        public Canvas child(Component component) {
            flush();
            root.append(component);
            return this;
        }

        /** Basit artı işareti imleç (px fontuyla, ~20 karakter). */
        public Canvas crosshair(int x, int y) {
            flush();
            PixelImage mark = new PixelImage(9, 9);
            mark.fillRect(0, 4, 9, 1, 0xFFF2B544);
            mark.fillRect(4, 0, 1, 9, 0xFFF2B544);
            root.append(PixelText.encodeAt(mark, x - 4, y - 4));
            return this;
        }

        public Component build() {
            write(PixelText.space(-cursor), FONT);
            flush();
            return root.build();
        }

        private void write(String value, Key font) {
            if (!font.equals(bufferFont)) {
                flush();
                bufferFont = font;
            }
            buffer.append(value);
        }

        private void flush() {
            if (buffer.length() == 0) {
                return;
            }
            root.append(Component.text(buffer.toString()).font(bufferFont));
            buffer.setLength(0);
        }
    }

    /** Küçük harf ve Türkçe karakterleri glif kümesine çevirir; bilinmeyen karakter boşluk olur. */
    public static String ascii(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            char m = upper(c);
            out.append(CHARS.indexOf(m) >= 0 ? m : ' ');
        }
        return out.toString();
    }
}