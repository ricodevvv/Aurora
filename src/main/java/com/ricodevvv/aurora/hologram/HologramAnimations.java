package com.ricodevvv.aurora.hologram;

import com.ricodevvv.aurora.animation.Animation;
import com.ricodevvv.aurora.util.Easing;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.List;

/** Animaciones para hologramas: cabezas girando, rebote, arcoiris, maquina de escribir. */
public final class HologramAnimations {

    private static final ChatColor[] RAINBOW = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN,
            ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE
    };

    private HologramAnimations() {
    }

    /**
     * Cabeza girando sobre su eje. Es el clasico de Hypixel.
     * No teletransporta nada: solo cambia el headPose, o sea un paquete de
     * metadata por tick y cero movimiento de entidad.
     */
    public static Animation rotate(HeadLine line, double degreesPerTick) {
        double step = Math.toRadians(degreesPerTick);
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (line.stand() == null || !line.stand().isValid()) {
                    stop();
                    return;
                }
                line.setYaw((step * tick) % (Math.PI * 2));
            }
        };
    }

    /** Giro con bamboleo: ademas de girar, se inclina de lado a lado. */
    public static Animation wobble(HeadLine line, double degreesPerTick, double tiltDegrees) {
        double step = Math.toRadians(degreesPerTick);
        double tilt = Math.toRadians(tiltDegrees);
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (line.stand() == null || !line.stand().isValid()) {
                    stop();
                    return;
                }
                line.setPose(Math.sin(tick * 0.08) * tilt, (step * tick) % (Math.PI * 2), 0);
            }
        };
    }

    /**
     * Flotacion arriba/abajo de todo el holograma.
     * OJO: esto si teletransporta ArmorStands cada tick. Con muchos hologramas
     * a la vista sube el trafico de paquetes; subele el interval si te pesa.
     */
    public static Animation bob(Hologram hologram, double amplitude, double speed) {
        Location base = hologram.location();
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!hologram.isSpawned()) {
                    stop();
                    return;
                }
                double y = Math.sin(tick * speed) * amplitude;
                hologram.teleport(base.clone().add(0, y, 0));
            }
        };
    }

    /** Rebote con easing, para cuando el holograma aparece. */
    public static Animation dropIn(Hologram hologram, double fromHeight, int ticks) {
        Location target = hologram.location();
        return new Animation() {
            @Override
            protected void update(long tick) {
                double y = Easing.EASE_OUT_BOUNCE.between(fromHeight, 0, progress());
                hologram.teleport(target.clone().add(0, y, 0));
            }
        }.duration(ticks);
    }

    /** Texto arcoiris: la onda de color recorre los caracteres. */
    public static Animation rainbow(TextLine line, String plainText, int ticksPerStep) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                long step = tick / Math.max(1, ticksPerStep);
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < plainText.length(); i++) {
                    builder.append(RAINBOW[(int) ((i + step) % RAINBOW.length)])
                            .append(plainText.charAt(i));
                }
                line.setText(builder.toString());
            }
        };
    }

    /** Maquina de escribir: revela el texto caracter por caracter y luego lo borra. */
    public static Animation typewriter(TextLine line, String fullText, int ticksPerChar, boolean loop) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                long step = tick / Math.max(1, ticksPerChar);
                int cycle = fullText.length() + 10; // pausa al final
                int index = (int) (loop ? step % cycle : Math.min(step, fullText.length()));
                line.setText(fullText.substring(0, Math.min(index, fullText.length())));
                if (!loop && index >= fullText.length()) stop();
            }
        };
    }

    /** Onda tipo "ola" sobre el texto: resalta un caracter a la vez. */
    public static Animation wave(TextLine line, String plainText, ChatColor base, ChatColor highlight, int ticksPerStep) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                int pos = (int) ((tick / Math.max(1, ticksPerStep)) % plainText.length());
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < plainText.length(); i++) {
                    builder.append(i == pos ? highlight : base).append(plainText.charAt(i));
                }
                line.setText(builder.toString());
            }
        };
    }

    /** Cicla varios textos en la misma linea. */
    public static Animation cycle(TextLine line, List<String> texts, int ticksPerText) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                int index = (int) ((tick / Math.max(1, ticksPerText)) % texts.size());
                line.setText(texts.get(index));
            }
        };
    }

    /** Varias cabezas orbitando alrededor del holograma. */
    public static Animation orbitHeads(Hologram hologram, List<HeadLine> heads,
                                       double radius, double degreesPerTick, double yOffset) {
        Location center = hologram.location();
        return new Animation() {
            @Override
            protected void update(long tick) {
                double base = Math.toRadians(degreesPerTick) * tick;
                for (int i = 0; i < heads.size(); i++) {
                    HeadLine head = heads.get(i);
                    if (head.stand() == null || !head.stand().isValid()) continue;
                    double angle = base + (Math.PI * 2 * i / heads.size());
                    head.teleport(center.clone().add(
                            Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius));
                    head.setYaw(-angle);
                }
            }
        };
    }

    // ------------------------------------------------------------ agregados

    /** El holograma sigue a una entidad (nametag de mob, barra de vida, etc). */
    public static Animation follow(Hologram hologram, org.bukkit.entity.Entity entity, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!entity.isValid() || !hologram.isSpawned()) {
                    stop();
                    return;
                }
                hologram.teleport(entity.getLocation().add(0, yOffset, 0));
            }
        };
    }

    /** Barra de vida en texto. Pasa el proveedor de porcentaje (0..1). */
    public static Animation healthBar(TextLine line, java.util.function.DoubleSupplier percent,
                                      int segments) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                double value = Math.max(0, Math.min(1, percent.getAsDouble()));
                int filled = (int) Math.round(value * segments);
                StringBuilder builder = new StringBuilder();
                String color = value > 0.5 ? "&a" : value > 0.25 ? "&e" : "&c";
                builder.append(color);
                for (int i = 0; i < segments; i++) {
                    if (i == filled) builder.append("&7");
                    builder.append('|');
                }
                line.setText(builder.toString());
            }
        }.interval(4);
    }

    /** Cuenta regresiva en segundos. Al llegar a cero corre el callback. */
    public static Animation countdown(TextLine line, int seconds, String prefix, Runnable onZero) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                int left = seconds - (int) (tick / 20);
                if (left <= 0) {
                    line.setText(prefix + "&c0");
                    if (onZero != null) onZero.run();
                    stop();
                    return;
                }
                String color = left <= 3 ? "&c" : left <= 10 ? "&e" : "&a";
                line.setText(prefix + color + left);
            }
        }.interval(20);
    }

    /** Texto que se desplaza dentro de una ventana fija, tipo marquesina. */
    public static Animation marquee(TextLine line, String text, int windowSize, int ticksPerStep) {
        String padded = text + "   ";
        return new Animation() {
            @Override
            protected void update(long tick) {
                int offset = (int) ((tick / Math.max(1, ticksPerStep)) % padded.length());
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < windowSize; i++) {
                    builder.append(padded.charAt((offset + i) % padded.length()));
                }
                line.setText(builder.toString());
            }
        };
    }

    /** Parpadeo entre dos textos. Para avisos urgentes. */
    public static Animation blink(TextLine line, String on, String off, int ticksPerState) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                boolean visible = ((tick / Math.max(1, ticksPerState)) % 2) == 0;
                line.setText(visible ? on : off);
            }
        };
    }

    /** La cabeza mira siempre al jugador mas cercano (billboard). */
    public static Animation lookAtNearest(HeadLine line, double range) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (line.stand() == null || !line.stand().isValid() || line.location() == null) {
                    stop();
                    return;
                }
                Location at = line.location();
                if (at.getWorld() == null) return;

                org.bukkit.entity.Player closest = null;
                double best = range * range;
                for (org.bukkit.entity.Player player : at.getWorld().getPlayers()) {
                    Location l = player.getLocation();
                    double dx = l.getX() - at.getX(), dz = l.getZ() - at.getZ();
                    double distance = dx * dx + dz * dz;
                    if (distance < best) {
                        best = distance;
                        closest = player;
                    }
                }
                if (closest == null) return;
                Location l = closest.getLocation();
                line.setYaw(-Math.atan2(l.getZ() - at.getZ(), l.getX() - at.getX()) - Math.PI / 2);
            }
        }.interval(2);
    }

    /** Aparicion escalonada: las lineas van saliendo una por una. */
    public static Animation revealLines(Hologram hologram, java.util.List<String> texts, int ticksPerLine) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                int shown = (int) (tick / Math.max(1, ticksPerLine));
                for (int i = 0; i < texts.size(); i++) {
                    if (hologram.get(i) instanceof TextLine) {
                        ((TextLine) hologram.get(i)).setText(i <= shown ? texts.get(i) : "");
                    }
                }
                if (shown >= texts.size()) stop();
            }
        };
    }
}
