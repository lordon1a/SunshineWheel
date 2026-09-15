package me.guldal.sunshinewheel.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.guldal.sunshinewheel.render.PixelImage;
import me.guldal.sunshinewheel.render.PixelText;
import me.guldal.sunshinewheel.render.Ui2;
import me.guldal.sunshinewheel.render.WheelArt;
import me.guldal.sunshinewheel.wheel.MotionStyle;
import me.guldal.sunshinewheel.wheel.Reward;
import me.guldal.sunshinewheel.wheel.WheelDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import java.awt.Color;

/**
 * Editör panelinin çizimi ve tıklanabilir alanları.
 *
 * <p>Panel dünyaya konan tek bir TextDisplay'dir. Çizim artık piksel piksel değil, kaynak paketindeki
 * hazır sprite'lar + 6x10 monospace metin glifleriyle yapılır: kare başına ~300 karakter.
 * Konum ve ışın hesabı {@link EditorSession} işidir.
 *
 * <p>Klavye girişi panelde mümkün olmadığı için ad, ses ve komut gibi yazı gerektiren ayarlar
 * dialog ekranına yönlendirilir.
 */
public final class EditorPanel {

    public static final int IMG_W = Ui2.PANEL_W;
    public static final int IMG_H = Ui2.PANEL_H;

    /** Panelde düzenlenebilen sekmeler. */
    private static final String[][] TABS = {
        {"animation", "ANIMASYON"},
        {"appearance", "GORUNUM"},
        {"items", "ODUL ESYALARI"},
        {"cooldown", "BEKLEME"},
        {"particles", "PARCACIK"},
    };

    // Yerleşim (pack/art/generate_ui2.py'deki üretilmiş konumlarla aynı olmalı)
    private static final int SIDEBAR_X = 12;
    private static final int TAB_H = 18, TAB_STEP = 18;
    private static final int CONTENT_X = 140;
    private static final int STEP_MINUS_X = 136, STEP_PLUS_X = 250;
    private static final int ROW1_Y = 36, ROW2_Y = 72, ROW3_Y = 108;
    private static final int VALUE_RIGHT = 264;
    private static final int SLIDER_LABEL_Y = 138, TRACK_X = 140, TRACK_Y = 150, KNOB_Y = 148;
    private static final int INFO_Y = 156;
    private static final int FOOTER_Y = 186, FOOTER_LABEL_Y = 192;
    private static final int PREVIEW_X = 278, PREVIEW_W = 74;
    /** Önizleme görselinin panel içindeki merkezi; ayrı TextDisplay'in konumu bununla hesaplanır. */
    public static final int PREVIEW_CENTER_X = 316;
    public static final int PREVIEW_CENTER_Y = 88;
    public static final int PREVIEW_IMG_SIZE = 56;
    private static final int PREVIEW_IMG_X = 288, PREVIEW_IMG_Y = 60;

    public static final class Button {
        final int x;
        final int y;
        final int w;
        final int h;
        final boolean active;
        final Runnable action;

        Button(int x, int y, int w, int h, boolean active, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.active = active;
            this.action = action;
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public void run() {
            action.run();
        }

        /** Sprite seçiminde hover durumu bu butona aitse vurgulu görünüm kullanılır. */
        boolean isSlider() {
            return h <= 4 || (y == TRACK_Y - 2 && w == 120);
        }
    }

    private final WheelDefinition def;
    private final Runnable onChange;
    private final Runnable onTextTab;
    private final Runnable onSpin;
    private final Runnable onClose;
    private final List<Button> buttons = new ArrayList<>();
    private int tab;

    private Component previewCache;
    private String previewKey = "";
    private double sliderMin = 0;
    private double sliderMax = 1;

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

    /** İmleç panelin içindeyse true. */
    public boolean inside(int px, int py) {
        return px >= 0 && px < IMG_W && py >= 0 && py < IMG_H;
    }

    /** Sekme veya içerik değişince paneli yeniden çizdiren dış çağrı. */
    public void selectTab(int index) {
        if (index >= 0 && index < TABS.length) {
            tab = index;
        }
    }

    public int tab() {
        return tab;
    }

    public WheelDefinition definition() {
        return def;
    }

    // --- yazı gerektiren ayarlar dialog'da düzenlendiği için burada sadece sayı/aç-kapa var ---

