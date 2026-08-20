package com.ricodevvv.aurora.example;

import com.ricodevvv.aurora.Aurora;
import com.ricodevvv.aurora.animation.Animation;
import com.ricodevvv.aurora.animation.Animations;
import com.ricodevvv.aurora.animation.ShapeAnimation;
import com.ricodevvv.aurora.animation.Timeline;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticListener;
import com.ricodevvv.aurora.cosmetic.CosmeticManager;
import com.ricodevvv.aurora.cosmetic.CosmeticRegistry;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import com.ricodevvv.aurora.cosmetic.Cosmetics;
import com.ricodevvv.aurora.hologram.FloatingText;
import com.ricodevvv.aurora.shape.Glyphs;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Sounds;
import com.ricodevvv.aurora.hologram.HeadLine;
import com.ricodevvv.aurora.hologram.Hologram;
import com.ricodevvv.aurora.hologram.HologramAnimations;
import com.ricodevvv.aurora.hologram.TextLine;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.Easing;
import com.ricodevvv.aurora.util.Items;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * Example plugin, not required to use the library.
 *
 * <p>It exists to show the API in use and to give something to test against on
 * a live server. Run {@code /aurora <effect>} in game; with no arguments the
 * command lists everything available.
 */
public class AuroraDemo extends JavaPlugin {

    @Override
    public void onEnable() {
        Aurora.init(this);
        Cosmetics.registerDefaults();
        // Without this, every disconnect leaves that player's cosmetics behind.
        getServer().getPluginManager().registerEvents(new CosmeticListener(), this);
    }

