package com.ricodevvv.aurora.packet;

import com.ricodevvv.aurora.util.ServerVersion;

/**
 * Indices de entity metadata por version.
 *
 * ESTO es lo que PacketEvents NO resuelve por ti: los wrappers unifican los
 * paquetes, pero el indice y el tipo de cada campo los mandas crudos. Y los
 * indices se recorrieron varias veces porque Mojang fue metiendo campos en
 * medio de las clases base.
 *
 * Historia de los corrimientos (de minecraft.wiki / wiki.vg):
 *  - 1.8            Entity 0-3, Living 6-9,   ArmorStand empieza en 10
 *  - 1.9  a 1.13    Entity 0-5, Living 6-10,  ArmorStand empieza en 11
 *  - 1.14           +Pose en Entity(6) y +BedLocation al final de Living  -> 13
 *  - 1.15 a 1.16    +StingerCount en Living                               -> 14
 *  - 1.17+          +FrozenTicks en Entity(7)                             -> 15
 *
 * Si algun dia se recorre otra vez, este archivo es el unico que se toca.
 */
public final class MetaIndex {

    private MetaIndex() {
    }

    /** Bitmask base de Entity. 0x20 = invisible, 0x40 = glowing. Nunca se movio. */
    public static final int ENTITY_FLAGS = 0;

    public static final byte FLAG_INVISIBLE = 0x20;
    public static final byte FLAG_GLOWING = 0x40;

    /** Custom name. OJO: en 1.13+ el TIPO cambia a Optional&lt;Component&gt;. */
    public static final int CUSTOM_NAME = 2;

    public static final int CUSTOM_NAME_VISIBLE = 3;

    /** Bitmask de ArmorStand: 0x01 small, 0x04 arms, 0x08 sin base, 0x10 marker. */
    public static int armorStandFlags() {
        if (ServerVersion.isLegacy()) return 10;
        if (!ServerVersion.atLeast(14)) return 11;
        if (!ServerVersion.atLeast(15)) return 13;
        if (!ServerVersion.atLeast(17)) return 14;
        return 15;
    }

    public static final byte STAND_SMALL = 0x01;
    public static final byte STAND_ARMS = 0x04;
    public static final byte STAND_NO_BASE_PLATE = 0x08;
    public static final byte STAND_MARKER = 0x10;

    /** Bitmask de Insentient (0x01 = NoAI). Mismo corrimiento que ArmorStand. */
    public static int mobFlags() {
        return armorStandFlags();
    }

    public static final byte MOB_NO_AI = 0x01;

    /** "Es bebe" de Ageable: va justo despues del bitmask de Insentient. */
    public static int ageableBaby() {
        // En 1.8 el bebe de Ageable era un byte de "age" en el indice 12.
        return ServerVersion.isLegacy() ? 12 : mobFlags() + 1;
    }

    /** true si el custom name viaja como Component en vez de String (1.13+). */
    public static boolean nameIsComponent() {
        return ServerVersion.isFlattened();
    }

    /** true si "name visible" es Boolean en vez de Byte (1.9+). */
    public static boolean nameVisibleIsBoolean() {
        return !ServerVersion.isLegacy();
    }
}
