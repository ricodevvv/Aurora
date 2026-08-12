package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.CosmeticRegistry;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.shape.Curves;
import com.ricodevvv.aurora.shape.Glyphs;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Items;
import com.ricodevvv.aurora.util.Sounds;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Catalogo de efectos.
 *
 * Los builders de particulas se crean UNA vez como constantes y se reusan: son
 * mutables a proposito, asi que dentro de un renderer solo se les cambia color
 * o material antes de spawnear. Crear un builder por tick con 50 jugadores
 * equipados es basura de GC gratis.
 *
 * Las figuras estaticas tambien se precalculan (SHAPE_*), porque generar una
 * esfera de 80 puntos cada tick es trigonometria pura desperdiciada.
 */
public final class ParticleEffects {

    private static final double TAU = Math.PI * 2;

    // Builders reusables
    private static final ParticleBuilder DUST = Particles.of(ParticleType.DUST).count(1).speed(0);
    private static final ParticleBuilder FLAME = Particles.of(ParticleType.FLAME).count(1).speed(0.01);
    private static final ParticleBuilder CLOUD = Particles.of(ParticleType.CLOUD).count(1).speed(0.01);
    private static final ParticleBuilder SMOKE = Particles.of(ParticleType.LARGE_SMOKE).count(1).speed(0.01);
    private static final ParticleBuilder HEART = Particles.of(ParticleType.HEART).count(1).speed(0);
    private static final ParticleBuilder NOTE = Particles.of(ParticleType.NOTE).count(1);
    private static final ParticleBuilder ENCHANT = Particles.of(ParticleType.ENCHANT).count(1).speed(0.5);
    private static final ParticleBuilder CRIT = Particles.of(ParticleType.CRIT).count(1).speed(0.05);
    private static final ParticleBuilder DRIP = Particles.of(ParticleType.DRIPPING_WATER).count(1).speed(0);
    private static final ParticleBuilder SPLASH = Particles.of(ParticleType.SPLASH).count(2).offset(0.1).speed(0.05);
    private static final ParticleBuilder PORTAL = Particles.of(ParticleType.PORTAL).count(1).speed(0.4);
    private static final ParticleBuilder FIREWORK = Particles.of(ParticleType.FIREWORK).count(1).speed(0.05);

    // Figuras precalculadas
    private static final Shape SHAPE_RING = Shapes.circle(1, 24);
    private static final Shape SHAPE_DOME = Shapes.dome(1.8, 60);
    private static final Shape SHAPE_WINGS = Shapes.wings(1.6, 6, 8);
    private static final Shape SHAPE_STAR = Shapes.star(5, 0.7, 0.3, 5);
    private static final Shape SHAPE_HEART = Shapes.heart(0.9, 40);
    private static final Shape SHAPE_SNOWFLAKE = Shapes.rose(3, 1.0, 60);

    private ParticleEffects() {
    }

    // --------------------------------------------------------------- fuego

    public static ParticleEffectType auraDeFuego() {
        return new ParticleEffectType("aura_fuego", "&6Aura de Fuego", icon("BLAZE_POWDER", "FIRE_CHARGE"),
                (player, at, tick) -> {
                    double angle = tick * 0.3;
                    for (int i = 0; i < 3; i++) {
                        double a = angle + TAU * i / 3;
                        double y = (tick % 40) / 40.0 * 2.2;
                        FLAME.range(28).spawn(at.clone().add(
                                Math.cos(a) * 0.9, y, Math.sin(a) * 0.9));
                    }
                }).interval(1);
    }

    public static ParticleEffectType anillosDeLlama() {
        return new ParticleEffectType("anillos_llama", "&cAnillos de Llama", icon("MAGMA_CREAM", "SLIME_BALL"),
                (player, at, tick) -> {
                    // Tres anillos a distinta altura girando en sentidos opuestos:
                    // es lo que evita que se vea como un cilindro plano.
                    for (int ring = 0; ring < 3; ring++) {
                        double y = 0.3 + ring * 0.7;
                        double spin = (ring % 2 == 0 ? 1 : -1) * tick * 0.12;
                        double radius = 0.8 + Math.sin(tick * 0.06 + ring) * 0.15;
                        FLAME.range(28).spawn(at.clone().add(0, y, 0),
                                SHAPE_RING.scale(radius).rotateY(spin));
                    }
                })
                // Al caminar: dos estelas a los costados, girando con el yaw.
                // Sus constantes: rango 0.1, count 1, offset (0.3, 0.0, 0.6), speed 0.
                .moving((player, at, tick) -> {
                    double yaw = Math.toRadians(at.getYaw());
                    double dx = 0.1 * Math.cos(yaw), dz = 0.1 * Math.sin(yaw);
                    ParticleBuilder trail = FLAME.count(1).offset(0.3, 0.0, 0.6).speed(0).range(28);
                    trail.spawn(at.clone().add(dx, 0, dz));
                    trail.spawn(at.clone().add(-dx, 0, -dz));
                    FLAME.offset(0, 0, 0).speed(0.01);
                })
                .interval(2);
    }

