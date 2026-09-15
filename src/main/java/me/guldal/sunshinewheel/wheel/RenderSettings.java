package me.guldal.sunshinewheel.wheel;

/** config.yml'deki piksel metin kalibrasyonu, animasyon yumuşatması ve editör ekranı ölçüleri. */
public final class RenderSettings {

    public double textPixelBlocks;
    public double textTopUnits;
    public double textLineUnits;
    public int interpolationTicks;
    public double panelPixelBlocks;
    public double panelDistance;
    /** Editör ekranındaki panel resminin kaydırması (GUI pikseli); /wheel ui ile ayarlanır. */
    public int uiOffsetX;
    public int uiOffsetY;

    public RenderSettings(double textPixelBlocks, double textTopUnits, double textLineUnits, int interpolationTicks,
            double panelPixelBlocks, double panelDistance, int uiOffsetX, int uiOffsetY) {
        this.textPixelBlocks = textPixelBlocks;
        this.textTopUnits = textTopUnits;
        this.textLineUnits = textLineUnits;
        this.interpolationTicks = interpolationTicks;
        this.panelPixelBlocks = panelPixelBlocks;
        this.panelDistance = panelDistance;
        this.uiOffsetX = uiOffsetX;
        this.uiOffsetY = uiOffsetY;
    }

    /** Görüntü merkezini TextDisplay kökenine en çok yaklaştıran boş satır sayısı. */
    public int padLines(int imageHeight) {
        if (textLineUnits <= 0) {
            return 0;
        }
        return Math.max(0, (int) Math.round((imageHeight / 2.0 - textTopUnits) / textLineUnits));
    }

    /** Görüntü merkezinin TextDisplay kökenine göre yerel yüksekliği (blok). */
    public double imageCenterY(int imageHeight, double pixelSize) {
        return (textTopUnits + padLines(imageHeight) * textLineUnits - imageHeight / 2.0) * pixelSize;
    }
}
