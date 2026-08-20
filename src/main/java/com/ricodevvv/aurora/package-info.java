/**
 * Aurora: particles, animations, holograms and cosmetics for Minecraft 1.8
 * through 1.21 and later.
 *
 * <p>Start with {@link com.ricodevvv.aurora.Aurora}, which must be initialised
 * in {@code onEnable} and shut down in {@code onDisable}. From there:
 *
 * <ul>
 *   <li>{@link com.ricodevvv.aurora.particle} spawns particles, through the
 *       Bukkit API on 1.9 and later and through packets on 1.8.</li>
 *   <li>{@link com.ricodevvv.aurora.shape} builds the geometry those particles
 *       are drawn along.</li>
 *   <li>{@link com.ricodevvv.aurora.animation} drives everything over time from
 *       a single shared server task.</li>
 *   <li>{@link com.ricodevvv.aurora.hologram} renders floating text and heads.</li>
 *   <li>{@link com.ricodevvv.aurora.cosmetic} ties it together into equippable
 *       balloons, pets and effects.</li>
 * </ul>
 */
package com.ricodevvv.aurora;
