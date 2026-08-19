package com.ricodevvv.aurora.particle;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * The two places where XSeries' API cannot be called directly from code
 * compiled against the 1.8 API.
 *
 * <p>Aurora is deliberately compiled against 1.8.8 so one jar runs everywhere.
 * The cost is that {@code org.bukkit.Particle} and
 * {@code org.bukkit.block.data.BlockData} do not exist at compile time, and
 * javac must load both before it can choose between overloads such as
 * {@code ParticleDisplay.of(XParticle)} and {@code of(Particle)} — even when
 * the overload being called mentions neither. Reflection sidesteps that: the
 * method handles are resolved once at class-load time, on whatever server is
 * actually running.
 *
 * <p>Everything here fails soft. A server that does not expose one of these
 * methods gets the conservative answer rather than an exception in a tick loop.
 */
final class ParticleCompat {

    /** {@code XParticle#isSupported()}, absent on very old XSeries builds. */
    private static final Method IS_SUPPORTED;

    /** {@code Material#createBlockData()}, 1.13 and later. */
    private static final Method CREATE_BLOCK_DATA;

    /** {@code ParticleDisplay#withBlock(BlockData)}, 1.13 and later. */
    private static final Method WITH_BLOCK_DATA;

    /** {@code ParticleDisplay#withBlock(MaterialData)}, the legacy path. */
    private static final Method WITH_MATERIAL_DATA;

    /** {@code ParticleDisplay.of(XParticle)}. */
    private static final Method DISPLAY_OF;

    /** {@code ParticleDisplay#withParticle(XParticle)}. */
    private static final Method WITH_PARTICLE;

    /** Support answers are stable for a given server, so they are worth caching. */
    private static final Map<XParticle, Boolean> SUPPORT = new HashMap<>();

    static {
        IS_SUPPORTED = method(XParticle.class, "isSupported");
        DISPLAY_OF = method(ParticleDisplay.class, "of", XParticle.class);
        WITH_PARTICLE = method(ParticleDisplay.class, "withParticle", XParticle.class);
        CREATE_BLOCK_DATA = method(Material.class, "createBlockData");
        WITH_MATERIAL_DATA = method(ParticleDisplay.class, "withBlock", MaterialData.class);

        Method withBlockData = null;
        try {
            withBlockData = ParticleDisplay.class.getMethod("withBlock",
                    Class.forName("org.bukkit.block.data.BlockData"));
        } catch (Throwable ignored) {
            // Pre-flattening server; the MaterialData path covers it.
        }
        WITH_BLOCK_DATA = withBlockData;
    }

    private ParticleCompat() {
    }

    /**
     * Creates a display for a particle.
     *
     * @param particle particle to display
     * @return a configured display, or a bare one if the factory is unreachable
     */
    static ParticleDisplay display(XParticle particle) {
        if (DISPLAY_OF != null) {
            try {
                return (ParticleDisplay) DISPLAY_OF.invoke(null, particle);
            } catch (Throwable ignored) {
                // Fall through to a bare display rather than failing a spawn.
            }
        }
        ParticleDisplay display = new ParticleDisplay();
        particle(display, particle);
        return display;
    }

    /**
     * Changes which particle a display spawns.
     *
     * @param display  display to reconfigure
     * @param particle new particle
     */
    static void particle(ParticleDisplay display, XParticle particle) {
        if (WITH_PARTICLE == null) return;
        try {
            WITH_PARTICLE.invoke(display, particle);
        } catch (Throwable ignored) {
            // Leave the display on its current particle.
        }
    }

    /**
     * Whether the running server knows this particle.
     *
     * @param particle particle to test
     * @return {@code true} if it can be spawned; {@code true} as well when the
     * check itself is unavailable, so callers are not silently downgraded
     */
    static boolean supported(XParticle particle) {
        if (particle == null) return false;
        if (IS_SUPPORTED == null) return true;

        Boolean cached = SUPPORT.get(particle);
        if (cached != null) return cached;

        boolean supported = true;
        try {
            supported = Boolean.TRUE.equals(IS_SUPPORTED.invoke(particle));
        } catch (Throwable ignored) {
            // Treat an unanswerable question as a yes and let the spawn decide.
        }
        SUPPORT.put(particle, supported);
        return supported;
    }

    /**
     * Textures a display with a block, on either side of the flattening.
     *
     * @param display display to configure
     * @param material block material
     */
    @SuppressWarnings("deprecation")
    static void block(ParticleDisplay display, Material material) {
        if (CREATE_BLOCK_DATA != null && WITH_BLOCK_DATA != null) {
            try {
                WITH_BLOCK_DATA.invoke(display, CREATE_BLOCK_DATA.invoke(material));
                return;
            } catch (Throwable ignored) {
                // Not a block on this version; fall through.
            }
        }
        if (WITH_MATERIAL_DATA == null) return;
        try {
            WITH_MATERIAL_DATA.invoke(display, new MaterialData(material));
        } catch (Throwable ignored) {
            // Nothing left to try; the particle keeps its default texture.
        }
    }

    /**
     * Resolves a method, or {@code null} if this server does not have it.
     *
     * @param owner      declaring class
     * @param name       method name
     * @param parameters parameter types
     * @return the method, or {@code null}
     */
    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
