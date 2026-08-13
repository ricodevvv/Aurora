package com.ricodevvv.aurora.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequences actions and animations along a shared clock.
 *
 * <p>This exists to replace nested {@code runTaskLater} calls. Because a
 * timeline is itself an {@link Animation}, chaining fifty effects still costs
 * one server task rather than fifty.
 *
 * <pre>{@code
 * Timeline.create()
 *         .run(() -> Sounds.ANVIL_LAND.playAt(location, 1f, 0.6f))
 *         .play(Animations.ringWave(location, particle, 0.5, 6, 20, 40))
 *         .wait(15)
 *         .play(Animations.burst(location, particle, 3, 15, 60))
 *         .repeat(3)
 *         .start();
 * }</pre>
 */
public class Timeline extends Animation {

    /**
     * A single scheduled action and the tick it fires on.
     */
    private static final class Step {

        private final long at;
        private final Runnable action;

        private Step(long at, Runnable action) {
            this.at = at;
            this.action = action;
        }
    }

    private final List<Step> steps = new ArrayList<>();

    private long cursor;
    private int index;
    private int loops = 1;
    private int loopsDone;

    /**
     * @return a new empty timeline
     */
    public static Timeline create() {
        return new Timeline();
    }

    /**
     * Advances the cursor, so anything added afterwards fires later.
     *
     * @param ticks ticks to wait; negative values are treated as zero
     * @return this timeline
     */
    public Timeline wait(int ticks) {
        cursor += Math.max(0, ticks);
        return this;
    }

    /**
     * Schedules an action at the current cursor position.
     *
     * @param action work to run
     * @return this timeline
     */
    public Timeline run(Runnable action) {
        steps.add(new Step(cursor, action));
        return this;
    }

    /**
     * Starts an animation at the current cursor position without waiting for it.
     *
     * @param animation animation to start
     * @return this timeline
     */
    public Timeline play(Animation animation) {
        return run(animation::start);
    }

    /**
     * Starts an animation and advances the cursor past its duration.
     *
     * @param animation animation to start; must have a finite duration
     * @return this timeline
     */
    public Timeline playAndWait(Animation animation) {
        play(animation);
        return wait(Math.max(1, animation.duration()));
    }

    /**
     * Schedules an animation to be stopped at a later point.
     *
     * @param animation animation to stop
     * @param ticks     ticks after the current cursor
     * @return this timeline
     */
    public Timeline stopAfter(Animation animation, int ticks) {
        steps.add(new Step(cursor + ticks, animation::stop));
        return this;
    }

    /**
     * Repeats the whole sequence.
     *
     * @param times how many times to run it; zero or negative means forever
     * @return this timeline
     */
    public Timeline repeat(int times) {
        this.loops = times <= 0 ? -1 : times;
        return this;
    }

    @Override
    public Timeline start() {
        super.start();
        return this;
    }

    @Override
    protected void update(long tick) {
        long cycleLength = cursor + 1;
        long localTick = loops == 1 ? tick : tick % cycleLength;

        if (localTick == 0 && tick > 0) {
            index = 0;
            loopsDone++;
            if (loops > 0 && loopsDone >= loops) {
                stop();
                return;
            }
        }

        while (index < steps.size() && steps.get(index).at <= localTick) {
            steps.get(index).action.run();
            index++;
        }

        if (loops == 1 && index >= steps.size() && tick >= cursor) stop();
    }
}
