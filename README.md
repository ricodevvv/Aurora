# Aurora

Particles, animations, holograms and cosmetics for Spigot/Paper **1.8 → 1.21+**, from a single jar.

Compiled against the 1.8.8 API; everything version-specific is resolved at runtime.

## Install

```xml
<repository><id>jitpack</id><url>https://jitpack.io</url></repository>

<dependency>
    <groupId>com.github.ricodevvv</groupId>
    <artifactId>Aurora</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Shade it. [XSeries](https://github.com/CryptoMorin/XSeries) is required and must be shaded too.
[PacketEvents](https://github.com/retrooper/packetevents) is optional — see [Packet backend](#packet-backend).

```java
@Override public void onEnable() {
    Aurora.init(this);
    Cosmetics.registerDefaults();
    getServer().getPluginManager().registerEvents(new CosmeticListener(), this);
}

@Override public void onDisable() {
    Aurora.shutdown();
}
```

`CosmeticListener` is not optional: it releases cosmetics on quit and protects Aurora's entities on 1.8.

## Particles

```java
Particles.dust(Color.AQUA).size(1.2f).range(24).spawn(location);
Particles.of(XParticle.FLAME).count(5).offset(0.2).speed(0.01).spawn(location);
Particles.crit().viewer(player).spawn(location);          // private
```

Builders are mutable and meant to be reused. Keep one as a field; don't allocate per tick.

## Shapes

`Shapes` — circle, arc, ellipse, polygon, sphere, dome, helix, doubleHelix, vortex, line,
cube, grid, star, heart, rose, torus, cloud, atom, wings, runeCircle

`Curves` — bezier, spline, arcBetween, lightning, spiralFlat

`Glyphs` — text and ASCII sprites drawn out of particles

Immutable and chainable:

```java
Particles.dust(Color.RED).spawn(location, Shapes.circle(2, 40).rotateY(angle).scale(1.5));
Particles.dust(Color.AQUA).spawn(location, Glyphs.text("GG", 0.12));
```

## Animations

All animations share one server task.

```java
new ShapeAnimation(Animations.follow(player, 0.1), Shapes.circle(1.2, 30), particle)
        .spinDegrees(8)
        .rainbow(0.01f)
        .scaleOverTime(0.2, 1.0, Easing.EASE_OUT_ELASTIC)
        .duration(200)
        .start();
```

Presets in `Animations`: aura, shield, helix, ringWave, burst, beam, trail, orbit, vortex,
heart, slash, lightning, wings, halo, tornado, blockExplosion, laser, text, sprite,
footsteps, pulse, gradientTrail, implode, lob.

Sequences via `Timeline`:

```java
Timeline.create()
        .run(() -> Sounds.ANVIL_LAND.playAt(location, 1f, 0.6f))
        .play(Animations.ringWave(location, particle, 0.5, 6, 20, 40))
        .wait(15)
        .play(Animations.blockExplosion(location, Material.STONE, 4, 20, 60))
        .repeat(3)
        .start();
```

## Holograms

```java
Hologram holo = new Hologram(location)
        .head(Items.head("Notch"))
        .text("&b&lSHOP")
        .text("&7Right click")
        .spawn();

HologramAnimations.rotate(holo.first(HeadLine.class), 5).start();
HologramAnimations.rainbow((TextLine) holo.get(1), "SHOP", 2).start();
```

Also: wobble, dropIn, bob, typewriter, wave, cycle, marquee, blink, countdown, healthBar,
follow, lookAtNearest, orbitHeads, revealLines.

Damage indicators:

```java
FloatingText.damage(location.add(0, 1.8, 0), 7.5);
FloatingText.critical(location, 12);
```

## Cosmetics

```java
CosmeticManager.equip(player, CosmeticRegistry.get(CosmeticCategory.BALLOON, "rojo"));
CosmeticManager.toggle(player, Cosmetics.miniCreeper());
CosmeticManager.unequipAll(player);

Map<CosmeticCategory, String> saved = CosmeticManager.snapshot(player);   // -> your DB
CosmeticManager.restore(player, saved);                                   // <- on join
```

Aurora stores nothing itself. Snapshot and restore are the whole persistence contract.

### Balloons

```java
new BalloonType("red", "&cRed Balloon", new ItemStack(Material.RED_WOOL))
        .leashLength(2.5)
        .height(2.2)
        .ambient(Particles.dust(Color.RED).size(0.7f), 8);
