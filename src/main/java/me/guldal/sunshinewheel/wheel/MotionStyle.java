package me.guldal.sunshinewheel.wheel;

/** Spin progress curves: t (0..1 time) -&gt; fraction of the total angle. All return exactly 1 at t=1. */
public enum MotionStyle {
    DECELERATE("Ease out") {
        @Override
        public double progress(double t) {
            return 1 - Math.pow(1 - t, 3);
        }
    },
    ACCEL_DECEL("Ease in-out") {
        @Override
        public double progress(double t) {
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }
    },
    LINEAR("Linear") {
        @Override
        public double progress(double t) {
            return t;
        }
    },
    MOMENTUM("Momentum") {
        @Override
        public double progress(double t) {
            // Overshoots the target slightly, then settles back.
            double c1 = 1.2;
            double c3 = c1 + 1;
            double u = t - 1;
            return 1 + c3 * u * u * u + c1 * u * u;
        }
    },
    PULSE("Pulse") {
        @Override
        public double progress(double t) {
            // Adds speed waves to a decelerating spin; derivative stays positive.
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
