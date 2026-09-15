package me.guldal.sunshinewheel.wheel;

/** Dönüş ilerleme eğrileri: t (0..1 zaman) -&gt; toplam açının oranı. Hepsi t=1'de tam 1 verir. */
public enum MotionStyle {
    DECELERATE("Yavaşlayarak") {
        @Override
        public double progress(double t) {
            return 1 - Math.pow(1 - t, 3);
        }
    },
    ACCEL_DECEL("Hızlan + yavaşla") {
        @Override
        public double progress(double t) {
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }
    },
    LINEAR("Sabit hız") {
        @Override
        public double progress(double t) {
            return t;
        }
    },
    MOMENTUM("Momentum") {
        @Override
        public double progress(double t) {
            // Hedefi biraz geçip geri oturur.
            double c1 = 1.2;
            double c3 = c1 + 1;
            double u = t - 1;
            return 1 + c3 * u * u * u + c1 * u * u;
        }
    },
    PULSE("Nabız") {
        @Override
        public double progress(double t) {
            // Yavaşlayan dönüşe hız dalgaları ekler; türev hep pozitif kalır.
            double base = DECELERATE.progress(t);
            double k = 6;
            return base - 0.8 * Math.sin(2 * Math.PI * k * base) / (2 * Math.PI * k);
        }
    };

    private final String label;

    MotionStyle(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public abstract double progress(double t);
}