```

Tether modes: `ENTITY` (real lead, anchored to an invisible mob), `PARTICLE` (drawn, no
extra entities), `NONE`.

### Pets

```java
new PetType("creeper", "&aMini Creeper", Items.head("creeper"))
        .head(Items.head("creeper"))   // RenderMode.HEAD
        .hover(0.25)
        .speed(0.3)
        .trail(Particles.dust(Color.LIME).size(0.6f), 6);
```

| Mode | What it is | Small on 1.8? |
|---|---|---|
| `MOB` | real entity | only via `baby()`; `scale()` needs 1.20.5+ |
| `HEAD` | small armour stand wearing a head | **yes**, identical on 1.8 and 1.21 |

Pets are moved by interpolation, not vanilla pathfinding, so behaviour is identical across
versions. They clip through walls on short hops.

### Particle effects

An effect is a function, not a class:

```java
new ParticleEffectType("halo", "&eHalo", icon, (player, at, tick) ->
        dust.spawn(at.clone().add(0, 2.3, 0), RING.rotateY(tick * 0.1)))
        .moving((player, at, tick) -> dust.spawn(at.clone().add(0, 0.6, 0)))
        .interval(2);
```

`.moving()` defines a separate variant used while walking. Without it the same variant is
used in both states.

29 effects ship in `ParticleEffects`. Performance levers, in order of impact:
`interval(n)` → `range(n)` → point counts.

## Packet backend

If PacketEvents is on the server, balloons and `HEAD` pets become packet-only entities:
they don't count towards entity limits, aren't ticked, don't show in `/kill @e` and can be
shown per-player. If it isn't, Aurora falls back to real entities. No configuration needed.

```java
Models.usePackets();        // true if PacketEvents is available
Models.usePackets(false);   // force real entities
```

`MetaIndex` handles the metadata index shifts PacketEvents does not normalise (armour stand
flags moved five times between 1.8 and 1.21).

## Utilities

| | |
|---|---|
| `Colors` | hex, lerp, gradient, rainbow, jitter; palettes FIRE / ICE / TOXIC / VOID |
| `Sounds` | cross-version sound keys, `pitchOf(semitones)` |
| `Entities` | disableAI, invisible, scale, baby, leashTo, persist, tag |
| `Items` | player heads and materials across the 1.13 flattening |
| `Easing` | 8 curves including elastic and bounce |
| `Tasks` | scheduler shortcuts |

## Building

```bash
mvn clean package        # jar, sources jar, javadoc jar
mvn javadoc:javadoc      # target/reports/apidocs
```

Java 17.

## Status

Compiles clean. Not yet tested on a live server — these are the parts to check first:

- **`PacketArmorStandModel` was never compiled against real PacketEvents.** The metadata
  indices are verified against the protocol documentation, but wrapper constructor
  signatures move between releases; check them against your version.
- Head offsets (`HeadLine.yAdjust`) and balloon `height` / `leashLength` are tuned by eye.
- `Entities.disableAI` falls back to reflection on 1.8; a fork with renamed fields will
  skip it. Not fatal, since Aurora teleports its entities every tick anyway.
- Balloons and pets teleport every tick on the Bukkit backend. Use the packet backend at
  scale.

## License

[MIT](LICENSE). Use it in closed-source plugins, commercial or otherwise; keep the
copyright notice.

Aurora's own source contains no code from GPL-licensed cosmetic plugins — the cosmetic
system was written from scratch.

### Third-party

| Dependency | License | Scope |
|---|---|---|
| [XSeries](https://github.com/CryptoMorin/XSeries) | MIT | required, shaded |
| [PacketEvents](https://github.com/retrooper/packetevents) | **GPL-3.0** | optional, `provided` |
| Spigot API | GPL-3.0 | `provided` |

PacketEvents is GPL-3.0. Aurora never bundles it: it is `provided`, resolved reflectively
at runtime, and Aurora falls back to real entities when it is absent. Whether a plugin that
calls a separately installed GPL library at runtime forms a combined work is unsettled and
untested in court, and the Minecraft ecosystem broadly ignores the question — the same
applies to the Spigot API itself, which is also GPL-3.0.

If that ambiguity matters for a closed-source project, `Models.usePackets(false)` disables
the packet backend entirely and nothing in Aurora touches PacketEvents. This is not legal
advice.
