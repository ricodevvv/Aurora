package com.ricodevvv.aurora.packet;

import com.ricodevvv.aurora.util.ServerVersion;

/**
 * Entity metadata indices, resolved per server version.
 *
 * <p>This is the part PacketEvents does not solve. Its wrappers unify the
 * <em>packets</em>, but the index and type of each metadata field are passed
 * raw, and Mojang shifted those indices repeatedly by inserting fields into the
 * base classes:
 *
 * <table border="1">
 *   <caption>Armour stand flag index by version</caption>
 *   <tr><th>Version</th><th>What changed</th><th>Index</th></tr>
 *   <tr><td>1.8</td><td>Entity 0-3, Living 6-9</td><td>10</td></tr>
 *   <tr><td>1.9-1.13</td><td>Silent and NoGravity added to Entity</td><td>11</td></tr>
 *   <tr><td>1.14</td><td>Pose added to Entity, BedLocation to Living</td><td>13</td></tr>
 *   <tr><td>1.15-1.16</td><td>StingerCount added to Living</td><td>14</td></tr>
 *   <tr><td>1.17+</td><td>FrozenTicks added to Entity</td><td>15</td></tr>
 * </table>
 *
 * <p>If the indices shift again, this is the only file that needs touching.
 */
public final class MetaIndex {

    /** Base entity bitmask. Never moved across any version. */
    public static final int ENTITY_FLAGS = 0;

    /** Entity flag bit marking the entity invisible. */
    public static final byte FLAG_INVISIBLE = 0x20;

    /** Entity flag bit marking the entity as glowing. */
    public static final byte FLAG_GLOWING = 0x40;

    /**
     * Custom name index. The index is stable, but the <em>type</em> changed in
     * 1.13 from {@code String} to {@code Optional<Component>}; see
     * {@link #nameIsComponent()}.
     */
    public static final int CUSTOM_NAME = 2;

    /** Custom name visibility index. */
    public static final int CUSTOM_NAME_VISIBLE = 3;

    /** Armour stand flag bit: render at half size. */
    public static final byte STAND_SMALL = 0x01;

    /** Armour stand flag bit: show arms. */
    public static final byte STAND_ARMS = 0x04;

    /** Armour stand flag bit: hide the base plate. */
    public static final byte STAND_NO_BASE_PLATE = 0x08;

    /** Armour stand flag bit: marker, giving it no hitbox. */
    public static final byte STAND_MARKER = 0x10;

    /** Mob flag bit disabling AI. */
    public static final byte MOB_NO_AI = 0x01;

    private MetaIndex() {
    }

    /**
     * @return the index of the armour stand bitmask on this version
     */
    public static int armorStandFlags() {
        if (ServerVersion.isLegacy()) return 10;
        if (!ServerVersion.atLeast(14)) return 11;
        if (!ServerVersion.atLeast(15)) return 13;
        if (!ServerVersion.atLeast(17)) return 14;
        return 15;
    }

    /**
     * @return the index of the mob bitmask, which shifts alongside the armour
     * stand one because both sit at the end of the same base class chain
     */
    public static int mobFlags() {
        return armorStandFlags();
    }

    /**
     * @return the index of the ageable "is baby" field
     */
    public static int ageableBaby() {
        // On 1.8 this was an age byte rather than a boolean, at a fixed index.
        return ServerVersion.isLegacy() ? 12 : mobFlags() + 1;
    }

    /**
     * @return {@code true} if custom names travel as a chat component rather
     * than a plain string, which is the case from 1.13 onwards
     */
    public static boolean nameIsComponent() {
        return ServerVersion.isFlattened();
    }

    /**
     * @return {@code true} if name visibility is a boolean rather than a byte,
     * which is the case from 1.9 onwards
     */
    public static boolean nameVisibleIsBoolean() {
        return !ServerVersion.isLegacy();
    }
}
