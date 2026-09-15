package me.guldal.sunshinewheel.render;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Hazır arayüz resimlerini ekranda istediğin yere koyan araç.
 *
 * <p>Kullanımı:
 * <pre>
 *   Component art = new UiCanvas(offsetX, offsetY).drawPanel(0).build();
 * </pre>
 *
 * <p>Nasıl çalışıyor:
 * <ul>
 *   <li><b>Yatay:</b> Kaynak paketindeki boşluk karakterleri negatif olabilir, yani imleç geri alınıp
 *       istenen sütuna gidilebilir.</li>
 *   <li><b>Dikey:</b> Her resmin her yükseklik için ayrı bir karakteri var (ascent varyantı).
 *       Satır atlanmadığı için çizim dialog içinde <b>yer kaplamaz</b>, sadece arkada durur.</li>
 * </ul>
 *
 * <p>Satırın net ilerlemesi sıfır bırakılır, böylece oyun satırı ortalarken resmi kaydırmaz.
 * Bu sınıf çarka özel değildir; sonraki menülerde de aynen kullanılabilir.
 */
public final class UiCanvas {

    public static final Key FONT = Key.key("sunshinewheel", "ui");

    private record Draw(UiSprite sprite, int x, int y) {
    }

    private final List<Draw> draws = new ArrayList<>();
    private final int offsetX;
    private final int offsetY;

    public UiCanvas() {
        this(0, 0);
    }

    public UiCanvas(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /**
     * @param x yatay konum; 0 ekranın ortasıdır, negatif değer sola gider
     * @param y dikey konum; 0 bu metin satırının üst kenarıdır, aşağı doğru artar
     */
    public UiCanvas draw(UiSprite sprite, int x, int y) {
        draws.add(new Draw(sprite, x + offsetX, y + offsetY));
        return this;
    }

    /** Panelin altı parçasını (2 sütun x 3 satır), ortalanmış olarak yerleştirir. */
    public UiCanvas drawPanel(int y) {
        int left = -UiSprite.PANEL_WIDTH / 2;
        UiSprite[][] tiles = {
            {UiSprite.PANEL_R0C0, UiSprite.PANEL_R0C1},
            {UiSprite.PANEL_R1C0, UiSprite.PANEL_R1C1},
            {UiSprite.PANEL_R2C0, UiSprite.PANEL_R2C1},
        };
        for (int row = 0; row < tiles.length; row++) {
            for (int col = 0; col < tiles[row].length; col++) {
                draw(tiles[row][col], left + col * UiSprite.TILE_WIDTH, y + row * UiSprite.TILE_HEIGHT);
            }
        }
        return this;
    }

    public Component build() {
        StringBuilder text = new StringBuilder();
        int cursor = 0;
        for (Draw draw : draws) {
            text.append(PixelText.space(draw.x() - cursor));
            text.append(draw.sprite().glyph(draw.y()));
            // Bitmap glifin ilerlemesi genişlik + 1'dir.
            cursor = draw.x() + draw.sprite().width() + 1;
        }
        text.append(PixelText.space(-cursor));
        return Component.text().font(FONT).append(Component.text(text.toString())).build();
    }
}
