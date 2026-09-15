package me.guldal.sunshinewheel.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.guldal.sunshinewheel.render.Glyphs;
import me.guldal.sunshinewheel.render.PixelImage;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelDefinition;

/**
 * Editör panelinin çizimi ve tıklanabilir alanları.
 *
 * <p>Panel dünyaya konan tek bir TextDisplay'dir; burada sadece piksel çizimi ve buton koordinatları
 * tutulur, konum/ışın hesabı {@link EditorSession} işidir.
 *
 * <p>Klavye girişi panelde mümkün olmadığı için ad, ses ve komut gibi yazı gerektiren ayarlar
 * dialog ekranına yönlendirilir.
 */
public final class EditorPanel {

    public static final int IMG_W = 280;
    public static final int IMG_H = 170;

    private static final int COL_PANEL = 0x171233;
    private static final int COL_BAR = 0x120E26;
    private static final int COL_BORDER = 0x2A2350;
    private static final int COL_TEXT = 0xE8E4F0;
    private static final int COL_DIM = 0x9C93B8;
    private static final int COL_ACCENT = 0xF2B544;
    private static final int COL_BTN = 0x241D45;
    private static final int COL_BTN_HOVER = 0x4A3C70;
    private static final int COL_TAB_ACTIVE = 0x3D2F12;

    /** Panelde düzenlenebilen sekmeler (sayı ve aç/kapa ayarları). */
    private static final String[][] TABS = {
        {"animation", "ANIMASYON"},
        {"appearance", "GORUNUM"},
        {"items", "ODUL ESYALARI"},
        {"cooldown", "BEKLEME"},
        {"particles", "PARCACIK"},
    };

    public static final class Button {
        final int x;
        final int y;
        final int w;
        final int h;
        final String label;
        final boolean active;
        final Runnable action;

        Button(int x, int y, int w, int h, String label, boolean active, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
            this.active = active;
            this.action = action;
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public void run() {
            action.run();
        }
    }

    private final WheelDefinition def;
    private final Runnable onChange;
    private final Runnable onTextTab;
    private final Runnable onSpin;
    private final Runnable onClose;
    private final List<Button> buttons = new ArrayList<>();
    private int tab;

    public EditorPanel(WheelDefinition def, Runnable onChange, Runnable onTextTab, Runnable onSpin, Runnable onClose) {
        this.def = def;
        this.onChange = onChange;
        this.onTextTab = onTextTab;
        this.onSpin = onSpin;
        this.onClose = onClose;
    }

