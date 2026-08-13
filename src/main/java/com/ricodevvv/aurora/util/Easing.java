package com.ricodevvv.aurora.util;

/**
 * Interpolation curves, used to keep animations from looking mechanical.
 *
 * <p>Every curve maps a normalised progress value to an eased one. Linear
 * motion reads as robotic to the eye; easing is what makes a ring expand like
 * an impact rather than like a spreadsheet.
 *
 * <pre>{@code
 * double radius = Easing.EASE_OUT_CUBIC.between(0.5, 6.0, progress());
 * }</pre>
 */
public enum Easing {

    /** No easing: progress passes through unchanged. */
    LINEAR {
        @Override
        public double apply(double progress) {
            return progress;
        }
    },

    /** Starts slow and accelerates. Good for things being pulled in. */
    EASE_IN_QUAD {
        @Override
        public double apply(double progress) {
            return progress * progress;
        }
    },

    /** Starts fast and decelerates. The default choice for expanding shapes. */
    EASE_OUT_QUAD {
        @Override
        public double apply(double progress) {
            return progress * (2 - progress);
        }
    },

    /** Accelerates then decelerates. Reads as a natural start-to-stop motion. */
    EASE_IN_OUT_QUAD {
        @Override
        public double apply(double progress) {
            return progress < 0.5
                    ? 2 * progress * progress
                    : -1 + (4 - 2 * progress) * progress;
        }
    },

    /** A stronger deceleration than {@link #EASE_OUT_QUAD}. */
    EASE_OUT_CUBIC {
        @Override
        public double apply(double progress) {
            double shifted = progress - 1;
            return shifted * shifted * shifted + 1;
        }
    },

    /** Smooth on both ends, based on a cosine curve. Good for breathing pulses. */
    EASE_IN_OUT_SINE {
        @Override
        public double apply(double progress) {
            return -(Math.cos(Math.PI * progress) - 1) / 2;
        }
    },

    /**
     * Overshoots the target and springs back. Note that this returns values
     * above {@code 1}, so a shape eased with it will briefly exceed its final
     * size, which is the point.
     */
    EASE_OUT_ELASTIC {
        @Override
        public double apply(double progress) {
            if (progress == 0 || progress == 1) return progress;
            double period = 0.3;
            return Math.pow(2, -10 * progress)
                    * Math.sin((progress - period / 4) * (2 * Math.PI) / period) + 1;
        }
    },

    /** Bounces on arrival, like something dropped onto a floor. */
    EASE_OUT_BOUNCE {
        @Override
        public double apply(double progress) {
            double factor = 7.5625;
            double divisor = 2.75;
            if (progress < 1 / divisor) return factor * progress * progress;
            if (progress < 2 / divisor) return factor * (progress -= 1.5 / divisor) * progress + 0.75;
            if (progress < 2.5 / divisor) return factor * (progress -= 2.25 / divisor) * progress + 0.9375;
            return factor * (progress -= 2.625 / divisor) * progress + 0.984375;
        }
    };

    /**
     * Applies the curve to a normalised progress value.
     *
     * @param progress progress in {@code 0..1}
     * @return the eased value; may exceed the range for overshooting curves
     */
    public abstract double apply(double progress);

    /**
     * Interpolates between two values along this curve.
     *
     * @param from     value at {@code progress = 0}
     * @param to       value at {@code progress = 1}
     * @param progress progress in {@code 0..1}, clamped
     * @return the interpolated value
     */
    public double between(double from, double to, double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        return from + (to - from) * apply(clamped);
    }
}
