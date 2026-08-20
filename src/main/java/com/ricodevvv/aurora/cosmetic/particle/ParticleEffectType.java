package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import com.ricodevvv.aurora.util.Sounds;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * The definition of a wearable particle effect.
 *
 * <p>An effect is a set of renderers keyed by what the wearer is doing, plus
 * the handful of settings that decide what it costs: how often it draws, how
 * far it is visible, and whether it should stay on while its wearer is hidden.
 *
 * <pre>{@code
 * new ParticleEffectType("halo", "&eHalo", icon, ctx ->
 *         ctx.emit(dust, ctx.head(), RING.rotateY(ctx.time() * 2)))
 *         .state(MovementState.SPRINTING, ctx -> ...)   // streams back at a sprint
 *         .sound(Sounds.NOTE_HARP, 40, 0.2f, 1.4f, 1.9f)
 *         .interval(2)
 *         .range(28);
 * }</pre>
 *
 * @see EffectContext
 * @see MovementState
 */
public class ParticleEffectType implements CosmeticType {

    private final String id;
    private final String displayName;
    private final ItemStack icon;

    private final Map<MovementState, ContextRenderer> renderers =
            new EnumMap<>(MovementState.class);

    private int interval = 1;
    private double range = 32;
    private String permission;
    private EffectSound sound;
    private boolean hideWhenInvisible = true;
    private boolean showToWearer = true;

