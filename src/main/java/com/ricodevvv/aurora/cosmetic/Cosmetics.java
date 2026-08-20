package com.ricodevvv.aurora.cosmetic;

import com.ricodevvv.aurora.cosmetic.balloon.BalloonType;
import com.ricodevvv.aurora.cosmetic.pet.PetType;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Entities;
import com.ricodevvv.aurora.util.Items;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * A starter catalogue of balloons and pets.
 *
 * <p>Materials are resolved through {@code Items.material(...)} with aliases,
 * because the 1.13 flattening renamed nearly everything: {@code WOOL:14} became
 * {@code RED_WOOL}, {@code SKULL_ITEM} became {@code PLAYER_HEAD}, and so on.
 *
 * <p>Call {@link #registerDefaults()} from {@code onEnable}, or copy these as
 * templates for your own.
 */
public final class Cosmetics {

    private Cosmetics() {
    }

    // -------------------------------------------------------------- balloons

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

    /**
     * @return a rainbow balloon with a colour-cycling trail
     */
    public static BalloonType rainbowBalloon() {
        return new BalloonType("arcoiris", "&d&lGlobo Arcoiris", wool((byte) 2, "MAGENTA_WOOL"))
                .popColor(Colors.hex("#FF60FF"))
                .ambient(Particles.of(ParticleType.DUST).size(0.8f), 3)
                .leashLength(3.0);
    }

    /**
     * @return a small cloud trailing vapour instead of a balloon
     */
    public static BalloonType cloudBalloon() {
        return new BalloonType("nube", "&fNubecita", wool((byte) 0, "WHITE_WOOL"))
                .height(2.6)
                .leashLength(3.2)
                .ambient(Particles.of(ParticleType.CLOUD).count(2).offset(0.15).speed(0.01), 4);
    }

    /**
     * @return a skull balloon, using a head instead of wool
     */
    public static BalloonType skullBalloon() {
        return new BalloonType("calavera", "&8Globo Calavera", Items.head())
                .popColor(Colors.hex("#404040"))
                .ambient(Particles.of(ParticleType.LARGE_SMOKE).speed(0.01), 6);
    }

    /**
     * A balloon whose tether is drawn with particles rather than a real lead.
     *
     * <p>Useful when extra mobs in the world are a problem: anti-cheats, entity
     * counters, or setups where the anchor drifts out of sync.
     *
     * @return the smoke balloon
     */
    public static BalloonType particleLeashBalloon() {
        return new BalloonType("humo", "&7Globo de Humo", wool((byte) 8, "LIGHT_GRAY_WOOL"))
                .leashMode(BalloonType.LeashMode.PARTICLE)
                .popColor(Colors.hex("#9A9A9A"));
    }

    // ------------------------------------------------------------------ pets

    /**
     * @return a floating head pet, which is the only rendering that looks
     * genuinely small on 1.8 as well as on 1.21
     */
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

    /**
     * @return a real baby wolf; on 1.8 "baby" is as small as an entity gets
     */
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

    /**
     * @return a hovering pet trailing flames
     */
    public static PetType emberSprite() {
        return new PetType("brasa", "&6Brasa", new ItemStack(Items.material("BLAZE_POWDER", "FIRE_CHARGE")))
                .head(new ItemStack(Items.material("MAGMA_BLOCK", "NETHERRACK")))
                .hover(0.35)
                .speed(0.34)
                .followDistance(2.6)
                .trail(Particles.of(ParticleType.FLAME).count(1).speed(0.01), 2);
    }

    // --------------------------------------------------------------- helpers

    /**
     * Registers the whole starter catalogue: balloons, pets and particle
     * effects.
     */
    public static void registerDefaults() {
        com.ricodevvv.aurora.cosmetic.particle.ParticleEffects.registerDefaults();
        CosmeticRegistry.registerAll(
                redBalloon(), blueBalloon(), rainbowBalloon(), cloudBalloon(),
                skullBalloon(), particleLeashBalloon(),
                miniCreeper(), miniZombie(), puppy(), chick(), emberSprite());
    }

    /**
     * Resolves coloured wool on either side of the flattening.
     *
     * @param data     legacy data value used before 1.13
     * @param flatName flattened material name used from 1.13
     * @return the wool item
     */
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