    public int hitTest(int px, int py) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).contains(px, py)) {
                return i;
            }
        }
        return -1;
    }

    public Button button(int index) {
        return index >= 0 && index < buttons.size() ? buttons.get(index) : null;
    }

    /**
     * @param hovered imlecin üstünde durduğu butonun sırası, yoksa -1
     * @param cursorX imlecin panel içindeki konumu (çizilecek), yoksa -1
     */
    public PixelImage render(int hovered, int cursorX, int cursorY) {
        buttons.clear();
        PixelImage img = new PixelImage(IMG_W, IMG_H);
        img.fillRect(0, 0, IMG_W, IMG_H, 0xFF000000 | COL_PANEL);
        img.border(0, 0, IMG_W, IMG_H, 0xFF000000 | COL_BORDER);

        // Başlık çubuğu
        img.fillRect(1, 1, IMG_W - 2, 16, 0xFF000000 | COL_BAR);
        Glyphs.draw(img, 6, 6, "SUNSHINE WHEEL", COL_ACCENT, 1);
        String id = Glyphs.ascii(def.id);
        Glyphs.draw(img, IMG_W - 6 - Glyphs.textWidth(id, 1), 6, id, COL_DIM, 1);

        // Sol sekme sütunu
        int ty = 24;
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            buttons.add(new Button(6, ty, 78, 15, TABS[i][1], i == tab, () -> tab = index));
            ty += 18;
        }
        buttons.add(new Button(6, ty + 4, 78, 15, "YAZI AYARLARI", false, onTextTab));
        buttons.add(new Button(6, IMG_H - 22, 37, 15, "CEVIR", false, onSpin));
        buttons.add(new Button(47, IMG_H - 22, 37, 15, "KAPAT", false, onClose));

        // İçerik
        switch (TABS[tab][0]) {
            case "appearance" -> {
                stepper(img, 26, "PIKSEL BOYU", trim(def.pixelSize),
                        () -> def.pixelSize = Math.max(0.005, round(def.pixelSize - 0.005)),
                        () -> def.pixelSize = Math.min(0.2, round(def.pixelSize + 0.005)));
                stepper(img, 56, "GORUNTU BOYUTU", String.valueOf(def.imageSize),
                        () -> def.imageSize = Math.max(32, def.imageSize - 8),
                        () -> def.imageSize = Math.min(256, def.imageSize + 8));
                info(img, 92, "CAP: " + trim(def.imageSize * def.pixelSize) + " BLOK");
            }
            case "items" -> {
                stepper(img, 26, "ESYA BOYUTU", trim(def.itemScale),
                        () -> def.itemScale = Math.max(0.05, round(def.itemScale - 0.05)),
                        () -> def.itemScale = Math.min(2.0, round(def.itemScale + 0.05)));
                buttons.add(new Button(96, 62, 120, 15, def.itemsUpright ? "ESYALAR DIK" : "ESYALAR DONER", false,
                        () -> {
                            def.itemsUpright = !def.itemsUpright;
                            onChange.run();
                        }));
                info(img, 92, "DILIMLERIN USTUNDEKI ESYALAR");
            }
            case "cooldown" -> {
                stepper(img, 26, "BEKLEME (SANIYE)", String.valueOf(def.cooldownSeconds),
                        () -> def.cooldownSeconds = Math.max(0, def.cooldownSeconds - 5),
                        () -> def.cooldownSeconds = Math.min(3600, def.cooldownSeconds + 5));
                info(img, 62, def.cooldownSeconds == 0 ? "KAPALI" : "AYNI OYUNCU ICIN");
            }
            case "particles" -> {
                stepper(img, 26, "PARCACIK ADEDI", String.valueOf(def.particleCount),
                        () -> def.particleCount = Math.max(0, def.particleCount - 10),
                        () -> def.particleCount = Math.min(300, def.particleCount + 10));
                info(img, 62, Glyphs.ascii(def.particleWin));
                info(img, 78, "TURU YAZI AYARLARINDAN");
            }
            default -> {
                stepper(img, 26, "SURE (TICK)", String.valueOf(def.duration),
                        () -> def.duration = Math.max(20, def.duration - 10),
                        () -> def.duration = Math.min(600, def.duration + 10));
                stepper(img, 56, "TUR SAYISI", trim(def.revolutions),
                        () -> def.revolutions = Math.max(1, def.revolutions - 0.5),
                        () -> def.revolutions = Math.min(30, def.revolutions + 0.5));
                stepper(img, 86, "STIL", def.style.name(),
                        () -> def.style = cycleStyle(-1),
                        () -> def.style = cycleStyle(1));
            }
        }

        // Ödül özeti (sağ alt)
        double total = def.rewards.stream().mapToDouble(r -> Math.max(0, r.weight)).sum();
        int ry = 118;
        Glyphs.draw(img, 96, ry - 12, "ODULLER (" + def.rewards.size() + ")", COL_ACCENT, 1);
        for (int i = 0; i < def.rewards.size() && i < 4; i++) {
            Reward reward = def.rewards.get(i);
            String percent = total <= 0 ? "-" : Math.round(reward.weight / total * 100) + "%";
            Glyphs.draw(img, 96, ry, clip(strip(reward.name), 20), COL_TEXT, 1);
            Glyphs.draw(img, IMG_W - 10 - Glyphs.textWidth(percent, 1), ry, percent, COL_DIM, 1);
            img.fillRect(90, ry, 3, 7, 0xFF000000 | reward.color);
            ry += 11;
        }

        // Butonlar en son çizilir ki yazıların üstünde kalsınlar
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            int bg = i == hovered ? COL_BTN_HOVER : (b.active ? COL_TAB_ACTIVE : COL_BTN);
            img.fillRect(b.x, b.y, b.w, b.h, 0xFF000000 | bg);
            img.border(b.x, b.y, b.w, b.h, 0xFF000000 | (i == hovered || b.active ? COL_ACCENT : COL_BORDER));
            Glyphs.drawCentered(img, b.x + b.w / 2, b.y + (b.h - Glyphs.HEIGHT) / 2, b.label,
                    b.active ? COL_ACCENT : COL_TEXT, 1);
        }

        // İmleç
        if (cursorX >= 0 && cursorY >= 0) {
            img.fillRect(cursorX - 3, cursorY, 7, 1, 0xFF000000 | COL_ACCENT);
            img.fillRect(cursorX, cursorY - 3, 1, 7, 0xFF000000 | COL_ACCENT);
        }
        return img;
    }

    private void info(PixelImage img, int y, String text) {
        Glyphs.draw(img, 96, y, text, COL_DIM, 1);
    }

    /** Etiket + [<] değer [>] satırı. */
    private void stepper(PixelImage img, int y, String label, String value, Runnable minus, Runnable plus) {
        Glyphs.draw(img, 96, y, label, COL_DIM, 1);
        Glyphs.drawCentered(img, 160, y + 14, value, COL_TEXT, 1);
        buttons.add(new Button(96, y + 9, 16, 15, "<", false, () -> {
            minus.run();
            onChange.run();
        }));
        buttons.add(new Button(208, y + 9, 16, 15, ">", false, () -> {
            plus.run();
            onChange.run();
        }));
    }

    private MotionStyle cycleStyle(int direction) {
        MotionStyle[] values = MotionStyle.values();
        return values[(def.style.ordinal() + direction + values.length) % values.length];
    }

    private static String strip(String miniMessage) {
        return miniMessage.replaceAll("<[^>]+>", "").trim();
    }

    private static String clip(String text, int max) {
        String ascii = Glyphs.ascii(text);
        return ascii.length() <= max ? ascii : ascii.substring(0, max);
    }

    private static String trim(double value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