    /**
     * @param id          identifier, unique within the category
     * @param displayName display name, supporting {@code &} colour codes
     * @param icon        icon shown in a selection menu
     * @param renderer    how it draws; used for every state that does not
     *                    define its own
     */
    public ParticleEffectType(String id, String displayName, ItemStack icon, ContextRenderer renderer) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.renderers.put(MovementState.IDLE, renderer);
    }

    /**
     * Builds an effect from the older {@code (player, location, tick)}
     * renderer.
     *
     * <p>Kept so existing effects keep working unchanged. New effects should
     * take a {@link ContextRenderer}: the tick counter here is a raw frame
     * count, so an effect written against it changes speed when its interval
     * changes, which is exactly what the quality system does under load.
     *
     * @param id          identifier, unique within the category
     * @param displayName display name, supporting {@code &} colour codes
     * @param icon        icon shown in a selection menu
     * @param renderer    how it draws
     */
    public ParticleEffectType(String id, String displayName, ItemStack icon, EffectRenderer renderer) {
        this(id, displayName, icon, adapt(renderer));
    }

    /**
     * Defines the variant drawn in one movement state.
     *
     * <p>This is what stops an effect reading as a sticker: a ring of flames
     * standing still becomes two trails at the wearer's sides when they run,
     * and opens out when they jump. States with no renderer of their own fall
     * back through {@link MovementState#fallback()} to the idle one.
     *
     * @param state    when to use it
     * @param renderer variant to draw
     * @return this type
     */
    public ParticleEffectType state(MovementState state, ContextRenderer renderer) {
        renderers.put(state, renderer);
        return this;
    }

    /**
     * Defines the variant used while the wearer is walking, and by inheritance
     * while sprinting and airborne.
     *
     * @param renderer variant drawn while moving
     * @return this type
     */
    public ParticleEffectType moving(ContextRenderer renderer) {
        return state(MovementState.WALKING, renderer);
    }

    /**
     * Defines the moving variant from the older renderer signature.
     *
     * @param renderer variant drawn while moving
     * @return this type
     */
    public ParticleEffectType moving(EffectRenderer renderer) {
        return state(MovementState.WALKING, adapt(renderer));
    }

    /**
     * Defines the variant used while the wearer is crouching.
     *
     * @param renderer variant drawn while sneaking
     * @return this type
     */
    public ParticleEffectType sneaking(ContextRenderer renderer) {
        return state(MovementState.SNEAKING, renderer);
    }

    /**
     * Defines the variant used while the wearer is off the ground, and by
     * inheritance while flying.
     *
     * @param renderer variant drawn in the air
     * @return this type
     */
    public ParticleEffectType airborne(ContextRenderer renderer) {
        return state(MovementState.AIRBORNE, renderer);
    }

    /**
     * Sets how often the effect is drawn.
     *
     * <p>This is the most important performance lever here. A sixty-particle
     * effect drawn every tick, worn by fifty players, is sixty thousand
     * particles a second. At an interval of three that drops to twenty
     * thousand and is close to indistinguishable by eye.
     *
     * <p>The value is a baseline: the quality system stretches it further when
     * the server is under load. Effects written against
     * {@link EffectContext#time()} keep their animation speed when it does.
     *
     * @param interval ticks between frames; clamped to at least {@code 1}
     * @return this type
     */
    public ParticleEffectType interval(int interval) {
        this.interval = Math.max(1, interval);
        return this;
    }

    /**
     * Sets the radius within which the effect is visible.
     *
     * @param range radius in blocks
     * @return this type
     */
    public ParticleEffectType range(double range) {
        this.range = range;
        return this;
    }

    /**
     * Gives the effect a quiet ambient loop, played only to the players who can
     * already see it.
     *
     * @param sound sound spec, or {@code null} for a silent effect
     * @return this type
     */
    public ParticleEffectType sound(EffectSound sound) {
        this.sound = sound;
        return this;
    }

    /**
     * Gives the effect a quiet ambient loop with a randomised pitch.
     *
     * @param sound    which sound
     * @param interval ticks between plays
     * @param volume   volume; keep it well below {@code 1}
     * @param minPitch lowest pitch
     * @param maxPitch highest pitch
     * @return this type
     */
    public ParticleEffectType sound(Sounds sound, int interval, float volume,
                                    float minPitch, float maxPitch) {
        return sound(new EffectSound(sound, interval, volume, minPitch, maxPitch));
    }

    /**
     * Hides the effect while its wearer is invisible or vanished, which is what
     * stops a cosmetic from being a wallhack. On by default.
     *
     * @param hide {@code false} to keep drawing regardless
     * @return this type
     */
    public ParticleEffectType hideWhenInvisible(boolean hide) {
        this.hideWhenInvisible = hide;
        return this;
    }

    /**
     * Whether the wearer sees their own effect. On by default; turn it off for
     * effects that sit in front of the camera.
     *
     * @param show {@code false} to hide it from the wearer
     * @return this type
     */
    public ParticleEffectType showToWearer(boolean show) {
        this.showToWearer = show;
        return this;
    }

    public ParticleEffectType permission(String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public CosmeticCategory category() {
        return CosmeticCategory.PARTICLE_EFFECT;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public ItemStack icon() {
        return icon;
    }

    @Override
    public String permission() {
        return permission != null ? permission : CosmeticType.super.permission();
    }

    @Override
    public Cosmetic create(Player player) {
        return new ParticleEffect(player, this);
    }

    /**
     * Picks the variant to draw this frame, walking the fallback chain until it
     * finds one.
     *
     * @param state what the wearer is doing
     * @return the renderer to use; never {@code null}
     */
    public ContextRenderer renderer(MovementState state) {
        MovementState current = state;
        for (int guard = 0; guard < MovementState.values().length; guard++) {
            ContextRenderer renderer = renderers.get(current);
            if (renderer != null) return renderer;
            current = current.fallback();
        }
        return renderers.get(MovementState.IDLE);
    }

    /**
     * Picks the variant using the older moving/idle distinction.
     *
     * @param moving whether the wearer is currently moving
     * @return the renderer to use
     */
    public ContextRenderer renderer(boolean moving) {
        return renderer(moving ? MovementState.WALKING : MovementState.IDLE);
    }

    public int interval() {
        return interval;
    }

    public double range() {
        return range;
    }

    /**
     * @return the ambient sound, or {@code null} if the effect is silent
     */
    public EffectSound soundSpec() {
        return sound;
    }

    /**
     * @return {@code true} if the effect hides with its wearer
     */
    public boolean hideWhenInvisible() {
        return hideWhenInvisible;
    }

    /**
     * @return {@code true} if the wearer sees their own effect
     */
    public boolean showToWearer() {
        return showToWearer;
    }

    /**
     * Wraps a legacy renderer so it can live in the same map as the modern one.
     *
     * @param renderer the legacy renderer
     * @return an equivalent context renderer
     */
    private static ContextRenderer adapt(EffectRenderer renderer) {
        return context -> renderer.render(context.player(), context.origin(), context.tick());
    }
}
