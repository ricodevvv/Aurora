package com.ricodevvv.aurora.hologram;

import com.ricodevvv.aurora.animation.Animation;
import com.ricodevvv.aurora.util.Easing;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.List;

/**
 * Ready-made animations for holograms: spinning heads, bobbing, rainbow text,
 * typewriters, countdowns and health bars.
 *
 * <p>Every method returns a stopped animation; call {@code start()} on it.
 */
public final class HologramAnimations {

    private static final ChatColor[] RAINBOW = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN,
            ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE
    };

    private HologramAnimations() {
    }

    /**
     * Spins a head on its own axis.
     *
     * <p>Nothing is teleported: only the head pose changes, which is one
     * metadata packet per tick and no entity movement at all.
     *
     * @param line           head line to spin
     * @param degreesPerTick rotation speed in degrees per tick
     * @return the animation, not yet started
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

    /**
     * Spins a head while tilting it from side to side.
     *
     * @param line           head line to animate
     * @param degreesPerTick rotation speed in degrees per tick
     * @param tiltDegrees    maximum tilt in degrees
     * @return the animation, not yet started
     */
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
     * Bobs the whole hologram up and down.
     *
     * <p>Unlike {@link #rotate}, this does teleport armour stands every tick.
     * With many holograms in view that is real packet traffic; raise the
     * animation's interval if it shows up in your profiling.
     *
     * @param hologram  hologram to bob
     * @param amplitude travel distance in blocks
     * @param speed     radians advanced per tick
     * @return the animation, not yet started
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

    /**
     * Drops the hologram into place with a bounce, for spawn-in moments.
     *
     * @param hologram   hologram to animate
     * @param fromHeight starting height above its final position
     * @param ticks      duration in ticks
     * @return the animation, not yet started
     */
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

    /**
     * Cycles a colour wave across the characters of a line.
     *
     * @param plainText     text without colour codes
     * @param line          line to write into
     * @param ticksPerStep  ticks between colour shifts
     * @return the animation, not yet started
     */
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

    /**
     * Reveals text one character at a time.
     *
     * @param line         line to write into
     * @param fullText     the complete text
     * @param ticksPerChar ticks between characters
     * @param loop         whether to restart after a short pause
     * @return the animation, not yet started
     */
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

    /**
     * Highlights one character at a time, producing a travelling wave.
     *
     * @param line         line to write into
     * @param plainText    text without colour codes
     * @param base         colour of unhighlighted characters
     * @param highlight    colour of the highlighted character
     * @param ticksPerStep ticks between steps
     * @return the animation, not yet started
     */
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

    /**
     * Cycles through several texts on one line.
     *
     * @param line          line to write into
     * @param texts         texts to cycle
     * @param ticksPerText  ticks each text stays visible
     * @return the animation, not yet started
     */
    public static Animation cycle(TextLine line, List<String> texts, int ticksPerText) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                int index = (int) ((tick / Math.max(1, ticksPerText)) % texts.size());
                line.setText(texts.get(index));
            }
        };
    }

    /**
     * Orbits several heads around the hologram.
     *
     * @param hologram       hologram at the centre
     * @param heads          heads to orbit
     * @param radius         orbit radius in blocks
     * @param degreesPerTick angular speed in degrees per tick
     * @param yOffset        vertical offset of the orbit
     * @return the animation, not yet started
     */
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

    // ------------------------------------------------------------- additions

    /**
     * Makes the hologram follow an entity, for mob nameplates and health bars.
     *
     * @param hologram hologram to move
     * @param entity   entity to follow
     * @param yOffset  height above the entity
     * @return the animation, not yet started
     */
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

    /**
     * Renders a text health bar that recolours as it drains.
     *
     * @param line     line to write into
     * @param percent  supplies the current fill, in {@code 0..1}
     * @param segments how many segments the bar has
     * @return the animation, not yet started
     */
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

    /**
     * Counts down in seconds, recolouring as it nears zero.
     *
     * @param line    line to write into
     * @param seconds starting value
     * @param prefix  text placed before the number
     * @param onZero  callback fired when it reaches zero; may be {@code null}
     * @return the animation, not yet started
     */
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

    /**
     * Scrolls text through a fixed-width window.
     *
     * @param line          line to write into
     * @param text          text to scroll
     * @param windowSize    visible width in characters
     * @param ticksPerStep  ticks between shifts
     * @return the animation, not yet started
     */
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

    /**
     * Alternates between two texts, for urgent notices.
     *
     * @param line           line to write into
     * @param on             text shown in the first state
     * @param off            text shown in the second state
     * @param ticksPerState  ticks each state lasts
     * @return the animation, not yet started
     */
    public static Animation blink(TextLine line, String on, String off, int ticksPerState) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                boolean visible = ((tick / Math.max(1, ticksPerState)) % 2) == 0;
                line.setText(visible ? on : off);
            }
        };
    }

    /**
     * Turns a head to face whichever player is nearest.
     *
     * @param line  head line to turn
     * @param range search radius in blocks
     * @return the animation, not yet started
     */
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

    /**
     * Reveals the hologram's lines one after another.
     *
     * @param hologram      hologram to reveal
     * @param texts         final text of each line, in order
     * @param ticksPerLine  ticks between reveals
     * @return the animation, not yet started
     */
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