    public static ParticleEffectType llamaDemoniaca() {
        return new ParticleEffectType("llama_demoniaca", "&4Llama Demoniaca", icon("NETHER_STAR"),
                (player, at, tick) -> {
                    double t = (tick % 60) / 60.0;
                    Shape vortex = Shapes.vortex(1.2, 2.4, 3, 40).rotateY(tick * 0.15);
                    DUST.color(Colors.gradient(Colors.FIRE, t)).size(1.1f).range(30)
                            .spawn(at, vortex);
                }).interval(2);
    }

    // ---------------------------------------------------------------- hielo

    public static ParticleEffectType senorDelHielo() {
        return new ParticleEffectType("senor_hielo", "&bSenor del Hielo", icon("PACKED_ICE", "ICE"),
                (player, at, tick) -> {
                    double t = (Math.sin(tick * 0.05) + 1) / 2;
                    DUST.color(Colors.gradient(Colors.ICE, t)).size(1.0f).range(30)
                            .spawn(at.clone().add(0, 0.1, 0),
                                    SHAPE_RING.scale(1.4).rotateY(tick * 0.08));
                    // Cristales cayendo alrededor
                    if (tick % 4 == 0) {
                        double a = random() * TAU;
                        DUST.color(Colors.hex("#DFF6FF")).size(0.8f).spawn(at.clone().add(
                                Math.cos(a) * 1.2, 2.2 + random(), Math.sin(a) * 1.2));
                    }
                }).interval(2);
    }

    public static ParticleEffectType copoDeNieve() {
        return new ParticleEffectType("copo_nieve", "&fCopo de Nieve", icon("SNOWBALL", "SNOW_BALL"),
                (player, at, tick) -> DUST.color(Colors.hex("#EAF6FF")).size(0.9f).range(28)
                        .spawn(at.clone().add(0, 2.6, 0),
                                SHAPE_SNOWFLAKE.rotateY(tick * 0.05))).interval(3);
    }

    public static ParticleEffectType nubeDeNieve() {
        return new ParticleEffectType("nube_nieve", "&fNube de Nieve", icon("SNOW_BLOCK", "SNOW"),
                (player, at, tick) -> {
                    Location cloud = at.clone().add(0, 2.8, 0);
                    CLOUD.range(28).spawn(cloud, Shapes.cloud(0.55, 6));
                    if (tick % 2 == 0) {
                        double a = random() * TAU, r = random() * 0.6;
                        DUST.color(Color.WHITE).size(0.7f).spawn(cloud.clone().add(
                                Math.cos(a) * r, -random() * 2.0, Math.sin(a) * r));
                    }
                }).interval(2);
    }

    // ---------------------------------------------------------------- agua

