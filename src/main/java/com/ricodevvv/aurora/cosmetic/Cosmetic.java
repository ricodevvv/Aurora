package com.ricodevvv.aurora.cosmetic;

import com.ricodevvv.aurora.animation.Animation;
import org.bukkit.entity.Player;

/**
 * Instancia viva de un cosmetico equipado por un jugador.
 *
 * Extiende Animation a proposito: asi cada cosmetico corre dentro del task
 * global de Aurora en vez de crear el suyo. Con 200 jugadores equipados eso
 * es 1 task, no 200.
 */
public abstract class Cosmetic extends Animation {

    protected final Player player;
    protected final CosmeticType type;

    protected Cosmetic(Player player, CosmeticType type) {
        this.player = player;
        this.type = type;
    }

    /** Se llama al equipar: aqui creas entidades, mandas sonidos, etc. */
    protected abstract void onEquip();

    /** Se llama al desequipar: aqui limpias TODO lo que creaste. */
    protected abstract void onUnequip();

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

    protected abstract void onTick(long tick);

    public Player player() {
        return player;
    }

    public CosmeticType type() {
        return type;
    }

    public CosmeticCategory category() {
        return type.category();
    }
}