    private ItemStack iconOf(Reward reward) {
        return new ItemStack(reward.icon);
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static String stripTags(String miniMessage) {
        return miniMessage.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * Paneli çizer.
     *
     * @param hovered imlecin üstünde durduğu butonun sırası, yoksa -1
     * @param cursorX imlecin panel içindeki konumu (çizilecek), yoksa -1
     */
    public Component render(int hovered, int cursorX, int cursorY) {
        buttons.clear();

        // --- satır içerikleri: her sekmede 2 ayar satırı + 1 kaydırıcı ---
        String[] labels = new String[2];
        String[] values = new String[2];
        Runnable[] minus = new Runnable[2];
        Runnable[] plus = new Runnable[2];
        String sliderLabel;
        double sliderValue;
        double sliderMin;
        double sliderMax;

        switch (TABS[tab][0]) {
            case "appearance" -> {
                labels[0] = "PIKSEL BOYU";
                values[0] = trim(def.pixelSize);
                minus[0] = () -> def.pixelSize = Math.max(0.005, round(def.pixelSize - 0.005));
                plus[0] = () -> def.pixelSize = Math.min(0.2, round(def.pixelSize + 0.005));
                labels[1] = "GORUNTU BOYUTU";
                values[1] = String.valueOf(def.imageSize);
                minus[1] = () -> def.imageSize = Math.max(32, def.imageSize - 8);
                plus[1] = () -> def.imageSize = Math.min(256, def.imageSize + 8);
                sliderLabel = "CAP (BLOK)";
                sliderValue = round(def.imageSize * def.pixelSize);
                sliderMin = 0.5;
                sliderMax = 8.0;
            }
            case "items" -> {
                labels[0] = "ESYA YONU";
                values[0] = def.itemsUpright ? "DIK" : "DONER";
                minus[0] = () -> def.itemsUpright = !def.itemsUpright;
                plus[0] = minus[0];
                labels[1] = "ESYA BOYUTU";
                values[1] = trim(def.itemScale);
                minus[1] = () -> def.itemScale = Math.max(0.05, round(def.itemScale - 0.05));
                plus[1] = () -> def.itemScale = Math.min(2.0, round(def.itemScale + 0.05));
                sliderLabel = "ESYA BOYUTU";
                sliderValue = def.itemScale;
                sliderMin = 0.05;
                sliderMax = 2.0;
            }
            case "cooldown" -> {
                labels[0] = "BEKLEME (SANIYE)";
                values[0] = String.valueOf(def.cooldownSeconds);
                minus[0] = () -> def.cooldownSeconds = Math.max(0, def.cooldownSeconds - 5);
                plus[0] = () -> def.cooldownSeconds = Math.min(3600, def.cooldownSeconds + 5);
                labels[1] = "BILET ADEDI";
                values[1] = String.valueOf(def.ticketAmount);
                minus[1] = () -> def.ticketAmount = Math.max(1, def.ticketAmount - 1);
                plus[1] = () -> def.ticketAmount = Math.min(64, def.ticketAmount + 1);
                sliderLabel = "BEKLEME (SANIYE)";
                sliderValue = def.cooldownSeconds;
                sliderMin = 0;
                sliderMax = 600;
            }
            case "particles" -> {
                labels[0] = "PARCACIK ADEDI";
                values[0] = String.valueOf(def.particleCount);
                minus[0] = () -> def.particleCount = Math.max(0, def.particleCount - 10);
                plus[0] = () -> def.particleCount = Math.min(300, def.particleCount + 10);
                labels[1] = "HOLOGRAM";
                values[1] = def.showHologram ? "ACIK" : "KAPALI";
                minus[1] = () -> def.showHologram = !def.showHologram;
                plus[1] = minus[1];
                sliderLabel = "PARCACIK ADEDI";
                sliderValue = def.particleCount;
                sliderMin = 0;
                sliderMax = 300;
            }
            default -> {
                labels[0] = "TUR SAYISI";
                values[0] = trim(def.revolutions);
                minus[0] = () -> def.revolutions = Math.max(1, round(def.revolutions - 0.5));
                plus[0] = () -> def.revolutions = Math.min(30, round(def.revolutions + 0.5));
                labels[1] = "STIL";
                values[1] = def.style.name();
                minus[1] = () -> def.style = cycleStyle(-1);
                plus[1] = () -> def.style = cycleStyle(1);
                sliderLabel = "SURE (TICK)";
                sliderValue = def.duration;
                sliderMin = 20;
                sliderMax = 600;
            }
        }
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;

        // --- buton yerleşimi (sıra sabit: 0-4 sekme, 5 yazı, 6-9 satırlar, 10 kaydırıcı, 11-13 alt) ---
        for (int i = 0; i < TABS.length; i++) {
            int y = 24 + TAB_STEP * i;
            final int index = i;
            buttons.add(new Button(SIDEBAR_X, y, 112, TAB_H, i == tab, () -> tab = index));
        }
        buttons.add(new Button(SIDEBAR_X, 120, 112, 20, false, onTextTab));
        buttons.add(rowButton(0, true, minus[0]));
        buttons.add(rowButton(0, false, plus[0]));
        buttons.add(rowButton(1, true, minus[1]));
        buttons.add(rowButton(1, false, plus[1]));
        buttons.add(new Button(TRACK_X, TRACK_Y - 2, 120, 10, false, () -> {
            // kaydırıcıya tıklama EditorSession içinde imleç konumuyla işlenir (sliderToCursor)
        }));

        // --- alt butonlar (11-13) ---
        buttons.add(new Button(12, FOOTER_Y, 56, 20, false, onSpin));
        buttons.add(new Button(72, FOOTER_Y, 56, 20, false, onClose));
        buttons.add(new Button(240, FOOTER_Y, 112, 20, false, this::save));

        // --- çizim ---
        Ui2.Canvas canvas = new Ui2.Canvas();
        canvas.sprite(Ui2.Sprite.PANEL_L, 0, 0).sprite(Ui2.Sprite.PANEL_R, 180, 0);
        canvas.text(12, 6, "SUNSHINE WHEEL");
        canvas.textRight(348, 6, def.id);

        for (int i = 0; i < TABS.length; i++) {
            int y = 24 + TAB_STEP * i;
            Ui2.Sprite sprite = i == tab ? Ui2.Sprite.TAB_ON
                    : (hovered == i ? Ui2.Sprite.TAB_HOVER : Ui2.Sprite.TAB_OFF);
            canvas.sprite(sprite, SIDEBAR_X, y);
            canvas.text(SIDEBAR_X + 8, y + 6, TABS[i][1]);
        }
        canvas.sprite(hovered == 5 ? Ui2.Sprite.WIDE_HOVER : Ui2.Sprite.WIDE_OFF, SIDEBAR_X, 120);
        canvas.text(SIDEBAR_X + 8, 126, "YAZI AYARLARI");

        drawRow(canvas, hovered, 0, labels[0], values[0], 6, 7);
        drawRow(canvas, hovered, 1, labels[1], values[1], 8, 9);

        canvas.text(CONTENT_X, SLIDER_LABEL_Y, sliderLabel);
        canvas.textRight(VALUE_RIGHT, SLIDER_LABEL_Y, trim(sliderValue));
        canvas.sprite(Ui2.Sprite.TRACK, TRACK_X, TRACK_Y);
        canvas.sprite(hovered == 10 ? Ui2.Sprite.KNOB_HOVER : Ui2.Sprite.KNOB,
                knobX(sliderValue, sliderMin, sliderMax), KNOB_Y);
        canvas.text(CONTENT_X, INFO_Y, infoLine());

        canvas.sprite(hovered == 11 ? Ui2.Sprite.SMALL_HOVER : Ui2.Sprite.SMALL_OFF, 12, FOOTER_Y);
        canvas.textCentered(40, FOOTER_LABEL_Y, "CEVIR");
        canvas.sprite(hovered == 12 ? Ui2.Sprite.SMALL_HOVER : Ui2.Sprite.SMALL_OFF, 72, FOOTER_Y);
        canvas.textCentered(100, FOOTER_LABEL_Y, "KAPAT");
        canvas.sprite(hovered == 13 ? Ui2.Sprite.SAVE_HOVER : Ui2.Sprite.SAVE_OFF, 240, FOOTER_Y);
        canvas.textCentered(296, FOOTER_LABEL_Y, "KAYDET");

        canvas.textCentered(PREVIEW_X + PREVIEW_W / 2, 36, "ONIZLEME");
        canvas.textCentered(PREVIEW_X + PREVIEW_W / 2, INFO_Y, def.rewards.size() + " ODUL");

        if (cursorX >= 0 && cursorY >= 0) {
            canvas.crosshair(cursorX, cursorY);
        }
        return canvas.build();
    }

    /** Panel içi imleç konumuna göre kaydırıcıyı ayarlar (sol tık). */
    public void sliderToCursor(int cursorX) {
        double usable = 120 - Ui2.Sprite.KNOB.width();
        applySliderValue((cursorX - TRACK_X) / usable);
    }

    /** Fare tekerleği: imleç kaydırıcı üzerindeyken ince ayar yapar. */
    public void scroll(int cursorX, int cursorY, int direction) {
        if (cursorY >= TRACK_Y - 14 && cursorY <= TRACK_Y + 14 && cursorX >= TRACK_X - 8
                && cursorX <= TRACK_X + 128) {
            applySliderValue(currentFraction() + direction * 0.02);
        }
    }

    private int knobX(double value, double min, double max) {
        double fraction = max <= min ? 0 : (value - min) / (max - min);
        fraction = Math.max(0, Math.min(1, fraction));
        return TRACK_X + (int) Math.round(fraction * (120 - Ui2.Sprite.KNOB.width()));
    }

    /** Kaydırıcının bağlı olduğu alanı sekmeden bulup değeri yazar. */
    private void applySliderValue(double rawFraction) {
        double fraction = Math.max(0, Math.min(1, rawFraction));
        switch (TABS[tab][0]) {
            case "appearance" -> def.pixelSize = Math.max(0.005,
                    round((sliderMin + fraction * (sliderMax - sliderMin)) / Math.max(1, def.imageSize)));
            case "items" -> def.itemScale = round(sliderMin + fraction * (sliderMax - sliderMin));
            case "particles" -> def.particleCount = (int) Math.round(sliderMin + fraction * (sliderMax - sliderMin));
            case "cooldown" -> def.cooldownSeconds = (int) Math.round(sliderMin + fraction * (sliderMax - sliderMin));
            default -> def.duration = (int) Math.round(sliderMin + fraction * (sliderMax - sliderMin));
        }
        onChange.run();
    }

    private double currentFraction() {
        return switch (TABS[tab][0]) {
            case "appearance" -> (def.imageSize * def.pixelSize - sliderMin) / (sliderMax - sliderMin);
            case "items" -> (def.itemScale - sliderMin) / (sliderMax - sliderMin);
            case "particles" -> (def.particleCount - sliderMin) / (sliderMax - sliderMin);
            case "cooldown" -> (def.cooldownSeconds - sliderMin) / (sliderMax - sliderMin);
            default -> (def.duration - sliderMin) / (sliderMax - sliderMin);
        };
    }

    /** Satır başındaki ◄ / ► butonları. */
    private Button rowButton(int row, boolean left, Runnable action) {
        int y = (row == 0 ? ROW1_Y : ROW2_Y) + 6;
        return new Button(left ? STEP_MINUS_X : STEP_PLUS_X, y, 14, 14, false, () -> {
            action.run();
            onChange.run();
        });
    }

    private void drawRow(Ui2.Canvas canvas, int hovered, int row, String label, String value, int minusIndex,
            int plusIndex) {
        int y = row == 0 ? ROW1_Y : ROW2_Y;
        canvas.sprite(hovered == minusIndex ? Ui2.Sprite.STEP_HOVER : Ui2.Sprite.STEP_OFF, STEP_MINUS_X, y + 6);
        canvas.sprite(hovered == plusIndex ? Ui2.Sprite.STEP_HOVER : Ui2.Sprite.STEP_OFF, STEP_PLUS_X, y + 6);
        canvas.text(CONTENT_X, y, label);
        canvas.textRight(VALUE_RIGHT, y + 12, value);
    }

    private void save() {
        onChange.run();
        onSpin.run();
    }

    private String infoLine() {
        if (def.rewards.isEmpty()) {
            return "ODUL YOK - YAZI AYARLARI";
        }
        double total = def.rewards.stream().mapToDouble(r -> Math.max(0, r.weight)).sum();
        return "TOPLAM AGIRLIK " + trim(total) + " / " + def.rewards.size() + " ODUL";
    }

    /**
     * Çark önizlemesi. Renkler ve boyut değişmedikçe yeniden kodlanmaz; bu yüzden fare hareketi
     * önizlemeyi yeniden üretmez, kodlanmış bileşen olduğu gibi tekrar kullanılır.
     */
    public Component previewComponent() {
        List<Integer> colors = def.rewards.stream().map(r -> r.color).toList();
        String key = colors + "|" + PREVIEW_IMG_SIZE;
        if (previewCache == null || !key.equals(previewKey)) {
            PixelImage image = colors.isEmpty() ? emptyPreview() : WheelArt.wheel(colors, PREVIEW_IMG_SIZE);
            previewCache = PixelText.encodeAt(image, 0, 0);
            previewKey = key;
        }
        return previewCache;
    }

    private static PixelImage emptyPreview() {
        PixelImage image = new PixelImage(PREVIEW_IMG_SIZE, PREVIEW_IMG_SIZE);
        image.fillRect(0, 0, PREVIEW_IMG_SIZE, PREVIEW_IMG_SIZE, 0xFF2A2350);
        return image;
    }

    private MotionStyle cycleStyle(int direction) {
        MotionStyle[] values = MotionStyle.values();
        return values[(def.style.ordinal() + direction + values.length) % values.length];
    }
}