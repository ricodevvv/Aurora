package com.ricodevvv.aurora.cosmetic;

import com.ricodevvv.aurora.animation.Animation;
import org.bukkit.entity.Player;

/**
 * A live cosmetic worn by one player.
 *
 * <p>Extending {@link Animation} is deliberate: every equipped cosmetic runs
 * inside Aurora's single shared task rather than creating its own. Two hundred
 * players wearing cosmetics costs one server task, not two hundred.
 *
 * <p>Subclasses must release everything they create in {@link #onUnequip()}.
 * That hook is guaranteed to run whether the cosmetic was unequipped, the
 * player disconnected, or the server shut down.
 */
public abstract class Cosmetic extends Animation {

    /** The player wearing this cosmetic. */
    protected final Player player;

    /** The definition this instance was created from. */
    protected final CosmeticType type;

    /**
     * @param player player wearing it
     * @param type   definition it was created from
     */
    protected Cosmetic(Player player, CosmeticType type) {
        this.player = player;
        this.type = type;
    }

    /**
     * Called once when equipped. Spawn entities and play feedback here.
     */
    protected abstract void onEquip();

    /**
     * Called once when unequipped. Remove everything created in
     * {@link #onEquip()} here.
     */
    protected abstract void onUnequip();

    /**
     * Called every tick while equipped and the player is valid.
     *
     * @param tick ticks since the cosmetic was equipped
     */
    protected abstract void onTick(long tick);

    @Override
    protected final void onStart() {
        onEquip();
    }

    @Override
    protected final void onStop() {
        onUnequip();
    }

    @Override
    protected void update(long tick) {
        if (!player.isValid()) {
            stop();
            return;
        }
        onTick(tick);
    }

    /**
     * @return the player wearing this cosmetic
     */
    public Player player() {
        return player;
    }

    /**
     * @return the definition this instance was created from
     */
    public CosmeticType type() {
        return type;
    }

    /**
     * @return the category of this cosmetic
     */
    public CosmeticCategory category() {
        return type.category();
    }
}
