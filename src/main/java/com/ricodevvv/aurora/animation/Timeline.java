package com.ricodevvv.aurora.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Secuenciador para encadenar efectos sin anidar cinco runTaskLater.
 *
 * <pre>
 * Timeline.create()
 *         .run(() -&gt; Sounds.ANVIL_LAND.playAt(loc, 1, 0.6f))
 *         .play(Animations.ringWave(loc, particle, 0.5, 5, 20, 40))
 *         .wait(10)
 *         .play(Animations.burst(loc, particle, 3, 15, 60))
 *         .wait(20)
 *         .run(() -&gt; loc.getWorld().getPlayers().forEach(p -&gt; ...))
 *         .start();
 * </pre>
 *
 * Corre dentro del mismo task global que las demas animaciones, asi que
 * encadenar 50 efectos no crea 50 tasks.
 */
public class Timeline extends Animation {

    private static class Step {
        final long at;
        final Runnable action;

        Step(long at, Runnable action) {
            this.at = at;
            this.action = action;
        }
    }

    private final List<Step> steps = new ArrayList<>();
    private long cursor = 0;
    private int index = 0;
    private int loops = 1;
    private int loopsDone = 0;

    public static Timeline create() {
        return new Timeline();
    }

    /** Avanza el cursor: lo que agregues despues ocurre X ticks mas tarde. */
    public Timeline wait(int ticks) {
        cursor += Math.max(0, ticks);
        return this;
    }

    public Timeline run(Runnable action) {
        steps.add(new Step(cursor, action));
        return this;
    }

    /** Arranca una animacion en este punto de la linea de tiempo. */
    public Timeline play(Animation animation) {
        return run(animation::start);
    }

    /** Arranca la animacion y espera a que termine antes de seguir. */
    public Timeline playAndWait(Animation animation) {
        play(animation);
        return wait(Math.max(1, animation.duration()));
    }

    public Timeline stopAfter(Animation animation, int ticks) {
        long at = cursor + ticks;
        steps.add(new Step(at, animation::stop));
        return this;
    }

    /** Repite toda la secuencia. 0 o negativo = infinito. */
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
