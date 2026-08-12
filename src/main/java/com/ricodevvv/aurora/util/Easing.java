package com.ricodevvv.aurora.util;

/** Curvas de interpolacion para que las animaciones no se sientan robotizadas. */
public enum Easing {

    LINEAR {
        public double apply(double t) {
            return t;
        }
    },
    EASE_IN_QUAD {
        public double apply(double t) {
            return t * t;
        }
    },
    EASE_OUT_QUAD {
        public double apply(double t) {
            return t * (2 - t);
        }
    },
    EASE_IN_OUT_QUAD {
        public double apply(double t) {
            return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }
    },
    EASE_OUT_CUBIC {
        public double apply(double t) {
            double f = t - 1;
            return f * f * f + 1;
        }
    },
    EASE_IN_OUT_SINE {
        public double apply(double t) {
            return -(Math.cos(Math.PI * t) - 1) / 2;
        }
    },
    EASE_OUT_ELASTIC {
        public double apply(double t) {
            if (t == 0 || t == 1) return t;
            double p = 0.3;
            return Math.pow(2, -10 * t) * Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1;
        }
    },
    EASE_OUT_BOUNCE {
        public double apply(double t) {
            double n1 = 7.5625, d1 = 2.75;
            if (t < 1 / d1) return n1 * t * t;
            if (t < 2 / d1) return n1 * (t -= 1.5 / d1) * t + 0.75;
            if (t < 2.5 / d1) return n1 * (t -= 2.25 / d1) * t + 0.9375;
            return n1 * (t -= 2.625 / d1) * t + 0.984375;
        }
    };

    public abstract double apply(double t);

    public double between(double from, double to, double t) {
        return from + (to - from) * apply(Math.max(0, Math.min(1, t)));
    }
}