    @Override
    public void onDisable() {
        Aurora.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        Location location = player.getLocation();
        String type = args.length > 0 ? args[0].toLowerCase() : "aura";

        switch (type) {
            case "aura": {
                // A rainbow ring spinning at the player's feet for ten seconds.
                ShapeAnimation animation = new ShapeAnimation(
                        Animations.follow(player, 0.1),
                        Shapes.circle(1.2, 30),
                        Particles.dust(Color.AQUA).size(1.1f));
                animation.spinDegrees(8).rainbow(0.01f).duration(200).start();
                break;
            }
            case "escudo": {
                Animations.shield(player, Particles.dust(Color.fromRGB(80, 180, 255)), 1.6)
                        .duration(200)
                        .start();
                break;
            }
            case "onda": {
                // Expanding shockwave, in the style of a kill effect.
                Animations.ringWave(location, Particles.of(ParticleType.FLAME).speed(0.01), 0.5, 6, 25, 40)
                        .start();
                Animations.burst(location.clone().add(0, 1, 0),
                        Particles.of(ParticleType.LARGE_SMOKE).count(2).offset(0.05), 3, 15, 60)
                        .start();
                break;
            }
            case "vortice": {
                Animations.vortex(location, Particles.dust(Color.PURPLE), 2, 3, 120)
                        .duration(300)
                        .start();
                break;
            }
            case "haz": {
                Location target = player.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 40).getLocation();
                Animations.beam(location.clone().add(0, 1.4, 0), target,
                                Particles.of(ParticleType.END_ROD), 2.5, 0.2)
                        .start();
                break;
            }
            case "corazon": {
                new ShapeAnimation(Animations.follow(player, 0), Shapes.heart(1.0, 60),
                        Particles.dust(Color.fromRGB(255, 60, 120)))
                        .spinDegrees(5)
                        .yOffset(2.4)
                        .duration(200)
                        .scaleOverTime(0.2, 1.0, Easing.EASE_OUT_ELASTIC)
                        .start();
                break;
            }
            case "holo": {
                Hologram hologram = new Hologram(location.clone().add(0, 2, 0))
                        .head(Items.head(player.getName()))
                        .text("&b&lAURORA")
                        .text("&7Sistema de animaciones")
                        .text("")
                        .spawn();

                HeadLine head = hologram.first(HeadLine.class);
                HologramAnimations.rotate(head, 5).start();
                HologramAnimations.bob(hologram, 0.15, 0.1).interval(2).start();

                TextLine title = (TextLine) hologram.get(1);
                HologramAnimations.rainbow(title, "AURORA", 2).start();

                TextLine last = (TextLine) hologram.get(3);
                Animation typing = HologramAnimations.typewriter(last, "&aClick derecho para abrir...", 2, true);
                typing.start();

                // A ring of particles around the hologram.
                new ShapeAnimation(hologram.location().add(0, 0.2, 0),
                        Shapes.circle(0.8, 20),
                        Particles.dust(Color.AQUA).size(0.8f))
                        .spinDegrees(6)
                        .start();
                break;
            }
            case "menu": {
                // A floating menu with heads orbiting around it.
                Hologram hologram = new Hologram(location.clone().add(0, 2.5, 0)).text("&e&lSELECCIONA").spawn();
                HeadLine a = new HeadLine(Items.head("Notch"));
                HeadLine b = new HeadLine(Items.head("Herobrine"));
                HeadLine c = new HeadLine(Items.head("Dinnerbone"));
                for (HeadLine head : Arrays.asList(a, b, c)) {
                    head.small(true);
                    hologram.line(head);
                }
                HologramAnimations.orbitHeads(hologram, Arrays.asList(a, b, c), 1.2, 3, 0).start();
                break;
            }
            case "alas": {
                Animations.wings(player, Particles.dust(Colors.hex("#FFF3B0")).size(0.7f), 1.6)
                        .duration(400)
                        .start();
                break;
            }
            case "tornado": {
                Animations.tornado(location, Particles.of(ParticleType.LARGE_SMOKE).speed(0.01),
                                2.5, 6, 12)
                        .duration(300)
                        .start();
                Sounds.DRAGON_GROWL.playAt(location, 1f, 0.5f);
                break;
            }
            case "rayo": {
                Animations.lightningStrike(location, Particles.dust(Colors.hex("#B0E0FF")), 20).start();
                Sounds.EXPLODE.playAt(location, 1f, 1.2f);
                break;
            }
            case "texto": {
                Animations.text(location.clone().add(0, 3, 0), "AURORA",
                                Particles.dust(Color.AQUA).size(0.6f), 0.12)
                        .duration(300)
                        .start();
                break;
            }
            case "calavera": {
                Animations.sprite(location.clone().add(0, 2.5, 0), Glyphs.SKULL,
                                Particles.dust(Colors.hex("#FF3355")).size(0.7f), 0.15)
                        .duration(200)
                        .start();
                break;
            }
            case "dano": {
                FloatingText.damage(location.clone().add(0, 1.8, 0), 7.5);
                FloatingText.critical(location.clone().add(0, 2.0, 0), 12);
                break;
            }
            case "combo": {
                // Full sequence: sound, wave, pause, explosion, text.
                Timeline.create()
                        .run(() -> Sounds.ANVIL_LAND.playAt(location, 1f, 0.6f))
                        .play(Animations.ringWave(location, Particles.dust(Colors.hex("#FFAA00")), 0.5, 6, 20, 40))
                        .wait(15)
                        .run(() -> Sounds.EXPLODE.playAt(location, 1f, 1.4f))
                        .play(Animations.blockExplosion(location, org.bukkit.Material.STONE, 4, 20, 60))
                        .wait(20)
                        .play(Animations.text(location.clone().add(0, 3, 0), "BOOM",
                                Particles.dust(Colors.hex("#FF4400")).size(0.8f), 0.14).duration(60))
                        .start();
                break;
            }
            case "gradiente": {
                Animations.gradientTrail(player, Particles.of(ParticleType.DUST), Colors.FIRE, 40, 1.0)
                        .duration(400)
                        .start();
                break;
            }
            case "globo": {
                String id = args.length > 1 ? args[1] : "rojo";
                CosmeticType balloon = CosmeticRegistry.get(CosmeticCategory.BALLOON, id);
                if (balloon == null) {
                    player.sendMessage("Globos: " + ids(CosmeticCategory.BALLOON));
                    break;
                }
                boolean on = CosmeticManager.toggle(player, balloon);
                player.sendMessage(on ? "Globo equipado." : "Globo quitado.");
                break;
            }
            case "pet": {
                String id = args.length > 1 ? args[1] : "creeper";
                CosmeticType pet = CosmeticRegistry.get(CosmeticCategory.PET, id);
                if (pet == null) {
                    player.sendMessage("Mascotas: " + ids(CosmeticCategory.PET));
                    break;
                }
                boolean on = CosmeticManager.toggle(player, pet);
                player.sendMessage(on ? "Mascota equipada." : "Mascota quitada.");
                break;
            }
            case "efecto": {
                String id = args.length > 1 ? args[1] : "aura_fuego";
                CosmeticType effect = CosmeticRegistry.get(CosmeticCategory.PARTICLE_EFFECT, id);
                if (effect == null) {
                    player.sendMessage("Efectos: " + ids(CosmeticCategory.PARTICLE_EFFECT));
                    break;
                }
                boolean on = CosmeticManager.toggle(player, effect);
                player.sendMessage(on ? "Efecto equipado." : "Efecto quitado.");
                break;
            }
            case "quitar": {
                CosmeticManager.unequipAll(player);
                player.sendMessage("Cosmeticos quitados.");
                break;
            }
            default:
                player.sendMessage("Usa: aura, escudo, onda, vortice, rayo, corazon, holo, menu, "
                        + "alas, tornado, texto, calavera, dano, combo, gradiente, "
                        + "globo <id>, pet <id>, efecto <id>, quitar");
        }
        return true;
    }

    private static String ids(CosmeticCategory category) {
        StringBuilder builder = new StringBuilder();
        for (CosmeticType type : CosmeticRegistry.byCategory(category)) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(type.id());
        }
        return builder.toString();
    }
}
