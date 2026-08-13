package com.ricodevvv.aurora.cosmetic;

import com.ricodevvv.aurora.cosmetic.balloon.BalloonType;
import com.ricodevvv.aurora.cosmetic.pet.PetType;
import com.cryptomorin.xseries.particles.XParticle;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Entities;
import com.ricodevvv.aurora.util.Items;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Catalogo de arranque. Todos los materiales van por Items.material(...) con
 * alias, porque el flattening de 1.13 renombro casi todo (WOOL:14 ->
 * RED_WOOL, SKULL_ITEM -> PLAYER_HEAD, etc).
 *
 * Llama registerDefaults() en tu onEnable, o copia estos y arma los tuyos.
 */
public final class Cosmetics {

    private Cosmetics() {
    }

    // --------------------------------------------------------------- globos

    public static BalloonType redBalloon() {
        return new BalloonType("rojo", "&cGlobo Rojo", wool((byte) 14, "RED_WOOL"))
                .popColor(Colors.hex("#E03030"))
                .ambient(Particles.dust(Colors.hex("#E03030")).size(0.7f), 8);
    }

    public static BalloonType blueBalloon() {
        return new BalloonType("azul", "&9Globo Azul", wool((byte) 11, "BLUE_WOOL"))
                .popColor(Colors.hex("#3050E0"))
                .ambient(Particles.dust(Colors.hex("#3050E0")).size(0.7f), 8);
    }

    /** Globo arcoiris: el rastro cicla de color con la paleta VOID. */
    public static BalloonType rainbowBalloon() {
        return new BalloonType("arcoiris", "&d&lGlobo Arcoiris", wool((byte) 2, "MAGENTA_WOOL"))
                .popColor(Colors.hex("#FF60FF"))
                .ambient(Particles.of(XParticle.DUST).size(0.8f), 3)
                .leashLength(3.0);
    }

    /** Nube: en vez de un globo, un bloque de lana blanca soltando vapor. */
    public static BalloonType cloudBalloon() {
        return new BalloonType("nube", "&fNubecita", wool((byte) 0, "WHITE_WOOL"))
                .height(2.6)
                .leashLength(3.2)
                .ambient(Particles.of(XParticle.CLOUD).count(2).offset(0.15).speed(0.01), 4);
    }

    /** Globo de calavera, con cabeza en vez de lana. */
    public static BalloonType skullBalloon() {
        return new BalloonType("calavera", "&8Globo Calavera", Items.head())
                .popColor(Colors.hex("#404040"))
                .ambient(Particles.of(XParticle.LARGE_SMOKE).speed(0.01), 6);
    }

    /**
     * Version sin correa real: la cuerda se dibuja con particulas.
     * Util si no quieres mobs extra en el mundo (anticheat, contadores de
     * entidades, o servidores donde el ancla se te desincroniza).
     */
    public static BalloonType particleLeashBalloon() {
        return new BalloonType("humo", "&7Globo de Humo", wool((byte) 8, "LIGHT_GRAY_WOOL"))
                .leashMode(BalloonType.LeashMode.PARTICLE)
                .popColor(Colors.hex("#9A9A9A"));
    }

    // ------------------------------------------------------------ mascotas

    /** Mini pet de verdad en 1.8: cabeza flotante, chiquita en cualquier version. */
    public static PetType miniCreeper() {
        return new PetType("creeper", "&aMini Creeper", head("creeper"))
                .head(head("creeper"))
                .hover(0.25)
                .speed(0.3)
                .trail(Particles.dust(Colors.hex("#44DD44")).size(0.6f), 6);
    }

    public static PetType miniZombie() {
        return new PetType("zombie", "&2Mini Zombie", head("zombie"))
                .head(head("zombie"))
                .hover(0.2)
                .speed(0.28);
    }

    /** Mascota clasica: lobo bebe real. En 1.8 "bebe" es lo mas chico posible. */
    public static PetType puppy() {
        return new PetType("perrito", "&fPerrito", new ItemStack(Material.getMaterial("BONE")))
                .mob(Entities.type("WOLF"))
                .baby(true)
                .scale(0.6)
                .speed(0.32);
    }

    public static PetType chick() {
        return new PetType("pollito", "&ePollito", new ItemStack(Material.getMaterial("EGG")))
                .mob(Entities.type("CHICKEN"))
                .baby(true)
                .scale(0.5)
                .speed(0.3);
    }

    /** Mascota flotante con estela de fuego. */
    public static PetType emberSprite() {
        return new PetType("brasa", "&6Brasa", new ItemStack(Items.material("BLAZE_POWDER", "FIRE_CHARGE")))
                .head(new ItemStack(Items.material("MAGMA_BLOCK", "NETHERRACK")))
                .hover(0.35)
                .speed(0.34)
                .followDistance(2.6)
                .trail(Particles.of(XParticle.FLAME).count(1).speed(0.01), 2);
    }

    // --------------------------------------------------------------- helpers

    /** Registra todo el catalogo de arranque (globos, mascotas y efectos). */
    public static void registerDefaults() {
        com.ricodevvv.aurora.cosmetic.particle.ParticleEffects.registerDefaults();
        CosmeticRegistry.registerAll(
                redBalloon(), blueBalloon(), rainbowBalloon(), cloudBalloon(),
                skullBalloon(), particleLeashBalloon(),
                miniCreeper(), miniZombie(), puppy(), chick(), emberSprite());
    }

    /** Lana por data value en 1.8, o por nombre plano en 1.13+. */
    @SuppressWarnings("deprecation")
    private static ItemStack wool(byte data, String flatName) {
        Material flat = Material.getMaterial(flatName);
        if (flat != null) return new ItemStack(flat);
        return new ItemStack(Items.material("WOOL"), 1, (short) data);
    }

    private static ItemStack head(String owner) {
        return Items.head(owner);
    }
}
