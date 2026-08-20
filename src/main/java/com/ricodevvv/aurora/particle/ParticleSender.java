package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.util.Reflect;
import com.ricodevvv.aurora.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Puts particles in front of players, by whichever route this server offers.
 *
 * <p>There are exactly two, and the split is 1.9:
 *
 * <ul>
 *   <li><b>1.9 and later</b> have {@code Player#spawnParticle}, which sends to
 *       that player alone and is all Aurora needs. It is called reflectively
 *       because its first parameter is {@code org.bukkit.Particle}, a class
 *       that does not exist on the 1.8 API Aurora compiles against.</li>
 *   <li><b>1.8</b> has no particle API at all, so the packet is built and sent
 *       by hand. Its long-distance flag is always set: the 1.8 client discards
 *       particles more than sixteen blocks away without it, and Aurora resolves
 *       its own audience out to thirty-two.</li>
 * </ul>
 *
 * <p>Aurora used to delegate this to XSeries. That had to go: from version 13
 * its particle layer is written against {@code org.bukkit.Particle} directly,
 * so merely loading it on 1.8 throws {@code NoClassDefFoundError} — which makes
 * it unusable for a library whose whole claim is one jar from 1.8 to 1.21.
 *
 * <p>Both backends fail soft. A particle that cannot be sent is skipped; it is
 * never allowed to throw out of a tick loop and take an effect down with it.
 */
abstract class ParticleSender {

    private static ParticleSender instance;

    /**
     * @return the backend for the running server, created on first use
     */
    static ParticleSender get() {
        if (instance == null) {
            instance = ServerVersion.isLegacy() ? new Legacy() : new Modern();
            Bukkit.getLogger().info("[Aurora] Particle backend: " + instance.name()
                    + " (" + ServerVersion.asString() + ").");
        }
        return instance;
    }

    /**
     * Sends one particle to every player in the audience.
     *
     * <p>The colour rewrite happens here rather than in the backends, because
     * it is a property of the protocol and not of how the packet is delivered:
     * below 1.13 a coloured particle is sent as a count of zero, the colour in
     * the offset fields and a speed of one, and that is true on both routes.
     *
     * @param viewers who should see it
     * @param type    what to spawn
     * @param x       world X
     * @param y       world Y
     * @param z       world Z
     * @param count   how many particles; {@code 0} makes the offset a velocity
     * @param offsetX spread or velocity along X
     * @param offsetY spread or velocity along Y
     * @param offsetZ spread or velocity along Z
     * @param extra   the particle's extra value, usually speed
     * @param data    colour, block or item payload; may be {@code null}
     */
    final void spawn(List<Player> viewers, ParticleType type,
                     double x, double y, double z, int count,
                     double offsetX, double offsetY, double offsetZ, double extra,
                     ParticleData data) {
        ParticleType effective = type.effective();
        Object handle = effective.handle();
        if (handle == null || viewers.isEmpty()) return;

        ParticleType.Payload payload = effective.payload();
        if (data != null && data.colorInOffsets(payload)) {
            float[] encoded = data.colorOffsets(payload);
            count = 0;
            offsetX = encoded[0];
            offsetY = encoded[1];
            offsetZ = encoded[2];
            extra = 1;
        }

        for (int i = 0; i < viewers.size(); i++) {
            try {
                send(viewers.get(i), handle, payload, x, y, z, count,
                        offsetX, offsetY, offsetZ, extra, data);
            } catch (Throwable ignored) {
                // One player's packet failing is not a reason to drop the rest.
            }
        }
    }

    /**
     * Delivers a single particle to a single player.
     *
     * @param player  recipient
     * @param handle  the resolved server-side particle constant
     * @param payload what extra data the particle expects
     * @param x       world X
     * @param y       world Y
     * @param z       world Z
     * @param count   particle count
     * @param offsetX spread or velocity along X
     * @param offsetY spread or velocity along Y
     * @param offsetZ spread or velocity along Z
     * @param extra   the particle's extra value
     * @param data    payload, may be {@code null}
     * @throws Exception if the underlying call fails
     */
    abstract void send(Player player, Object handle, ParticleType.Payload payload,
                       double x, double y, double z, int count,
                       double offsetX, double offsetY, double offsetZ, double extra,
                       ParticleData data) throws Exception;

    /**
     * @return a short name for the log line
     */
    abstract String name();

    // -------------------------------------------------------------- 1.9-1.21

    /**
     * Calls {@code Player#spawnParticle} reflectively.
     */
    private static final class Modern extends ParticleSender {

        /** The overload that takes a data object; present since 1.9. */
        private final Method withData;

        /** The overload without one, as a fallback. */
        private final Method plain;

        private Modern() {
            Class<?> particle = Reflect.lookup("org.bukkit.Particle");
            withData = Reflect.method(Player.class, "spawnParticle", particle,
                    double.class, double.class, double.class, int.class,
                    double.class, double.class, double.class, double.class, Object.class);
            plain = Reflect.method(Player.class, "spawnParticle", particle,
                    double.class, double.class, double.class, int.class,
                    double.class, double.class, double.class, double.class);
        }

        @Override
        void send(Player player, Object handle, ParticleType.Payload payload,
                  double x, double y, double z, int count,
                  double offsetX, double offsetY, double offsetZ, double extra,
                  ParticleData data) throws Exception {
            Object payloadObject = data == null ? null : data.modern(payload);

            if (withData != null) {
                withData.invoke(player, handle, x, y, z, count,
                        offsetX, offsetY, offsetZ, extra, payloadObject);
                return;
            }
            if (plain != null) {
                plain.invoke(player, handle, x, y, z, count, offsetX, offsetY, offsetZ, extra);
            }
        }

        @Override
        String name() {
            return "Bukkit API";
        }
    }

    // ------------------------------------------------------------------- 1.8

    /**
     * Builds {@code PacketPlayOutWorldParticle} by hand.
     */
    private static final class Legacy extends ParticleSender {

        private final Constructor<?> packet;
        private final Method getHandle;
        private final Field connectionField;
        private final Method sendPacket;

        private Legacy() {
            Class<?> packetClass = Reflect.nms("PacketPlayOutWorldParticle");
            Class<?> enumParticle = Reflect.nms("EnumParticle");
            Class<?> packetInterface = Reflect.nms("Packet");
            Class<?> connectionClass = Reflect.nms("PlayerConnection");
            Class<?> entityPlayer = Reflect.nms("EntityPlayer");

            packet = Reflect.constructor(packetClass, enumParticle, boolean.class,
                    float.class, float.class, float.class,
                    float.class, float.class, float.class,
                    float.class, int.class, int[].class);
            getHandle = Reflect.method(Reflect.obc("entity.CraftPlayer"), "getHandle");
            connectionField = entityPlayer == null ? null
                    : Reflect.fieldByType(entityPlayer, connectionClass);
            sendPacket = Reflect.method(connectionClass, "sendPacket", packetInterface);
        }

        @Override
        void send(Player player, Object handle, ParticleType.Payload payload,
                  double x, double y, double z, int count,
                  double offsetX, double offsetY, double offsetZ, double extra,
                  ParticleData data) throws Exception {
            if (packet == null || getHandle == null || connectionField == null || sendPacket == null) return;

            int[] extraData = data == null ? new int[0] : data.legacy(payload);
            Object built = packet.newInstance(handle, true,
                    (float) x, (float) y, (float) z,
                    (float) offsetX, (float) offsetY, (float) offsetZ,
                    (float) extra, count, extraData);

            Object connection = connectionField.get(getHandle.invoke(player));
            if (connection != null) sendPacket.invoke(connection, built);
        }

        @Override
        String name() {
            return "1.8 packets";
        }
    }
}
