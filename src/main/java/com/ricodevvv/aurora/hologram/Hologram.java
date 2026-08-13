package com.ricodevvv.aurora.hologram;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holograma multi-linea: mezcla texto, items y cabezas.
 *
 * <pre>
 * Hologram holo = new Hologram(loc)
 *         .head(Items.head("Notch"))
 *         .text("&amp;b&amp;lTIENDA")
 *         .text("&amp;7Click derecho")
 *         .spawn();
 * </pre>
 */
public class Hologram {

    public static final String METADATA_KEY = "aurora_hologram";

    private static final List<Hologram> REGISTRY = Collections.synchronizedList(new ArrayList<>());

    private final List<HologramLine> lines = new ArrayList<>();
    private Location location;
    private double lineGap = 0.03;
    private boolean spawned;

    public Hologram(Location location) {
        this.location = location.clone();
        REGISTRY.add(this);
    }

    // ---------------------------------------------------------- construction

    public Hologram text(String text) {
        return line(new TextLine(text));
    }

    public Hologram head(ItemStack item) {
        return line(new HeadLine(item));
    }

    public Hologram smallHead(ItemStack item) {
        return line((HologramLine) new HeadLine(item).small(true));
    }

    public Hologram line(HologramLine line) {
        lines.add(line);
        if (spawned) reposition();
        return this;
    }

    /**
     * Sets the vertical gap between lines.
     *
     * @param gap gap in blocks
     * @return this hologram
     */
    public Hologram gap(double gap) {
        this.lineGap = gap;
        if (spawned) reposition();
        return this;
    }

    // ------------------------------------------------------------- lifecycle

    public Hologram spawn() {
        if (spawned) return this;
        spawned = true;
        double y = totalHeight();
        for (HologramLine line : lines) {
            y -= line.height();
            line.spawn(location.clone().add(0, y, 0));
            y -= lineGap;
        }
        return this;
    }

    public void despawn() {
        for (HologramLine line : lines) line.remove();
        spawned = false;
    }

    /**
     * Removes the hologram and drops it from the global registry.
     */
    public void destroy() {
        despawn();
        lines.clear();
        REGISTRY.remove(this);
    }

    public Hologram teleport(Location to) {
        this.location = to.clone();
        if (spawned) reposition();
        return this;
    }

    private void reposition() {
        double y = totalHeight();
        for (HologramLine line : lines) {
            y -= line.height();
            Location target = location.clone().add(0, y, 0);
            if (line.stand() == null) line.spawn(target);
            else line.teleport(target);
            y -= lineGap;
        }
    }

    private double totalHeight() {
        double total = 0;
        for (HologramLine line : lines) total += line.height() + lineGap;
        return total;
    }

    // ---------------------------------------------------------------- access

    public void setLine(int index, String text) {
        if (index < 0 || index >= lines.size()) return;
        HologramLine line = lines.get(index);
        if (line instanceof TextLine) ((TextLine) line).setText(text);
    }

    public HologramLine get(int index) {
        return lines.get(index);
    }

    /**
     * Finds the first line of a given type, which is the convenient way to grab
     * a head line and animate it.
     *
     * @param type line class to look for
     * @param <T>  line type
     * @return the first matching line, or {@code null} if there is none
     */
    @SuppressWarnings("unchecked")
    public <T extends HologramLine> T first(Class<T> type) {
        for (HologramLine line : lines) {
            if (type.isInstance(line)) return (T) line;
        }
        return null;
    }

    public List<HologramLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public Location location() {
        return location.clone();
    }

    public boolean isSpawned() {
        return spawned;
    }

    /**
     * Despawns every hologram ever created. Called during shutdown.
     */
    public static void despawnAll() {
        synchronized (REGISTRY) {
            for (Hologram hologram : new ArrayList<>(REGISTRY)) hologram.despawn();
            REGISTRY.clear();
        }
    }

    public static List<Hologram> all() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY));
    }
}
