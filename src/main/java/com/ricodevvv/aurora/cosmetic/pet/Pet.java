package com.ricodevvv.aurora.cosmetic.pet;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.util.Entities;
import com.ricodevvv.aurora.util.Sounds;
import org.bukkit.Location;
import com.ricodevvv.aurora.model.Model;
import com.ricodevvv.aurora.model.Models;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * A pet that follows its owner.
 *
 * <p>Movement is interpolated by Aurora rather than delegated to vanilla
 * pathfinding, for three reasons: the pathfinder changed substantially between
 * 1.8 and 1.21 so doing it by hand gives identical behaviour everywhere; it
 * avoids fighting the server for control of the entity and spends no AI ticks;
 * and it makes hovering and "teleport if left behind" trivial rather than
 * hacks.
 *
 * <p>The trade-off is that pets clip through walls on short hops. For a lobby
 * cosmetic that reads better than a pet stuck on a corner.
 */
public class Pet extends Cosmetic {

    private static final double TELEPORT_DISTANCE_SQ = 20 * 20;

    private final PetType petType;

    private Entity entity;      // modo MOB
    private Model model;        // modo HEAD
    private final Location location = new Location(null, 0, 0, 0);
    private final Location target = new Location(null, 0, 0, 0);

    public Pet(Player player, PetType type) {
        super(player, type);
        this.petType = type;
    }

    @Override
    protected void onEquip() {
        spawn(player.getLocation());
        Sounds.POP.play(player, 0.5f, 1.4f);
    }

    private void spawn(Location at) {
        if (petType.renderMode() == PetType.RenderMode.HEAD) {
            // HEAD mode has no real entity, so position is tracked in
            // `location`. With PacketEvents this is zero world entities.
            copy(location, at);
            model = Models.armorStand(true);
            model.spawn(at);
            model.helmet(petType.headItem());
            model.name(nameTag());
            return;
        }
        {
            entity = at.getWorld().spawnEntity(at, petType.entityType());
            if (petType.isBaby()) Entities.baby(entity);
            Entities.scale(entity, petType.scale());
        }

        Entities.tag(entity);
        Entities.disableAI(entity);
        Entities.silence(entity);
        Entities.invulnerable(entity);
        Entities.collidable(entity, false);
        Entities.persist(entity);
        Entities.nameTag(entity, nameTag());

        // If another plugin cancelled the spawn, do not leave the cosmetic hanging.
        if (!entity.isValid()) stop();
    }

    private String nameTag() {
        String raw = petType.nameTag();
        if (raw == null || raw.isEmpty()) return "";
        return raw.replace("<player>", player.getName())
                .replace("<pet>", petType.displayName());
    }

    @Override
    protected void onTick(long tick) {
        if (petType.renderMode() == PetType.RenderMode.HEAD) {
            tickHead(tick);
            return;
        }

        if (entity == null || !entity.isValid()) {
            stop();
            return;
        }

        Location petLocation = entity.getLocation();
        Location playerLocation = player.getLocation();

        // Different world or too far behind: respawn rather than crawl back.
        if (petLocation.getWorld() != playerLocation.getWorld()
                || petLocation.distanceSquared(playerLocation) > TELEPORT_DISTANCE_SQ) {
            entity.remove();
            spawn(playerLocation);
            return;
        }

        Vector delta = playerLocation.toVector().subtract(petLocation.toVector());
        double distance = delta.length();
        double hover = petType.hover() == 0 ? 0
                : Math.sin(tick * 0.15) * petType.hover() + petType.hover();

        if (distance > petType.followDistance()) {
            // Close a fraction of the remaining gap, so the pet speeds up when
            // the owner runs and eases to a stop on arrival.
            double step = Math.min(petType.speed(), distance - petType.followDistance());
            Vector move = delta.clone().normalize().multiply(step);
            target.setWorld(playerLocation.getWorld());
            target.setX(petLocation.getX() + move.getX());
            target.setY(petLocation.getY() + move.getY() + hover * 0.1);
            target.setZ(petLocation.getZ() + move.getZ());
            target.setYaw(Entities.yawTowards(target, playerLocation));
            target.setPitch(0);
            entity.teleport(target);
        } else if (petType.hover() > 0 || tick % 4 == 0) {
            // At rest: hover in place and turn to face the owner.
            target.setWorld(playerLocation.getWorld());
            target.setX(petLocation.getX());
            target.setY(petLocation.getY() + hover * 0.05);
            target.setZ(petLocation.getZ());
            target.setYaw(Entities.yawTowards(petLocation, playerLocation));
            target.setPitch(0);
            entity.teleport(target);
        }

        if (petType.trailParticle() != null && tick % petType.trailInterval() == 0) {
            petType.trailParticle().spawn(entity.getLocation().add(0, 0.3, 0));
        }
    }

    /**
     * Runs the same follow logic as MOB mode, but against a position Aurora
     * tracks itself rather than one read back from an entity.
     *
     * @param tick ticks since the pet was equipped
     */
    private void tickHead(long tick) {
        if (model == null || !model.alive()) {
            stop();
            return;
        }
        Location playerLocation = player.getLocation();

        if (location.getWorld() != playerLocation.getWorld()
                || location.distanceSquared(playerLocation) > TELEPORT_DISTANCE_SQ) {
            copy(location, playerLocation);
            model.teleport(location);
            return;
        }

        Vector delta = playerLocation.toVector().subtract(location.toVector());
        double distance = delta.length();
        double hover = petType.hover() == 0 ? 0 : Math.sin(tick * 0.15) * petType.hover();

        if (distance > petType.followDistance()) {
            double step = Math.min(petType.speed(), distance - petType.followDistance());
            Vector move = delta.clone().normalize().multiply(step);
            location.setX(location.getX() + move.getX());
            location.setY(location.getY() + move.getY());
            location.setZ(location.getZ() + move.getZ());
        }
        location.setYaw(Entities.yawTowards(location, playerLocation));
        location.setPitch(0);

        Location render = location.clone().add(0, hover * 0.1, 0);
        model.teleport(render);
        model.headYaw(location.getYaw());

        if (petType.trailParticle() != null && tick % petType.trailInterval() == 0) {
            petType.trailParticle().spawn(render.clone().add(0, 0.3, 0));
        }
    }

    private static void copy(Location target, Location source) {
        target.setWorld(source.getWorld());
        target.setX(source.getX());
        target.setY(source.getY());
        target.setZ(source.getZ());
        target.setYaw(source.getYaw());
        target.setPitch(0);
    }

    @Override
    protected void onUnequip() {
        if (entity != null && entity.isValid()) entity.remove();
        if (model != null) model.destroy();
        entity = null;
        model = null;
    }

    public Entity entity() {
        return entity;
    }

    public Model model() {
        return model;
    }
}
