package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.cosmetic.CosmeticRegistry;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.shape.Curves;
import com.ricodevvv.aurora.shape.Glyphs;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.ColorRamp;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Easing;
import com.ricodevvv.aurora.util.Items;
import com.ricodevvv.aurora.util.Noise;
import com.ricodevvv.aurora.util.Sounds;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * The built-in catalogue of particle effects.
 *
 * <p>Every effect here is built to the same five rules, and they are what
 * separate an effect that looks bought from one that looks improvised:
 *
 * <ol>
 *   <li><b>Animate against {@link EffectContext#time()}, never a frame
 *       counter.</b> Speeds are then written in turns per second and survive
 *       the quality system changing the frame rate underneath them.</li>
 *   <li><b>Layer.</b> A single flat colour reads as dots. Every effect has at
 *       least two of: a bright core, a coloured body sampled from a
 *       {@link ColorRamp}, and a sparse accent.</li>
 *   <li><b>React.</b> Anything worn on the body defines at least one
 *       {@link MovementState} variant, so it behaves differently at a sprint
 *       or in the air.</li>
 *   <li><b>Wander.</b> Offsets come from {@link Noise} rather than
 *       {@code Math.random()}, so particles drift instead of flickering, and
 *       each wearer's drift is their own.</li>
 *   <li><b>Stay cheap.</b> Builders and static geometry are constants;
 *       procedural counts go through {@link EffectContext#count(int)} so they
 *       scale with quality like shapes already do.</li>
 * </ol>
 *
 * <p>Two allocation rules are followed throughout, and both matter at scale.
 * Particle builders are created once as constants and reused: they are mutable
 * by design, so a renderer only adjusts colour or size before spawning.
 * Static geometry is precomputed into the {@code SHAPE_*} constants, because
 * regenerating an eighty-point sphere every tick is wasted trigonometry.
 */
public final class ParticleEffects {

    private static final double TAU = Math.PI * 2;

    // ------------------------------------------------------- shared builders

    private static final ParticleBuilder DUST = Particles.of(ParticleType.DUST).count(1).speed(0);

    /** Kept apart from {@link #DUST} because it stays on the transition particle. */
    private static final ParticleBuilder FADE = Particles.dust(Color.WHITE, Color.WHITE).count(1).speed(0);

    private static final ParticleBuilder FLAME = Particles.flame().count(1).speed(0.01);
    private static final ParticleBuilder SOUL_FLAME = Particles.soulFlame().count(1).speed(0.01);
    private static final ParticleBuilder CLOUD = Particles.of(ParticleType.CLOUD).count(1).speed(0.01);
    private static final ParticleBuilder SMOKE = Particles.of(ParticleType.LARGE_SMOKE).count(1).speed(0.01);
    private static final ParticleBuilder HEART = Particles.of(ParticleType.HEART).count(1).speed(0);
    private static final ParticleBuilder NOTE = Particles.of(ParticleType.NOTE).count(1);
    private static final ParticleBuilder ENCHANT = Particles.enchant().count(1).speed(0.5);
    private static final ParticleBuilder CRIT = Particles.crit().count(1).speed(0.05);
    private static final ParticleBuilder DRIP = Particles.of(ParticleType.DRIPPING_WATER).count(1).speed(0);
    private static final ParticleBuilder SPLASH = Particles.of(ParticleType.SPLASH).count(2).offset(0.1).speed(0.05);
    private static final ParticleBuilder PORTAL = Particles.of(ParticleType.PORTAL).count(1).speed(0.4);
    private static final ParticleBuilder FIREWORK = Particles.spark().count(1).speed(0.05);
    private static final ParticleBuilder END_ROD = Particles.endRod().count(1).speed(0);
    private static final ParticleBuilder ELECTRIC = Particles.electric().count(1).speed(0);
    private static final ParticleBuilder SNOWFLAKE = Particles.snowflake().count(1).speed(0);
    private static final ParticleBuilder TOTEM = Particles.totem().count(1).speed(0.05);
    private static final ParticleBuilder SOUL = Particles.soul().count(1).speed(0.02);
    private static final ParticleBuilder PETAL = Particles.petal().count(1).speed(0);
    private static final ParticleBuilder BUBBLE = Particles.of(ParticleType.BUBBLE).count(1).speed(0.02);
    private static final ParticleBuilder LAVA_DRIP = Particles.of(ParticleType.DRIPPING_LAVA).count(1).speed(0);

    // --------------------------------------------------- precomputed geometry

    private static final Shape SHAPE_RING = Shapes.circle(1, 28);
    private static final Shape SHAPE_RING_FINE = Shapes.circle(1, 48);
    private static final Shape SHAPE_DOME = Shapes.dome(1.8, 64);
    private static final Shape SHAPE_WINGS = Shapes.wings(1.6, 6, 8);
    private static final Shape SHAPE_BAT_WINGS = Shapes.dragonWings(1.7, 5, 9);
    private static final Shape SHAPE_BUTTERFLY = Shapes.butterflyWings(1.5, 34);
    private static final Shape SHAPE_STAR = Shapes.star(5, 0.7, 0.3, 5);
    private static final Shape SHAPE_HEART = Shapes.heart(0.9, 40);
    private static final Shape SHAPE_SNOWFLAKE = Shapes.rose(3, 1.0, 60);
    private static final Shape SHAPE_GALAXY = Shapes.galaxy(1.9, 3, 26);
    private static final Shape SHAPE_ORB = Shapes.spiralSphere(0.4, 6, 22);
    private static final Shape SHAPE_CROWN = Shapes.crown(0.45, 8, 0.3, 24);
    private static final Shape SHAPE_ATOM = Shapes.orbit(1.15, 3, 22);
    private static final Shape SHAPE_FLOWER = Shapes.flower(6, 1.1, 9);
    private static final Shape SHAPE_CRESCENT = Shapes.crescent(0.8, 30);
    private static final Shape SHAPE_SKULL = Glyphs.sprite(Glyphs.SKULL, 0.12);

    private ParticleEffects() {
    }

    // ----------------------------------------------------------------- fire

    public static ParticleEffectType auraDeFuego() {
        Color hot = Colors.hex("#FFE9A8");
        Color ember = Colors.hex("#C22A08");

        return new ParticleEffectType("aura_fuego", "&6Aura de Fuego", icon("BLAZE_POWDER", "FIRE_CHARGE"),
                (ContextRenderer) ctx -> {
                    // Three tongues of flame, each on its own climb so the
                    // column never pulses as one block of light.
                    double t = ctx.time();
                    for (int i = 0; i < 3; i++) {
                        double climb = (t * 0.85 + i / 3.0) % 1.0;
                        double angle = t * 2.0 + TAU * i / 3;
                        double radius = 0.85 - climb * 0.4;
                        double lean = ctx.noise(1.4, i * 3.0) * 0.14;
                        ctx.emit(fade(hot, ember, 1.1f),
                                Math.cos(angle) * radius + lean,
                                0.15 + climb * 2.0,
                                Math.sin(angle) * radius + lean);
                    }
                    ctx.emit(dust(ember, 0.9f), ctx.feet(),
                            SHAPE_RING.scale(0.95), ColorRamp.of(Colors.FIRE).jitter(14));
                    if (ctx.tick() % 6 == 0) ctx.emit(FLAME, 0, 0.3, 0);
                })
                // At a sprint the column cannot keep up, so it stops trying:
                // the fire is left behind along the path actually covered.
                .state(MovementState.SPRINTING, ctx -> ctx.trail(3, (position, progress) -> {
                    ctx.emit(fade(hot, ember, 1.2f), position.clone().add(0, 0.2 + progress * 0.5, 0));
                    if (progress > 0.6) ctx.emit(FLAME, position);
                }))
                .state(MovementState.AIRBORNE, ctx -> {
                    double t = ctx.time();
                    ctx.emit(fade(hot, ember, 1.2f), ctx.feet(),
                            Shapes.vortex(0.7, -1.4, 2, ctx.count(18)).rotateY(t * 4));
                })
                .sound(Sounds.FIRE_IGNITE, 45, 0.12f, 0.6f, 0.9f)
                .interval(2)
                .range(30);
    }

    public static ParticleEffectType anillosDeLlama() {
        return new ParticleEffectType("anillos_llama", "&cAnillos de Llama", icon("MAGMA_CREAM", "SLIME_BALL"),
                (ContextRenderer) ctx -> {
                    // Three rings at different heights spinning in opposite
                    // directions, which is what stops it reading as a cylinder.
                    double t = ctx.time();
                    for (int ring = 0; ring < 3; ring++) {
                        double y = 0.3 + ring * 0.7;
                        double spin = (ring % 2 == 0 ? 1 : -1) * t * 2.4;
                        double radius = 0.8 + Math.sin(t * 1.2 + ring) * 0.15;
                        ctx.emit(dust(Colors.hex("#FF9430"), 1.0f), ctx.above(y),
                                SHAPE_RING.scale(radius).rotateY(spin),
                                ColorRamp.of(Colors.FIRE).repeat(2));
                    }
                })
                .moving(ctx -> {
                    // Two streaks at the sides, aligned with the body rather
                    // than with the world.
                    Vector side = ctx.right().multiply(0.45);
                    ctx.trail(2, (position, progress) -> {
                        ctx.emit(FLAME, position.clone().add(side));
                        ctx.emit(FLAME, position.clone().subtract(side));
                    });
                })
                .interval(2)
                .range(28);
    }

    public static ParticleEffectType llamaDemoniaca() {
        return new ParticleEffectType("llama_demoniaca", "&4Llama Demoniaca", icon("NETHER_STAR"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    Shape vortex = Shapes.vortex(1.2, 2.4, 3, ctx.count(42)).rotateY(t * 3);
                    ctx.emit(dust(Colors.hex("#FF6A1A"), 1.2f), ctx.origin(), vortex,
                            ColorRamp.of(Colors.LAVA).jitter(12));
                    if (ctx.tick() % 8 == 0) {
                        ctx.emit(SMOKE, ctx.random() - 0.5, 2.2, ctx.random() - 0.5);
                    }
                })
                .state(MovementState.SPRINTING, ctx -> ctx.trail(3, (position, progress) ->
                        ctx.emit(fade(Colors.hex("#FF8A20"), Colors.hex("#2A0A05"), 1.4f),
                                position.clone().add(0, 0.25, 0))))
                .interval(2)
                .range(30);
    }

    /**
     * @return fire wings that beat harder the moment their wearer leaves the
     * ground, trailing embers behind them
     */
    public static ParticleEffectType fenix() {
        return new ParticleEffectType("fenix", "&6&lFenix", icon("BLAZE_ROD", "BLAZE_POWDER"),
                (ContextRenderer) ctx -> {
                    Shape wings = SHAPE_WINGS.scale(flap(ctx, 0.12, 0.3)).facingYaw(ctx.yaw());
                    ctx.emit(dust(Colors.hex("#FFB030"), 1.0f), ctx.behind(0.15, 1.15), wings,
                            ColorRamp.of(Colors.FIRE).mirror());
                    if (ctx.tick() % 4 == 0) {
                        // Embers falling off the trailing edge.
                        double side = ctx.random() < 0.5 ? -1 : 1;
                        ctx.emit(fade(Colors.hex("#FFD070"), Colors.hex("#801000"), 0.8f),
                                ctx.beside(side * (0.6 + ctx.random() * 0.7), 0.6 + ctx.random() * 0.6));
                    }
                })
                .airborne(ctx -> {
                    Shape wings = SHAPE_WINGS.scale(flap(ctx, 0.12, 0.3) * 1.25).facingYaw(ctx.yaw());
                    ctx.emit(dust(Colors.hex("#FFD070"), 1.1f), ctx.behind(0.15, 1.2), wings,
                            ColorRamp.of(Colors.FIRE).mirror());
                    ctx.emit(FLAME, 0, 0.2, 0);
                })
                .interval(2)
                .range(32);
    }

    /**
     * @return cracked ground and dripping lava, heaviest where the wearer last
     * put their feet
     */
    public static ParticleEffectType magma() {
        return new ParticleEffectType("magma", "&4Magma", icon("MAGMA_BLOCK", "NETHERRACK"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#FF5A10"), 1.3f), ctx.feet(),
                            Shapes.runeCircle(1.1, 5, 0.25, ctx.count(26)).rotateY(t * 0.4),
                            ColorRamp.of(Colors.LAVA));
                    if (ctx.tick() % 5 == 0) {
                        double angle = ctx.random() * TAU;
                        double radius = ctx.random() * 1.1;
                        ctx.emit(LAVA_DRIP, Math.cos(angle) * radius, 0.9, Math.sin(angle) * radius);
                    }
                })
                .moving(ctx -> ctx.trail(3, (position, progress) ->
                        ctx.emit(dust(Colors.hex("#FF7020"), 1.4f), position.clone().add(0, 0.06, 0))))
                .interval(3)
                .range(28);
    }

    // ------------------------------------------------------------------ ice

    public static ParticleEffectType senorDelHielo() {
        return new ParticleEffectType("senor_hielo", "&bSenor del Hielo", icon("PACKED_ICE", "ICE"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#9EE4FF"), 1.0f), ctx.feet(),
                            SHAPE_RING.scale(1.4).rotateY(t * 1.6), ColorRamp.of(Colors.ICE));
                    ctx.emit(dust(Colors.hex("#DFF6FF"), 0.7f), ctx.above(1.0),
                            SHAPE_RING.scale(0.9).rotateY(-t * 1.1), ColorRamp.of(Colors.ICE).mirror());
                    // Crystals settling out of the air around the wearer.
                    if (ctx.tick() % 4 == 0) {
                        double angle = ctx.random() * TAU;
                        ctx.emit(SNOWFLAKE, Math.cos(angle) * 1.2, 2.2 + ctx.random(), Math.sin(angle) * 1.2);
                    }
                })
                .sneaking(ctx -> ctx.emit(dust(Colors.hex("#DFF6FF"), 0.8f), ctx.feet(),
                        SHAPE_RING.scale(0.7), ColorRamp.of(Colors.ICE)))
                .interval(2)
                .range(30);
    }

    public static ParticleEffectType copoDeNieve() {
        return new ParticleEffectType("copo_nieve", "&fCopo de Nieve", icon("SNOWBALL", "SNOW_BALL"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#EAF6FF"), 0.9f), ctx.above(2.6),
                            SHAPE_SNOWFLAKE.scale(0.9 + Math.sin(t) * 0.06).rotateY(t * 0.9),
                            ColorRamp.of(Colors.ICE).mirror());
                    if (ctx.tick() % 6 == 0) ctx.emit(SNOWFLAKE, 0, 2.6, 0);
                })
                .interval(3)
                .range(28);
    }

    public static ParticleEffectType nubeDeNieve() {
        return new ParticleEffectType("nube_nieve", "&fNube de Nieve", icon("SNOW_BLOCK", "SNOW"),
                (ContextRenderer) ctx -> {
                    Location cloud = ctx.above(2.8);
                    ctx.emit(CLOUD, cloud, Shapes.cloud(0.55, ctx.count(7)));
                    for (int i = 0; i < ctx.count(2); i++) {
                        double angle = ctx.random() * TAU;
                        double radius = ctx.random() * 0.6;
                        ctx.emit(SNOWFLAKE, Math.cos(angle) * radius,
                                2.8 - ctx.random() * 2.2, Math.sin(angle) * radius);
                    }
                })
                .interval(2)
                .range(28);
    }

    /**
     * @return a blizzard that tightens into a slipstream when its wearer runs
     */
    public static ParticleEffectType ventisca() {
        return new ParticleEffectType("ventisca", "&b&lVentisca", icon("BLUE_ICE", "PACKED_ICE", "ICE"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(7); i++) {
                        double phase = (t * 0.7 + i / 7.0) % 1.0;
                        double angle = phase * TAU * 2 + i;
                        double radius = 0.5 + phase * 1.2;
                        ctx.emit(dust(Colors.gradient(Colors.ICE, phase), 0.9f),
                                Math.cos(angle) * radius, 0.2 + phase * 1.9, Math.sin(angle) * radius);
                    }
                })
                .state(MovementState.SPRINTING, ctx -> ctx.trail(3, (position, progress) -> {
                    ctx.emit(dust(Colors.hex("#CFEFFF"), 1.0f), position.clone().add(0, 0.9, 0));
                    ctx.emit(SNOWFLAKE, position.clone().add(0, 0.4, 0));
                }))
                .interval(2)
                .range(28);
    }

    // ---------------------------------------------------------------- water

    public static ParticleEffectType nubeDeLluvia() {
        return new ParticleEffectType("nube_lluvia", "&8Nube de Lluvia", icon("WATER_BUCKET"),
                (ContextRenderer) ctx -> {
                    Location cloud = ctx.above(2.8);
                    ctx.emit(SMOKE, cloud, Shapes.cloud(0.6, ctx.count(6)));
                    for (int i = 0; i < ctx.count(2); i++) {
                        double angle = ctx.random() * TAU;
                        double radius = ctx.random() * 0.7;
                        ctx.emit(DRIP, cloud.clone().add(Math.cos(angle) * radius, -0.3, Math.sin(angle) * radius));
                    }
                    if (ctx.tick() % 4 == 0) {
                        double angle = ctx.random() * TAU;
                        double radius = ctx.random() * 0.8;
                        ctx.emit(SPLASH, Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                    }
                })
                .interval(2)
                .range(28);
    }

    public static ParticleEffectType paraguas() {
        return new ParticleEffectType("paraguas", "&9Paraguas", icon("LEAD", "STICK"),
                (ContextRenderer) ctx -> {
                    ctx.emit(dust(Colors.hex("#3355AA"), 1.0f), ctx.above(2.3),
                            SHAPE_DOME.scale(0.55), ColorRamp.of(Colors.OCEAN));
                    if (ctx.tick() % 2 == 0) {
                        double angle = ctx.random() * TAU;
                        ctx.emit(DRIP, Math.cos(angle) * 1.05, 2.3 - ctx.random(), Math.sin(angle) * 1.05);
                    }
                })
                .interval(3)
                .range(26);
    }

    /**
     * @return bubbles rising and popping around the wearer
     */
    public static ParticleEffectType burbujas() {
        return new ParticleEffectType("burbujas", "&bBurbujas", icon("PRISMARINE_SHARD", "WATER_BUCKET"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(4); i++) {
                        double phase = (t * 0.5 + i / 4.0) % 1.0;
                        double angle = i * 2.4 + ctx.noise(0.5, i) * 1.5;
                        double radius = 0.35 + phase * 0.35;
                        ctx.emit(BUBBLE, Math.cos(angle) * radius, phase * 2.2, Math.sin(angle) * radius);
                    }
                    if (ctx.tick() % 10 == 0) ctx.emit(SPLASH, 0, 2.1, 0);
                })
                .interval(3)
                .range(26);
    }

    // --------------------------------------------------------------- wings

    public static ParticleEffectType alasDeAngel() {
        return wings("alas_angel", "&fAlas de Angel", Items.head(),
                ColorRamp.between(Colors.hex("#FFFBE0"), Colors.hex("#FFE08A")).mirror(), SHAPE_WINGS, true);
    }

    public static ParticleEffectType alasDeVampiro() {
        return wings("alas_vampiro", "&4Alas de Vampiro", icon("REDSTONE"),
                ColorRamp.of(Colors.BLOOD), SHAPE_BAT_WINGS, false);
    }

    public static ParticleEffectType alasArcoiris() {
        return new ParticleEffectType("alas_arcoiris", "&d&lAlas Arcoiris", icon("FEATHER"),
                (ContextRenderer) ctx -> {
                    Shape shape = SHAPE_WINGS.scale(flap(ctx, 0.1, 0.26)).facingYaw(ctx.yaw());
                    ctx.emit(dust(Color.WHITE, 0.95f), ctx.behind(0.12, 1.1), shape,
                            ColorRamp.rainbow(ctx.time() * 0.25).mirror());
                })
                .interval(2)
                .range(30);
    }

    /**
     * @return membranous wings with embers dripping off the trailing edge
     */
    public static ParticleEffectType alasDeDragon() {
        return new ParticleEffectType("alas_dragon", "&5&lAlas de Dragon", icon("DRAGON_BREATH", "COAL"),
                (ContextRenderer) ctx -> {
                    Shape shape = SHAPE_BAT_WINGS.scale(flap(ctx, 0.08, 0.24)).facingYaw(ctx.yaw());
                    ctx.emit(dust(Colors.hex("#8A5AD8"), 1.0f), ctx.behind(0.15, 1.15), shape,
                            ColorRamp.of(Colors.VOID).mirror());
                    if (ctx.tick() % 6 == 0) {
                        double side = ctx.random() < 0.5 ? -1 : 1;
                        ctx.emit(SMOKE, ctx.beside(side * (0.8 + ctx.random() * 0.6), 0.7));
                    }
                })
                .airborne(ctx -> {
                    Shape shape = SHAPE_BAT_WINGS.scale(flap(ctx, 0.08, 0.24) * 1.3).facingYaw(ctx.yaw());
                    ctx.emit(dust(Colors.hex("#A070FF"), 1.1f), ctx.behind(0.15, 1.2), shape,
                            ColorRamp.of(Colors.VOID).mirror());
                })
                .interval(2)
                .range(32);
    }

    /**
     * @return butterfly wings, beating slowly enough to be read as one shape
     */
    public static ParticleEffectType alasDeMariposa() {
        return new ParticleEffectType("alas_mariposa", "&d&lAlas de Mariposa", icon("PINK_DYE", "INK_SACK"),
                (ContextRenderer) ctx -> {
                    Shape shape = SHAPE_BUTTERFLY.scale(flap(ctx, 0.14, 0.3)).facingYaw(ctx.yaw());
                    ctx.emit(dust(Colors.hex("#FFB7D5"), 0.9f), ctx.behind(0.1, 1.2), shape,
                            ColorRamp.of(Colors.SAKURA).mirror());
                    if (ctx.tick() % 8 == 0) ctx.emit(PETAL, ctx.beside(ctx.random() - 0.5, 1.4));
                })
                .interval(2)
                .range(30);
    }

    /**
     * Builds a pair of wings that sit on the wearer's back and beat with them.
     *
     * @param id      identifier
     * @param name    display name
     * @param icon    menu icon
     * @param ramp    colour along each feather
     * @param shape   wing geometry
     * @param feathers whether to shed the occasional feather
     * @return the effect
     */
    private static ParticleEffectType wings(String id, String name, ItemStack icon,
                                            ColorRamp ramp, Shape shape, boolean feathers) {
        return new ParticleEffectType(id, name, icon,
                (ContextRenderer) ctx -> {
                    Shape beat = shape.scale(flap(ctx, 0.1, 0.28)).facingYaw(ctx.yaw());
                    ctx.emit(dust(Color.WHITE, 0.95f), ctx.behind(0.12, 1.1), beat, ramp);
                    if (feathers && ctx.tick() % 12 == 0) {
                        ctx.emit(dust(Colors.hex("#FFFDF0"), 0.7f),
                                ctx.beside(ctx.random() * 2 - 1, 0.7 + ctx.random() * 0.6));
                    }
                })
                // Off the ground the wings open wider and beat faster; that
                // single change is what sells them as wings rather than as a
                // decal on the player's back.
                .airborne(ctx -> {
                    Shape beat = shape.scale(flap(ctx, 0.1, 0.28) * 1.25).facingYaw(ctx.yaw());
                    ctx.emit(dust(Color.WHITE, 1.0f), ctx.behind(0.12, 1.15), beat, ramp);
                })
                .interval(2)
                .range(30);
    }

    /**
     * The wing beat: a slow breath on the ground, a hard flap in the air, and
     * a phase offset per wearer so a group never beats in unison.
     *
     * @param ctx     current frame
     * @param ground  beat amplitude on the ground
     * @param airborne beat amplitude in the air
     * @return a scale factor to apply to the wing shape
     */
    private static double flap(EffectContext ctx, double ground, double airborne) {
        boolean flying = ctx.state().airborne();
        double speed = flying ? 7.0 : 2.6;
        double amplitude = flying ? airborne : ground;
        return 1 + Math.sin(ctx.time() * speed + ctx.phase() * TAU) * amplitude;
    }

    // ----------------------------------------------------------------- love

    public static ParticleEffectType enamorado() {
        return new ParticleEffectType("enamorado", "&dEnamorado", icon("RED_DYE", "INK_SACK"),
                (ContextRenderer) ctx -> {
                    if (ctx.tick() % 6 == 0) {
                        ctx.emit(HEART, ctx.random() - 0.5, 1.8 + ctx.random() * 0.6, ctx.random() - 0.5);
                    }
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#FF74A8"), 0.7f), ctx.above(1.4),
                            SHAPE_RING.scale(0.55).rotateY(t * 1.2), ColorRamp.of(Colors.SAKURA));
                })
                .interval(2)
                .range(26);
    }

    public static ParticleEffectType corazonGigante() {
        return new ParticleEffectType("corazon_gigante", "&cCorazon Gigante", icon("APPLE"),
                (ContextRenderer) ctx -> {
                    // A heartbeat rather than a constant size: lub, dub, rest.
                    double cycle = (ctx.time() * 1.1 + ctx.phase()) % 1.0;
                    double pulse = 1 + 0.13 * (beat(cycle, 0) + beat(cycle, 0.22) * 0.7);
                    ctx.emit(dust(Colors.hex("#FF3B6B"), 1.0f), ctx.above(2.4),
                            SHAPE_HEART.scale(pulse).facingYaw(ctx.yaw()),
                            ColorRamp.between(Colors.hex("#FF9AB8"), Colors.hex("#B4001F")).mirror());
                })
                .interval(3)
                .range(30);
    }

    /**
     * @return blossom petals drifting down and settling behind the wearer
     */
    public static ParticleEffectType lluviaDePetalos() {
        return new ParticleEffectType("petalos", "&d&lLluvia de Petalos", icon("CHERRY_SAPLING", "PINK_DYE", "INK_SACK"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(3); i++) {
                        double phase = (t * 0.35 + i / 3.0) % 1.0;
                        double angle = i * 2.1 + ctx.noise(0.4, i) * 2;
                        double radius = 0.9 + ctx.noise(0.3, i * 2.0) * 0.3;
                        ctx.emit(dust(Colors.gradient(Colors.SAKURA, phase), 0.9f),
                                Math.cos(angle) * radius, 2.6 - phase * 2.4, Math.sin(angle) * radius);
                    }
                    if (ctx.tick() % 5 == 0) ctx.emit(PETAL, ctx.random() - 0.5, 2.5, ctx.random() - 0.5);
                })
                .interval(2)
                .range(28);
    }

    // ---------------------------------------------------------------- magic

    public static ParticleEffectType encantado() {
        return new ParticleEffectType("encantado", "&5Encantado", icon("ENCHANTING_TABLE", "ENCHANTMENT_TABLE"),
                (ContextRenderer) ctx -> {
                    ctx.emit(ENCHANT, 0, 2.2, 0);
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#C9A6FF"), 0.8f), ctx.above(1.0),
                            SHAPE_RING.scale(0.75).rotateY(t * 1.4).rotateX(0.35),
                            ColorRamp.of(Colors.VOID).mirror());
                })
                .interval(1)
                .range(26);
    }

    public static ParticleEffectType auraLegendaria() {
        return new ParticleEffectType("aura_legendaria", "&6&lAura Legendaria", icon("GOLD_INGOT"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    Shape helix = Shapes.doubleHelix(0.85, 2.4, 2, ctx.count(24)).rotateY(t * 2.4);
                    ctx.emit(dust(Colors.hex("#FFD24A"), 1.0f), ctx.origin(), helix,
                            ColorRamp.of(Colors.GOLD).repeat(2));
                    ctx.emit(END_ROD, ctx.above(2.45), SHAPE_RING.scale(0.42).rotateY(-t * 1.6));
                    if (ctx.tick() % 8 == 0) ctx.emit(CRIT, 0, 1.2, 0);
                })
                .sound(Sounds.ORB, 60, 0.12f, 1.5f, 1.9f)
                .interval(2)
                .range(32);
    }

    public static ParticleEffectType escudoProtector() {
        return new ParticleEffectType("escudo", "&bEscudo Protector", icon("SHIELD", "IRON_CHESTPLATE"),
                (ContextRenderer) ctx -> {
                    // Breathing rather than spinning: the dome swells and
                    // settles, and the seam rotates slowly underneath.
                    double pulse = 1 + Math.sin(ctx.time() * 1.6 + ctx.phase() * TAU) * 0.07;
                    ctx.emit(dust(Colors.hex("#66CCFF"), 0.8f), ctx.above(0.2),
                            SHAPE_DOME.scale(pulse).rotateY(ctx.time() * 0.6),
                            ColorRamp.of(Colors.ICE).jitter(10));
                })
                .sneaking(ctx -> ctx.emit(dust(Colors.hex("#9EE4FF"), 0.9f), ctx.above(0.2),
                        SHAPE_DOME.scale(0.75), ColorRamp.of(Colors.ICE)))
                .interval(3)
                .range(30);
    }

    public static ParticleEffectType agujeroNegro() {
        return new ParticleEffectType("agujero_negro", "&8Agujero Negro", icon("COAL_BLOCK", "COAL"),
                (ContextRenderer) ctx -> {
                    // Inward spiral. The radius depends on each particle's own
                    // phase rather than on the global clock, so the flow stays
                    // continuous instead of pulsing.
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(5); i++) {
                        double phase = (t * 0.7 + i / 5.0) % 1.0;
                        double radius = 2.2 * (1 - phase);
                        double angle = phase * TAU * 3 + i;
                        ctx.emit(PORTAL, Math.cos(angle) * radius, 1.0 + phase * 0.4, Math.sin(angle) * radius);
                        ctx.emit(dust(Colors.gradient(Colors.NEBULA, 1 - phase), 1.0f),
                                Math.cos(angle + 0.4) * radius, 1.0 + phase * 0.4, Math.sin(angle + 0.4) * radius);
                    }
                    ctx.emit(dust(Colors.hex("#120820"), 1.6f), ctx.above(1.4), SHAPE_ORB);
                })
                .interval(2)
                .range(30);
    }

    public static ParticleEffectType yinYang() {
        return new ParticleEffectType("yin_yang", "&f&lYin &8&lYang", icon("BONE"),
                (ContextRenderer) ctx -> {
                    double spin = ctx.time() * 1.8;
                    Location base = ctx.feet();
                    ctx.emit(dust(Color.WHITE, 1.0f), base, Shapes.arc(1.1, 18, 0, 180).rotateY(spin));
                    ctx.emit(dust(Colors.hex("#1A1A1A"), 1.0f), base, Shapes.arc(1.1, 18, 180, 360).rotateY(spin));
                    ctx.emit(dust(Colors.hex("#1A1A1A"), 1.1f), Math.cos(spin) * 0.55, 0.08, Math.sin(spin) * 0.55);
                    ctx.emit(dust(Color.WHITE, 1.1f), -Math.cos(spin) * 0.55, 0.08, -Math.sin(spin) * 0.55);
                })
                .interval(2)
                .range(28);
    }

    public static ParticleEffectType circuloDeRunas() {
        return new ParticleEffectType("runas", "&5Circulo de Runas", icon("AMETHYST_SHARD", "EMERALD"),
                (ContextRenderer) ctx -> {
                    // The circle draws itself on over a second and a half, then
                    // holds; a summoning circle that simply appears reads as a
                    // texture rather than as a spell.
                    double draw = Math.min(1, ctx.time() * 0.7);
                    Shape circle = Shapes.runeCircle(1.5, 6, 0.4, ctx.count(40))
                            .rotateY(ctx.time() * 0.5)
                            .take(Easing.EASE_OUT_CUBIC.apply(draw));
                    ctx.emit(dust(Colors.hex("#B478FF"), 1.1f), ctx.feet(), circle,
                            ColorRamp.of(Colors.VOID).repeat(3));
                })
                .interval(3)
                .range(30);
    }

    /**
     * @return a crown of light that stays level while its wearer turns
     */
    public static ParticleEffectType coronaReal() {
        return new ParticleEffectType("corona", "&6&lCorona Real", icon("GOLDEN_HELMET", "GOLD_HELMET"),
                (ContextRenderer) ctx -> {
                    ctx.emit(dust(Colors.hex("#FFD24A"), 0.8f), ctx.above(2.15),
                            SHAPE_CROWN.rotateY(ctx.time() * 0.8), ColorRamp.of(Colors.GOLD).mirror());
                    if (ctx.tick() % 10 == 0) ctx.emit(END_ROD, 0, 2.3, 0);
                })
                .interval(3)
                .range(28);
    }

    /**
     * @return three tilted rings with an electron running round each
     */
    public static ParticleEffectType orbitaAtomica() {
        return new ParticleEffectType("atomo", "&b&lOrbita Atomica", icon("NETHER_STAR", "DIAMOND"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#7FD8FF"), 0.7f), ctx.above(1.1),
                            SHAPE_ATOM.rotateY(t * 0.9), ColorRamp.of(Colors.OCEAN).repeat(3));
                    for (int i = 0; i < 3; i++) {
                        double angle = t * 3.2 + TAU * i / 3;
                        double tilt = Math.PI * i / 3;
                        Vector electron = new Vector(Math.cos(angle) * 1.15, 0, Math.sin(angle) * 1.15);
                        com.ricodevvv.aurora.util.VectorMath.rotateX(electron, tilt);
                        ctx.emit(END_ROD, electron.getX(), 1.1 + electron.getY(), electron.getZ());
                    }
                    ctx.emit(dust(Colors.hex("#FFFFFF"), 1.4f), 0, 1.1, 0);
                })
                .interval(2)
                .range(30);
    }

    /**
     * @return a shaft of light with a halo, for staff and donator ranks
     */
    public static ParticleEffectType divino() {
        return new ParticleEffectType("divino", "&e&lDivino", icon("BEACON", "GLOWSTONE"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    // The column is drawn as motes rising rather than as a
                    // static line, so it reads as light and not as a fence.
                    for (int i = 0; i < ctx.count(5); i++) {
                        double phase = (t * 0.5 + i / 5.0) % 1.0;
                        double angle = i * 2.4 + t;
                        double radius = 0.32 * (1 - phase * 0.5);
                        ctx.emit(END_ROD, Math.cos(angle) * radius, phase * 3.0, Math.sin(angle) * radius);
                    }
                    ctx.emit(dust(Colors.hex("#FFF3B0"), 0.9f), ctx.above(2.3),
                            SHAPE_RING.scale(0.62).rotateY(t * 0.7).rotateX(0.12),
                            ColorRamp.of(Colors.GOLD).mirror());
                })
                .sound(Sounds.ORB, 80, 0.1f, 1.7f, 2.0f)
                .interval(2)
                .range(32);
    }

    /**
     * @return forest spirits circling the wearer at head height
     */
    public static ParticleEffectType espiritusDelBosque() {
        return new ParticleEffectType("espiritus", "&a&lEspiritus del Bosque", icon("OAK_SAPLING", "SAPLING"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(3); i++) {
                        double angle = t * 1.3 + TAU * i / 3;
                        double height = 1.4 + Math.sin(t * 1.7 + i * 2) * 0.45;
                        double radius = 1.0 + ctx.noise(0.6, i) * 0.2;
                        Location spirit = ctx.at(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                        ctx.emit(dust(Colors.gradient(Colors.TOXIC, ctx.phase()), 1.1f), spirit);
                        if (ctx.tick() % 4 == 0) ctx.emit(TOTEM, spirit);
                    }
                })
                .interval(2)
                .range(28);
    }

    // ---------------------------------------------------------------- party

    public static ParticleEffectType notasMusicales() {
        return new ParticleEffectType("notas", "&aNotas Musicales", icon("JUKEBOX"),
                (ContextRenderer) ctx -> {
                    double angle = ctx.time() * 4;
                    ctx.emit(NOTE.count(0).color(Colors.rainbow(ctx.time() * 0.4)),
                            Math.cos(angle) * 0.6, 2.2, Math.sin(angle) * 0.6);
                })
                .moving(ctx -> ctx.emit(NOTE.count(0).color(Colors.rainbow(ctx.time() * 0.4)), 0, 0.6, 0))
                .sound(Sounds.NOTE_HARP, 20, 0.25f, 0.8f, 1.9f)
                .interval(4)
                .range(26);
    }

    public static ParticleEffectType confeti() {
        return new ParticleEffectType("confeti", "&e&lConfeti", icon("FIREWORK_ROCKET", "FIREWORK"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(3); i++) {
                        double phase = (t * 0.8 + i / 3.0) % 1.0;
                        // Each streamer flutters as it falls rather than
                        // dropping straight down.
                        double drift = ctx.noise(1.1, i * 5.0) * 0.5;
                        ctx.emit(dust(Colors.gradient(Colors.CANDY, (i + phase) % 1.0), 1.2f),
                                drift + Math.cos(i * 2.1) * 0.5,
                                2.6 - phase * 2.4,
                                drift + Math.sin(i * 2.1) * 0.5);
                    }
                })
                .interval(2)
                .range(28);
    }

    public static ParticleEffectType horaDeFiesta() {
        return new ParticleEffectType("fiesta", "&d&lHora de Fiesta", icon("GLOWSTONE_DUST"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(4); i++) {
                        double angle = t * 2.5 + TAU * i / 4;
                        ctx.emit(FIREWORK, Math.cos(angle) * 1.1,
                                1.0 + Math.sin(t * 3 + i) * 0.5, Math.sin(angle) * 1.1);
                    }
                    if (ctx.tick() % 20 == 0) {
                        ctx.emit(dust(Colors.rainbow(ctx.random()), 1.3f), ctx.above(2.4),
                                Shapes.sphere(0.6, ctx.count(20)).jitter(0.1));
                    }
                })
                .sound(Sounds.FIREWORK_BLAST, 40, 0.15f, 1.4f, 1.9f)
                .interval(2)
                .range(28);
    }

    public static ParticleEffectType estrella() {
        return new ParticleEffectType("estrella", "&eEstrella", icon("NETHER_STAR"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    double twinkle = 0.95 + Math.sin(t * 3 + ctx.phase() * TAU) * 0.08;
                    ctx.emit(dust(Colors.hex("#FFE86B"), 1.0f), ctx.above(2.5),
                            SHAPE_STAR.scale(twinkle).rotateY(t * 1.6),
                            ColorRamp.of(Colors.GOLD).mirror());
                    if (ctx.tick() % 6 == 0) ctx.emit(END_ROD, 0, 2.5, 0);
                })
                .interval(3)
                .range(28);
    }

    /**
     * @return fireflies wandering around the wearer on their own paths
     */
    public static ParticleEffectType luciernagas() {
        return new ParticleEffectType("luciernagas", "&e&lLuciernagas", icon("GLOW_INK_SAC", "GLOWSTONE_DUST"),
                (ContextRenderer) ctx -> {
                    // Position comes from noise, not from a circle: fireflies
                    // that orbit look like a clock, ones that wander look alive.
                    for (int i = 0; i < ctx.count(5); i++) {
                        double x = ctx.noise(0.5, i * 7.0) * 1.5;
                        double z = ctx.noise(0.5, i * 7.0 + 3.5) * 1.5;
                        double y = 0.6 + (ctx.noise(0.4, i * 7.0 + 11) + 1) * 0.7;
                        boolean lit = Noise.at(ctx.time() * 2.5 + i * 13) > -0.2;
                        if (lit) ctx.emit(dust(Colors.hex("#FFF07A"), 0.8f), x, y, z);
                    }
                })
                .interval(2)
                .range(24);
    }

    // ------------------------------------------------------------------ dark

    public static ParticleEffectType calavera() {
        return new ParticleEffectType("calavera", "&8Calavera", Items.head(),
                (ContextRenderer) ctx -> {
                    double bob = Math.sin(ctx.time() * 1.4 + ctx.phase() * TAU) * 0.08;
                    ctx.emit(dust(Colors.hex("#DDDDDD"), 0.8f), ctx.above(2.6 + bob),
                            SHAPE_SKULL.facingYaw(ctx.yaw()),
                            ColorRamp.between(Colors.hex("#FFFFFF"), Colors.hex("#7A7A7A")));
                })
                .interval(3)
                .range(28);
    }

    public static ParticleEffectType helicesDeSangre() {
        return new ParticleEffectType("helices_sangre", "&4Helices de Sangre", icon("REDSTONE_BLOCK"),
                (ContextRenderer) ctx -> ctx.emit(dust(Colors.hex("#A00818"), 1.0f), ctx.origin(),
                        Shapes.doubleHelix(0.7, 2.2, 2, ctx.count(20)).rotateY(-ctx.time() * 3),
                        ColorRamp.of(Colors.BLOOD).repeat(2)))
                .interval(2)
                .range(30);
    }

    public static ParticleEffectType caminarEnSombras() {
        return new ParticleEffectType("sombras", "&8Caminar en Sombras", icon("COAL"),
                (ContextRenderer) ctx -> {
                    for (int i = 0; i < ctx.count(3); i++) {
                        ctx.emit(dust(Colors.gradient(Colors.SHADOW, ctx.random()), 1.3f),
                                (ctx.random() - 0.5) * 0.8, 0.08, (ctx.random() - 0.5) * 0.8);
                    }
                })
                // Walking leaves a shadow along the path rather than a puddle
                // under the feet.
                .moving(ctx -> ctx.trail(3, (position, progress) ->
                        ctx.emit(dust(Colors.gradient(Colors.SHADOW, progress), 1.4f),
                                position.clone().add(0, 0.06, 0))))
                .interval(2)
                .range(26);
    }

    /**
     * @return souls circling the wearer and drifting up out of the ground
     */
    public static ParticleEffectType almasEnPena() {
        return new ParticleEffectType("almas", "&f&lAlmas en Pena", icon("SOUL_SAND", "SOUL_SOIL", "SAND"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(3); i++) {
                        double phase = (t * 0.4 + i / 3.0) % 1.0;
                        double angle = phase * TAU + i * 2;
                        double radius = 0.9 - phase * 0.4;
                        ctx.emit(SOUL, Math.cos(angle) * radius, phase * 2.3, Math.sin(angle) * radius);
                        if (ctx.tick() % 4 == 0) {
                            ctx.emit(SOUL_FLAME, Math.cos(angle) * radius, phase * 2.3, Math.sin(angle) * radius);
                        }
                    }
                    ctx.emit(dust(Colors.hex("#7FE6D8"), 0.9f), ctx.feet(),
                            SHAPE_RING.scale(1.1).rotateY(-t * 0.9), ColorRamp.of(Colors.ENDER));
                })
                .interval(2)
                .range(28);
    }

    /**
     * @return a toxic cloud that settles when its wearer crouches
     */
    public static ParticleEffectType toxico() {
        return new ParticleEffectType("toxico", "&a&lToxico", icon("SLIME_BALL", "SLIME_BLOCK"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    for (int i = 0; i < ctx.count(4); i++) {
                        double angle = t * 0.9 + TAU * i / 4;
                        double radius = 0.7 + ctx.noise(0.8, i) * 0.35;
                        double height = 0.4 + (ctx.noise(0.6, i * 3.0) + 1) * 0.7;
                        ctx.emit(dust(Colors.gradient(Colors.TOXIC, (i / 4.0 + t * 0.3) % 1.0), 1.3f),
                                Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                    }
                    if (ctx.tick() % 6 == 0) ctx.emit(SMOKE, 0, 0.9, 0);
                })
                .sneaking(ctx -> ctx.emit(dust(Colors.hex("#78E63C"), 1.4f), ctx.feet(),
                        SHAPE_RING.scale(1.3), ColorRamp.of(Colors.TOXIC)))
                .interval(2)
                .range(26);
    }

    /**
     * @return a moon that keeps its face towards the wearer's back while it
     * drifts overhead
     */
    public static ParticleEffectType lunaCreciente() {
        return new ParticleEffectType("luna", "&f&lLuna Creciente", icon("LIGHT_GRAY_DYE", "INK_SACK"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    double drift = Math.sin(t * 0.6 + ctx.phase() * TAU) * 0.1;
                    ctx.emit(dust(Colors.hex("#E8F0FF"), 0.9f), ctx.above(2.7 + drift),
                            SHAPE_CRESCENT.facingYaw(ctx.yaw()),
                            ColorRamp.between(Colors.hex("#FFFFFF"), Colors.hex("#8FA8D0")));
                    // A few stars around it, lit on their own noise so they
                    // twinkle instead of blinking together.
                    for (int i = 0; i < ctx.count(3); i++) {
                        if (Noise.at(t * 1.8 + i * 9 + ctx.seed()) < 0) continue;
                        double angle = i * 2.4 + t * 0.4;
                        ctx.emit(END_ROD, Math.cos(angle) * 1.3, 2.6 + Math.sin(i * 3.0) * 0.4,
                                Math.sin(angle) * 1.3);
                    }
                })
                .interval(3)
                .range(30);
    }

    /**
     * @return a lotus opening at the wearer's feet and closing when they crouch
     */
    public static ParticleEffectType florDeLoto() {
        return new ParticleEffectType("loto", "&d&lFlor de Loto", icon("LILY_PAD", "WATER_LILY"),
                (ContextRenderer) ctx -> {
                    // The bloom opens over two seconds and then breathes.
                    double open = Math.min(1, ctx.time() * 0.5);
                    double breath = 1 + Math.sin(ctx.time() * 1.1 + ctx.phase() * TAU) * 0.06;
                    ctx.emit(dust(Colors.hex("#FF9AC8"), 1.0f), ctx.feet(),
                            SHAPE_FLOWER.scale(Easing.EASE_OUT_CUBIC.apply(open) * breath)
                                    .rotateY(ctx.time() * 0.35),
                            ColorRamp.of(Colors.SAKURA).mirror());
                    if (ctx.tick() % 8 == 0) ctx.emit(PETAL, ctx.above(0.9));
                })
                .sneaking(ctx -> ctx.emit(dust(Colors.hex("#FF9AC8"), 1.0f), ctx.feet(),
                        SHAPE_FLOWER.scale(0.45), ColorRamp.of(Colors.SAKURA)))
                .interval(3)
                .range(28);
    }

    // --------------------------------------------------------------- cosmic

    public static ParticleEffectType rastroColorido() {
        return new ParticleEffectType("rastro_colorido", "&b&lRastro Colorido", icon("LIGHT_BLUE_DYE", "INK_SACK"),
                (ContextRenderer) ctx -> ctx.emit(dust(Colors.rainbow(ctx.time() * 0.4), 1.2f), 0, 0.15, 0))
                // The whole point of a trail is that it is continuous, so it is
                // drawn along the ground actually covered since the last frame.
                .moving(ctx -> ctx.trail(4, (position, progress) ->
                        ctx.emit(dust(Colors.rainbow(ctx.time() * 0.4 + progress * 0.1), 1.2f),
                                position.clone().add(0, 0.15, 0))))
                .interval(1)
                .range(30);
    }

    public static ParticleEffectType tornado() {
        return new ParticleEffectType("tornado", "&7Tornado", icon("GRAY_WOOL", "WOOL"),
                (ContextRenderer) ctx -> {
                    // Wider and faster with height; without that it reads as a
                    // spinning cylinder rather than as a tornado.
                    double t = ctx.time();
                    int layers = ctx.count(9);
                    for (int layer = 0; layer < layers; layer++) {
                        double climb = layer / (double) layers;
                        double radius = 0.25 + climb * 1.5;
                        double angle = t * 5 * (1 + climb) + layer;
                        ctx.emit(SMOKE, Math.cos(angle) * radius, climb * 3.0, Math.sin(angle) * radius);
                    }
                    ctx.emit(dust(Colors.hex("#C8C8C8"), 1.0f), ctx.feet(),
                            SHAPE_RING.scale(1.7).rotateY(t * 3), ColorRamp.of(Colors.SHADOW));
                })
                .interval(2)
                .range(30);
    }

    public static ParticleEffectType rayoElectrico() {
        return new ParticleEffectType("electrico", "&e&lElectrico", icon("LIGHTNING_ROD", "IRON_INGOT"),
                (ContextRenderer) ctx -> {
                    // A different bolt each frame, which is what makes it
                    // flicker; a bolt that persists reads as a wire.
                    double angle = ctx.random() * TAU;
                    Vector to = new Vector(Math.cos(angle) * 1.3, 2.2, Math.sin(angle) * 1.3);
                    ctx.emit(dust(Colors.hex("#FFF37A"), 0.8f), ctx.above(0.2),
                            Curves.lightning(new Vector(0, 0, 0), to, 0.35, 3),
                            ColorRamp.between(Colors.hex("#FFFFFF"), Colors.hex("#4A6BFF")));
                    if (ctx.tick() % 4 == 0) ctx.emit(ELECTRIC, 0, 1.1, 0);
                })
                .interval(4)
                .range(28);
    }

    /**
     * @return a galaxy turning overhead, tilted so it reads as a disc
     */
    public static ParticleEffectType galaxia() {
        return new ParticleEffectType("galaxia", "&5&lGalaxia", icon("END_CRYSTAL", "NETHER_STAR"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#B478FF"), 1.0f), ctx.above(2.4),
                            SHAPE_GALAXY.rotateY(t * 0.6).rotateX(0.5),
                            ColorRamp.of(Colors.NEBULA).jitter(12));
                    ctx.emit(dust(Colors.hex("#FFF0FF"), 1.5f), 0, 2.4, 0);
                    if (ctx.tick() % 6 == 0) ctx.emit(END_ROD, ctx.above(2.4));
                })
                .interval(3)
                .range(32);
    }

    /**
     * @return curtains of northern light standing around the wearer
     */
    public static ParticleEffectType auroraBoreal() {
        return new ParticleEffectType("aurora", "&b&lAurora Boreal", icon("PRISMARINE_CRYSTALS", "GLOWSTONE_DUST"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    int curtains = ctx.count(8);
                    for (int i = 0; i < curtains; i++) {
                        double angle = TAU * i / curtains + t * 0.3;
                        // Each curtain ripples on its own noise, which is what
                        // gives the sheet its fold.
                        double ripple = Noise.at(t * 0.9 + i * 2.0 + ctx.seed()) * 0.35;
                        double radius = 1.5 + ripple;
                        double height = 1.4 + Noise.at(t * 0.6 + i * 3.0) * 0.6;
                        for (int step = 0; step < 4; step++) {
                            double climb = step / 3.0;
                            ctx.emit(dust(Colors.gradient(Colors.AURORA, climb), 1.1f),
                                    Math.cos(angle) * radius, 0.6 + climb * height, Math.sin(angle) * radius);
                        }
                    }
                })
                .interval(3)
                .range(32);
    }

    /**
     * @return a rift that pulls light in and spits it back out
     */
    public static ParticleEffectType portalDimensional() {
        return new ParticleEffectType("portal", "&5&lPortal Dimensional", icon("ENDER_PEARL", "ENDER_EYE"),
                (ContextRenderer) ctx -> {
                    double t = ctx.time();
                    ctx.emit(dust(Colors.hex("#8A5AD8"), 1.1f), ctx.above(1.1),
                            SHAPE_RING.scale(1.15).rotateY(t * 2).rotateX(Math.PI / 2).facingYaw(ctx.yaw()),
                            ColorRamp.of(Colors.ENDER).repeat(2));
                    for (int i = 0; i < ctx.count(4); i++) {
                        double phase = (t * 0.8 + i / 4.0) % 1.0;
                        double angle = phase * TAU * 2;
                        double radius = 1.15 * (1 - phase);
                        ctx.emit(PORTAL, Math.cos(angle) * radius, 1.1, Math.sin(angle) * radius);
                    }
                })
                .sound(Sounds.PORTAL_TRAVEL, 70, 0.08f, 1.6f, 2.0f)
                .interval(2)
                .range(28);
    }

    /**
     * @return a shockwave ring that fires out of the wearer's feet every
     * second and a half
     */
    public static ParticleEffectType ondaDeChoque() {
        return new ParticleEffectType("onda", "&f&lOnda de Choque", icon("HEAVY_CORE", "ANVIL"),
                (ContextRenderer) ctx -> {
                    // One wave every 1.5 seconds, expanding on an easing curve
                    // so it leaves fast and settles slowly, the way an impact
                    // actually looks.
                    double phase = (ctx.time() / 1.5) % 1.0;
                    double radius = Easing.EASE_OUT_CUBIC.between(0.2, 2.6, phase);
                    float size = (float) (1.4 - phase * 0.8);
                    ctx.emit(dust(Colors.hex("#EAF2FF"), Math.max(0.4f, size)), ctx.feet(),
                            SHAPE_RING_FINE.scale(radius),
                            ColorRamp.between(Colors.hex("#FFFFFF"), Colors.hex("#5A7AA8")));
                    if (phase < 0.08) ctx.emit(CLOUD, 0, 0.1, 0);
                })
                .interval(2)
                .range(30);
    }

    // --------------------------------------------------------------- helpers

    /**
     * Registers every effect in this catalogue.
     */
    public static void registerDefaults() {
        CosmeticRegistry.registerAll(
                auraDeFuego(), anillosDeLlama(), llamaDemoniaca(), fenix(), magma(),
                senorDelHielo(), copoDeNieve(), nubeDeNieve(), ventisca(),
                nubeDeLluvia(), paraguas(), burbujas(),
                alasDeAngel(), alasDeVampiro(), alasArcoiris(), alasDeDragon(), alasDeMariposa(),
                enamorado(), corazonGigante(), lluviaDePetalos(),
                encantado(), auraLegendaria(), escudoProtector(), agujeroNegro(), yinYang(),
                circuloDeRunas(), coronaReal(), orbitaAtomica(), divino(), espiritusDelBosque(),
                lunaCreciente(), florDeLoto(),
                notasMusicales(), confeti(), horaDeFiesta(), estrella(), luciernagas(),
                calavera(), helicesDeSangre(), caminarEnSombras(), almasEnPena(), toxico(),
                rastroColorido(), tornado(), rayoElectrico(), galaxia(), auroraBoreal(),
                portalDimensional(), ondaDeChoque());
    }

    /**
     * Configures the shared dust builder.
     *
     * @param color dust colour
     * @param size  dust size
     * @return the shared builder, ready to spawn
     */
    private static ParticleBuilder dust(Color color, float size) {
        return DUST.color(color, size);
    }

    /**
     * Configures the shared transition-dust builder, whose particles fade from
     * one colour to another over their own lifetime rather than needing a
     * second layer.
     *
     * @param from colour each particle starts at
     * @param to   colour it fades towards
     * @param size dust size
     * @return the shared builder, ready to spawn
     */
    private static ParticleBuilder fade(Color from, Color to, float size) {
        return FADE.color(from, size).fadeTo(to);
    }

    /**
     * One beat of a heartbeat: a half sine that starts at {@code at} and is
     * over an eighth of a second later.
     *
     * @param cycle position in the beat cycle, in {@code 0..1}
     * @param at    where this beat starts
     * @return the beat's contribution, in {@code 0..1}
     */
    private static double beat(double cycle, double at) {
        double since = cycle - at;
        if (since < 0 || since > 0.12) return 0;
        return Math.sin(Math.PI * (since / 0.12));
    }

    private static ItemStack icon(String... names) {
        Material material = Items.material(names);
        return new ItemStack(material);
    }
}