    public static ParticleEffectType nubeDeLluvia() {
        return new ParticleEffectType("nube_lluvia", "&8Nube de Lluvia", icon("WATER_BUCKET"),
                (player, at, tick) -> {
                    Location cloud = at.clone().add(0, 2.8, 0);
                    SMOKE.range(28).spawn(cloud, Shapes.cloud(0.6, 5));
                    for (int i = 0; i < 2; i++) {
                        double a = random() * TAU, r = random() * 0.7;
                        DRIP.spawn(cloud.clone().add(Math.cos(a) * r, -0.3, Math.sin(a) * r));
                    }
                    // Salpicaduras en el suelo
                    if (tick % 4 == 0) {
                        double a = random() * TAU, r = random() * 0.8;
                        SPLASH.spawn(at.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r));
                    }
                }).interval(2);
    }

    public static ParticleEffectType paraguas() {
        return new ParticleEffectType("paraguas", "&9Paraguas", icon("LEAD", "STICK"),
                (player, at, tick) -> {
                    DUST.color(Colors.hex("#3355AA")).size(1.0f).range(26)
                            .spawn(at.clone().add(0, 2.3, 0), SHAPE_DOME.scale(0.55));
                    if (tick % 2 == 0) {
                        double a = random() * TAU;
                        DRIP.spawn(at.clone().add(Math.cos(a) * 1.05, 2.3 - random(), Math.sin(a) * 1.05));
                    }
                }).interval(3);
    }

    // ----------------------------------------------------------------- alas

    public static ParticleEffectType alasDeAngel() {
        return wings("alas_angel", "&fAlas de Angel", Items.head(), Colors.hex("#FFFBE0"), false);
    }

    public static ParticleEffectType alasDeVampiro() {
        return wings("alas_vampiro", "&4Alas de Vampiro", icon("REDSTONE"), Colors.hex("#8B1020"), false);
    }

    public static ParticleEffectType alasArcoiris() {
        return wings("alas_arcoiris", "&d&lAlas Arcoiris", icon("FEATHER"), null, true);
    }

    private static ParticleEffectType wings(String id, String name, ItemStack icon,
                                            Color color, boolean rainbow) {
        return new ParticleEffectType(id, name, icon, (player, at, tick) -> {
            // Batido suave y giro con el yaw para que siempre queden de espaldas
            double flap = 1 + Math.sin(tick * 0.12) * 0.12;
            double yaw = Math.toRadians(-at.getYaw());
            DUST.color(rainbow ? Colors.rainbow(tick * 0.01) : color).size(0.9f).range(30)
                    .spawn(at.clone().add(0, 1.1, 0), SHAPE_WINGS.scale(flap).rotateY(yaw));
        }).interval(2);
    }

    // ----------------------------------------------------------------- amor

    public static ParticleEffectType enamorado() {
        return new ParticleEffectType("enamorado", "&dEnamorado", icon("RED_DYE", "INK_SACK"),
                (player, at, tick) -> {
                    if (tick % 6 == 0) {
                        HEART.range(26).spawn(at.clone().add(
                                random() - 0.5, 1.8 + random() * 0.6, random() - 0.5));
                    }
                }).interval(2);
    }

    public static ParticleEffectType corazonGigante() {
        return new ParticleEffectType("corazon_gigante", "&cCorazon Gigante", icon("APPLE"),
                (player, at, tick) -> DUST.color(Colors.hex("#FF3B6B")).size(1.0f).range(30)
                        .spawn(at.clone().add(0, 2.4, 0),
                                SHAPE_HEART.rotateY(tick * 0.06))).interval(3);
    }

    // ------------------------------------------------------------- magicos

    public static ParticleEffectType encantado() {
        return new ParticleEffectType("encantado", "&5Encantado", icon("ENCHANTING_TABLE", "ENCHANTMENT_TABLE"),
                (player, at, tick) -> ENCHANT.range(26)
                        .spawn(at.clone().add(0, 2.2, 0))).interval(1);
    }

    public static ParticleEffectType auraLegendaria() {
        return new ParticleEffectType("aura_legendaria", "&6&lAura Legendaria", icon("GOLD_INGOT"),
                (player, at, tick) -> {
                    Shape helix = Shapes.doubleHelix(0.85, 2.4, 2, 24).rotateY(tick * 0.12);
                    DUST.color(Colors.hex("#FFD24A")).size(1.0f).range(32).spawn(at, helix);
                    if (tick % 8 == 0) CRIT.spawn(at.clone().add(0, 1.2, 0));
                }).interval(2);
    }

    public static ParticleEffectType escudoProtector() {
        return new ParticleEffectType("escudo", "&bEscudo Protector", icon("SHIELD", "IRON_CHESTPLATE"),
                (player, at, tick) -> {
                    double pulse = 1 + Math.sin(tick * 0.08) * 0.08;
                    DUST.color(Colors.hex("#66CCFF")).size(0.8f).range(30)
                            .spawn(at.clone().add(0, 0.2, 0),
                                    SHAPE_DOME.scale(pulse).rotateY(tick * 0.03));
                }).interval(3);
    }

    public static ParticleEffectType agujeroNegro() {
        return new ParticleEffectType("agujero_negro", "&8Agujero Negro", icon("COAL_BLOCK", "COAL"),
                (player, at, tick) -> {
                    // Espiral que se cierra: el radio depende de la fase de cada
                    // particula, no del tick global, para que el flujo sea continuo.
                    for (int i = 0; i < 4; i++) {
                        double phase = ((tick * 2 + i * 15) % 60) / 60.0;
                        double r = 2.2 * (1 - phase);
                        double a = phase * TAU * 3 + i;
                        PORTAL.range(30).spawn(at.clone().add(
                                Math.cos(a) * r, 1.0 + phase * 0.4, Math.sin(a) * r));
                    }
                }).interval(1);
    }

    public static ParticleEffectType yinYang() {
        return new ParticleEffectType("yin_yang", "&f&lYin &8&lYang", icon("BONE"),
                (player, at, tick) -> {
                    Shape half = Shapes.arc(1.1, 16, 0, 180).rotateY(tick * 0.09);
                    Shape other = Shapes.arc(1.1, 16, 180, 360).rotateY(tick * 0.09);
                    Location base = at.clone().add(0, 0.15, 0);
                    DUST.color(Color.WHITE).size(1.0f).range(28).spawn(base, half);
                    DUST.color(Colors.hex("#1A1A1A")).size(1.0f).spawn(base, other);
                }).interval(2);
    }

    // -------------------------------------------------------------- fiesta

    public static ParticleEffectType notasMusicales() {
        return new ParticleEffectType("notas", "&aNotas Musicales", icon("JUKEBOX"),
                // Quieto: la nota orbita a 0.6 de radio, 2.2 de altura.
                (player, at, tick) -> {
                    double angle = Math.toRadians(tick * 12);
                    NOTE.count(0).range(26).color(Colors.rainbow((tick % 24) / 24.0))
                            .spawn(at.clone().add(
                                    Math.cos(angle) * 0.6, 2.2, Math.sin(angle) * 0.6));
                    if (tick % 20 == 0) {
                        Sounds.NOTE_HARP.play(player, 0.3f, Sounds.pitchOf((int) (random() * 12)));
                    }
                })
                // Caminando: la nota sale a la altura del pecho, sin orbitar.
                .moving((player, at, tick) -> NOTE.count(0).range(26)
                        .color(Colors.rainbow((tick % 24) / 24.0))
                        .spawn(at.clone().add(0, 0.6, 0)))
                .interval(4);
    }

    public static ParticleEffectType confeti() {
        return new ParticleEffectType("confeti", "&e&lConfeti", icon("FIREWORK_ROCKET", "FIREWORK"),
                (player, at, tick) -> {
                    for (int i = 0; i < 3; i++) {
                        DUST.color(Colors.rainbow(random())).size(1.2f).range(28)
                                .spawn(at.clone().add(
                                        (random() - 0.5) * 1.6,
                                        2.6 - ((tick * 3 + i * 7) % 30) / 30.0 * 2.4,
                                        (random() - 0.5) * 1.6));
                    }
                }).interval(2);
    }

    public static ParticleEffectType horaDeFiesta() {
        return new ParticleEffectType("fiesta", "&d&lHora de Fiesta", icon("GLOWSTONE_DUST"),
                (player, at, tick) -> {
                    double a = tick * 0.25;
                    for (int i = 0; i < 4; i++) {
                        double angle = a + TAU * i / 4;
                        FIREWORK.range(28).spawn(at.clone().add(
                                Math.cos(angle) * 1.1,
                                1.0 + Math.sin(tick * 0.15 + i) * 0.5,
                                Math.sin(angle) * 1.1));
                    }
                }).interval(2);
    }

    public static ParticleEffectType estrella() {
        return new ParticleEffectType("estrella", "&eEstrella", icon("NETHER_STAR"),
                (player, at, tick) -> DUST.color(Colors.hex("#FFE86B")).size(1.0f).range(28)
                        .spawn(at.clone().add(0, 2.5, 0),
                                SHAPE_STAR.rotateY(tick * 0.08))).interval(3);
    }

    // ------------------------------------------------------------- oscuros

    public static ParticleEffectType calavera() {
        return new ParticleEffectType("calavera", "&8Calavera", Items.head(),
                (player, at, tick) -> {
                    double yaw = Math.toRadians(-at.getYaw());
                    DUST.color(Colors.hex("#DDDDDD")).size(0.8f).range(28)
                            .spawn(at.clone().add(0, 2.6, 0),
                                    Glyphs.sprite(Glyphs.SKULL, 0.12).rotateY(yaw));
                }).interval(3);
    }

    public static ParticleEffectType helicesDeSangre() {
        return new ParticleEffectType("helices_sangre", "&4Helices de Sangre", icon("REDSTONE_BLOCK"),
                (player, at, tick) -> DUST.color(Colors.hex("#A00818")).size(1.0f).range(30)
                        .spawn(at, Shapes.doubleHelix(0.7, 2.2, 2, 20)
                                .rotateY(-tick * 0.15))).interval(2);
    }

    public static ParticleEffectType caminarEnSombras() {
        return new ParticleEffectType("sombras", "&8Caminar en Sombras", icon("COAL"),
                (player, at, tick) -> {
                    for (int i = 0; i < 3; i++) {
                        DUST.color(Colors.hex("#2A0A3A")).size(1.3f).range(26)
                                .spawn(at.clone().add(
                                        (random() - 0.5) * 0.8, 0.08, (random() - 0.5) * 0.8));
                    }
                }).interval(2);
    }

    // ------------------------------------------------------------- rastros

    public static ParticleEffectType rastroColorido() {
        return new ParticleEffectType("rastro_colorido", "&b&lRastro Colorido", icon("LIGHT_BLUE_DYE", "INK_SACK"),
                (player, at, tick) -> DUST.color(Colors.rainbow(tick * 0.02)).size(1.2f).range(30)
                        .spawn(at.clone().add(0, 0.15, 0)))
                .interval(1);
    }

    public static ParticleEffectType tornado() {
        return new ParticleEffectType("tornado", "&7Tornado", icon("GRAY_WOOL", "WOOL"),
                (player, at, tick) -> {
                    // Mientras mas arriba, mas ancho y mas rapido: sin eso se ve
                    // como un cilindro girando, no como un tornado.
                    for (int layer = 0; layer < 8; layer++) {
                        double t = layer / 8.0;
                        double r = 0.25 + t * 1.5;
                        double angle = tick * 0.25 * (1 + t) + layer;
                        SMOKE.range(30).spawn(at.clone().add(
                                Math.cos(angle) * r, t * 3.0, Math.sin(angle) * r));
                    }
                }).interval(2);
    }

    public static ParticleEffectType circuloDeRunas() {
        return new ParticleEffectType("runas", "&5Circulo de Runas", icon("AMETHYST_SHARD", "EMERALD"),
                (player, at, tick) -> DUST.color(Colors.gradient(Colors.VOID,
                                (Math.sin(tick * 0.05) + 1) / 2)).size(1.1f).range(30)
                        .spawn(at.clone().add(0, 0.08, 0),
                                Shapes.runeCircle(1.5, 6, 0.4, 40).rotateY(tick * 0.04)))
                .interval(3);
    }

    public static ParticleEffectType rayoElectrico() {
        return new ParticleEffectType("electrico", "&e&lElectrico", icon("LIGHTNING_ROD", "IRON_INGOT"),
                (player, at, tick) -> {
                    // Cada frame es un rayo distinto; por eso se ve "vivo".
                    double a = random() * TAU;
                    Vector to = new Vector(Math.cos(a) * 1.3, 2.2, Math.sin(a) * 1.3);
                    DUST.color(Colors.hex("#FFF37A")).size(0.8f).range(28)
                            .spawn(at.clone().add(0, 0.2, 0),
                                    Curves.lightning(new Vector(0, 0, 0), to, 0.35, 3));
                }).interval(4);
    }

    // -------------------------------------------------------------- helpers

    /** Registra todo el catalogo. */
    public static void registerDefaults() {
        CosmeticRegistry.registerAll(
                auraDeFuego(), anillosDeLlama(), llamaDemoniaca(),
                senorDelHielo(), copoDeNieve(), nubeDeNieve(),
                nubeDeLluvia(), paraguas(),
                alasDeAngel(), alasDeVampiro(), alasArcoiris(),
                enamorado(), corazonGigante(),
                encantado(), auraLegendaria(), escudoProtector(), agujeroNegro(), yinYang(),
                notasMusicales(), confeti(), horaDeFiesta(), estrella(),
                calavera(), helicesDeSangre(), caminarEnSombras(),
                rastroColorido(), tornado(), circuloDeRunas(), rayoElectrico());
    }

    private static ItemStack icon(String... names) {
        Material material = Items.material(names);
        return new ItemStack(material);
    }

    private static double random() {
        return ThreadLocalRandom.current().nextDouble();
    }
}
