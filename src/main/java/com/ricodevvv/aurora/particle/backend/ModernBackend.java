package com.ricodevvv.aurora.particle.backend;

import com.ricodevvv.aurora.particle.ParticleBackend;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.util.Reflect;
import com.ricodevvv.aurora.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Backend de 1.9 hasta 1.21+.
 *
 * Va todo por reflection a proposito: asi el modulo compila contra la API de
 * 1.8.8 (que es contra la que compilas Frost) y aun asi corre en 1.21.
 * Los Method/Constructor se resuelven una vez y quedan cacheados.
 */
public class ModernBackend implements ParticleBackend {

    private final Map<ParticleType, Object> resolved = new EnumMap<>(ParticleType.class);

    private final Class<?> particleClass;
    private final Method spawnMethod;

    private Constructor<?> dustOptionsCtor;   // 1.13+
    private Constructor<?> materialDataCtor;  // 1.9 - 1.12
    private Method createBlockData;           // 1.13+

    public ModernBackend() {
        particleClass = Reflect.lookup("org.bukkit.Particle");
        spawnMethod = Reflect.method(Player.class, "spawnParticle",
                particleClass, double.class, double.class, double.class,
                int.class, double.class, double.class, double.class,
                double.class, Object.class);

        if (ServerVersion.isFlattened()) {
            Class<?> dust = Reflect.lookup("org.bukkit.Particle$DustOptions");
            dustOptionsCtor = Reflect.constructor(dust, Color.class, float.class);
            createBlockData = Reflect.method(Material.class, "createBlockData");
        } else {
            Class<?> matData = Reflect.lookup("org.bukkit.material.MaterialData");
            materialDataCtor = Reflect.constructor(matData, Material.class, byte.class);
        }

        if (spawnMethod == null) {
            Bukkit.getLogger().warning("[Aurora] No pude resolver Player#spawnParticle; las particulas quedaran desactivadas.");
        }
    }

    @Override
    public boolean supports(ParticleType type) {
        return particle(type) != null;
    }

    @Override
    public void spawn(Collection<? extends Player> viewers, ParticleBuilder b,
                      double x, double y, double z) {
        if (spawnMethod == null) return;

        Object particle = particle(b.type());
        if (particle == null) return;

        int count = b.count();
        double ox = b.offsetX(), oy = b.offsetY(), oz = b.offsetZ(), extra = b.speed();
        Object data = null;

        if (b.type().colorable()) {
            if (b.type() == ParticleType.DUST && dustOptionsCtor != null) {
                data = dustOptions(b.color(), b.size());
            } else {
                // 1.9 - 1.12 (y SPELL_MOB en cualquier version): el color viaja
                // en los offsets y hay que mandar count = 0.
                Color c = b.color();
                count = 0;
                extra = 1;
                ox = Math.max(c.getRed(), 1) / 255D;
                oy = c.getGreen() / 255D;
                oz = c.getBlue() / 255D;
            }
        } else if (b.type().needsMaterial()) {
            data = materialData(b);
        }

        for (Player viewer : viewers) {
            try {
                spawnMethod.invoke(viewer, particle, x, y, z, count, ox, oy, oz, extra, data);
            } catch (Throwable ignored) {
            }
        }
    }

    private Object dustOptions(Color color, float size) {
        try {
            return dustOptionsCtor.newInstance(color, size);
        } catch (Throwable t) {
            return null;
        }
    }

    private Object materialData(ParticleBuilder b) {
        try {
            if (b.type().kind() == ParticleType.Kind.ITEM) {
                return ServerVersion.isFlattened()
                        ? new ItemStack(b.material())
                        : new ItemStack(b.material(), 1, (short) b.materialData());
            }
            if (ServerVersion.isFlattened()) {
                return createBlockData != null ? createBlockData.invoke(b.material()) : null;
            }
            return materialDataCtor != null
                    ? materialDataCtor.newInstance(b.material(), b.materialData())
                    : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Resuelve el primer alias que exista en el enum Particle de esta version. */
    private Object particle(ParticleType type) {
        if (resolved.containsKey(type)) return resolved.get(type);

        Object value = null;
        for (String name : type.modernNames()) {
            value = Reflect.enumValue(particleClass, name);
            if (value != null) break;
        }
        if (value == null) {
            // fallback razonable para no dejar el efecto mudo
            value = Reflect.enumValue(particleClass, "FLAME");
            Bukkit.getLogger().info("[Aurora] " + type + " no existe en " + ServerVersion.minor()
                    + ", usando FLAME como fallback.");
        }
        resolved.put(type, value);
        return value;
    }
}
