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

Shade it. **No other dependency is required** — Aurora talks to the server itself.
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
Particles.of(ParticleType.FLAME).count(5).offset(0.2).speed(0.01).spawn(location);
Particles.crit().viewer(player).spawn(location);          // private

Particles.dust(Color.WHITE, Color.RED).spawn(location);   // fades white -> red per particle
Particles.endRod().spawn(location, ring);                 // crisp lines that hold their shape
```

Builders are mutable and meant to be reused. Keep one as a field; don't allocate per tick.

Presets fall back on their own: `soulFlame()`, `endRod()`, `snowflake()`, `totem()`,
`soul()`, `electric()`, `petal()` each resolve to something that exists on the running
version, and `dust(from, to)` degrades to plain dust below 1.17.

### Cross-version backend

`org.bukkit.Particle` does not exist on 1.8, and its constants were renamed wholesale in
1.20.5 (`REDSTONE` → `DUST`, `SMOKE_LARGE` → `LARGE_SMOKE`, `EXPLOSION_NORMAL` → `POOF`).
Aurora therefore never names it: `ParticleType` stores each particle under all the names
it has ever had, plus its 1.8 `EnumParticle` constant, and resolves the right one once at
load. Particles added after 1.8 declare a fallback and quietly become it, so every one of
the 40 types draws *something* on every version from 1.8 up.

| | |
|---|---|
| 1.9+ | `Player#spawnParticle`, called reflectively |
| 1.8 | `PacketPlayOutWorldParticle`, built by hand, long-distance flag set |
| colour ≥ 1.13 | `Particle.DustOptions`, built once per colour change |
| colour ≤ 1.12 | count `0`, colour in the offsets, speed `1` — with a red of zero nudged above zero, because the client reads an exact zero as full red |
| fading colour | `DustTransition` on 1.17+, first colour below it |
| blocks and items | `BlockData` on 1.13+, `MaterialData` on 1.9-1.12, packed ints on 1.8 |

Shapes can be drawn with a colour sampled along their length rather than in one flat tone,
which is most of the difference between a decal and an effect:

```java
Particles.dust(Color.RED).spawn(location, ring, ColorRamp.of(Colors.FIRE));
Particles.dust(Color.RED).spawn(location, helix, ColorRamp.rainbow(time).repeat(2));
```

`ColorRamp` — solid, of(palette), between, rainbow; chain `repeat`, `mirror`, `jitter`.

### Performance

Point counts, effect intervals and view range all follow one global quality level, and
that level drops on its own when the server starts falling behind:

```java
RenderSettings.quality(RenderQuality.HIGH);   // LOW / MEDIUM / HIGH / ULTRA
RenderSettings.adaptive(true);                // drop a level below 19 TPS, two below 17
RenderSettings.rangeMultiplier(0.8);          // shorter view range on a busy lobby
RenderSettings.enabled(false);                // silence everything without unequipping
```

Effects animate against elapsed seconds rather than frame counts, so dropping frames
costs particles and not animation speed. Audiences are resolved once per effect frame
through a per-tick snapshot of each world's players, instead of copying the player list
on every spawn call.

## Shapes

`Shapes` — circle, arc, ellipse, polygon, sphere, spiralSphere, dome, helix, doubleHelix,
vortex, cone, column, line, cube, grid, star, heart, rose, flower, crescent, torus, cloud,
atom, orbit, galaxy, crown, wings, dragonWings, butterflyWings, runeCircle

`Curves` — bezier, spline, arcBetween, lightning, spiralFlat

`Glyphs` — text and ASCII sprites drawn out of particles

Immutable and chainable: `rotateX/Y/Z`, `scale`, `translate`, `facing`, `facingYaw`,
`plus`, `jitter`, `take`.

```java
Particles.dust(Color.RED).spawn(location, Shapes.circle(2, 40).rotateY(angle).scale(1.5));
Particles.dust(Color.AQUA).spawn(location, Glyphs.text("GG", 0.12));

// take(fraction) draws a shape on over time, which is what makes a summoning
// circle read as a spell rather than as a texture
runes.spawn(location, Shapes.runeCircle(1.5, 6, 0.4, 40).take(progress));
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

An effect is a function of a frame, not a class:

```java
new ParticleEffectType("halo", "&eHalo", icon, ctx ->
        ctx.emit(dust, ctx.head(), RING.rotateY(ctx.time() * 2), ColorRamp.of(Colors.GOLD)))
        .state(MovementState.SPRINTING, ctx -> ctx.trail(3, (at, t) -> ctx.emit(dust, at)))
        .sound(Sounds.ORB, 60, 0.12f, 1.5f, 1.9f)
        .interval(2)
        .range(28);
```

`EffectContext` is the frame: where the wearer is, what they are doing, who can see it.

| | |
|---|---|
| `time()` | elapsed **seconds** — animate against this, never the tick counter |
| `state()` | IDLE / WALKING / SPRINTING / SNEAKING / AIRBORNE / FLYING |
| `trail(steps, step)` | walks the ground actually covered since the last frame |
| `yaw()` | body yaw, smoothed, so back-mounted shapes don't snap on a mouse flick |
| `seed()`, `phase()`, `noise(f)` | per-wearer drift, so a group never pulses in lockstep |
| `head()`, `chest()`, `feet()`, `behind()`, `beside()` | anchors relative to the body |
| `emit(...)` | spawns with the frame's audience, resolved once |
| `count(n)` | scales a loop count by the current quality, as shapes already are |

`.state()` defines the variant for one movement state; states with none of their own fall
back through SPRINTING → WALKING → IDLE and FLYING → AIRBORNE → WALKING. `.moving()` is
shorthand for the walking variant, and the older `(player, at, tick)` renderer still works
unchanged.

Also on the type: `.sound()` for an ambient loop played only to players who can see the
effect, `.hideWhenInvisible()` (on by default — a cosmetic that keeps drawing on a
vanished player is a wallhack), and `.showToWearer()`.

**49 effects** ship in `ParticleEffects`: fire, ice, water, five sets of wings, love,
magic, party, dark and cosmic. Performance levers, in order of impact:
`RenderSettings.quality(...)` → `interval(n)` → `range(n)` → point counts.

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
| `Colors` | hex, lerp, gradient, wave, shade, glow, rainbow, jitter; 14 palettes |
| `ColorRamp` | colour as a function of position along a shape |
| `Noise` | continuous value noise, so effects drift instead of flickering |
| `ServerLoad` | rolling TPS estimate, without needing Paper |
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

`mvn package` builds clean. Not yet tested on a live server — these are the parts to
check first:

- **`PacketArmorStandModel` was never run against real PacketEvents.** The metadata
  indices are verified against the protocol documentation and the spawn wrapper matches
  2.9, but wrapper constructor signatures move between releases; check them against your
  version.
- **The 1.8 particle path is packet code and needs a real 1.8 server to confirm.** The
  packet signature and the colour-in-offsets encoding are the documented ones, and the
  name table, fallback chains and encodings are covered by an offline check, but no
  packet has been put in front of a 1.8 client.
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
