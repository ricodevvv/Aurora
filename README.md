# Aurora — sistema de partículas, animaciones y hologramas (1.8 → 1.21)

Librería sin dependencias externas. Un solo jar corre de **1.8.8 a 1.21+** porque
todo lo que cambió entre versiones se resuelve por reflection en runtime.

```
com.ricodevvv.aurora
├── Aurora                      init/shutdown
├── particle/                   Particles, ParticleBuilder, ParticleType
│   └── backend/                LegacyBackend (1.8) · ModernBackend (1.9+)
├── shape/                      Shape, Shapes, Curves (bézier/spline/rayos), Glyphs (texto y sprites)
├── animation/                  Animation, AnimationManager, ShapeAnimation, Animations, Timeline
├── hologram/                   Hologram, TextLine, HeadLine, HologramAnimations, FloatingText
├── cosmetic/                   CosmeticRegistry, CosmeticManager, Cosmetics
│   ├── balloon/                Balloon, BalloonType
│   ├── pet/                    Pet, PetType
│   └── particle/               ParticleEffect, ParticleEffectType, ParticleEffects (29 efectos)
├── model/                      Model, Models, BukkitArmorStandModel, PacketArmorStandModel
├── packet/                     MetaIndex, PacketSupport
└── util/                       ServerVersion, Reflect, VectorMath, Easing, Items
```

## Arranque

```java
@Override public void onEnable()  { Aurora.init(this); }
@Override public void onDisable() { Aurora.shutdown(); }
```

## Partículas

```java
Particles.dust(Color.AQUA).size(1.2f).range(24).spawn(loc);
Particles.of(ParticleType.FLAME).count(5).offset(0.2).speed(0.01).spawn(loc);
Particles.of(ParticleType.DUST).color(255, 80, 0).spawn(loc, Shapes.circle(2, 40));
Particles.crit().viewer(player).spawn(loc);   // efecto privado
```

Sin `viewers()` explícito manda solo a los jugadores dentro del `range` (32 por defecto).

## Figuras

`circle`, `arc`, `ellipse`, `polygon`, `sphere` (Fibonacci, sin acumularse en los polos),
`dome`, `helix`, `doubleHelix`, `vortex`, `line`, `cube`, `grid`, `star`, `heart`, `rose`,
`torus`, `cloud`, `atom`, `wings`, `runeCircle`.

En `Curves`: `bezier` (cuadrática y cúbica), `spline` (Catmull-Rom), `arcBetween`,
`lightning` (desplazamiento de punto medio, sale distinto cada vez), `spiralFlat`.

Son inmutables y encadenables: `Shapes.circle(2, 40).rotateY(rad).scale(1.5).facing(loc)`.

### Texto y sprites con partículas

```java
Particles.dust(Color.AQUA).spawn(loc, Glyphs.text("GG", 0.12));
Particles.dust(Color.RED).spawn(loc, Glyphs.sprite(Glyphs.SKULL, 0.15));
```

Fuente propia de 3x5 px (A-Z, 0-9 y símbolos). El sprite se define con arte ASCII,
así que puedes meter el tuyo sin tocar código.

## Animaciones

Todo corre en **un solo task** del servidor, no uno por efecto.

```java
new ShapeAnimation(Animations.follow(player, 0.1), Shapes.circle(1.2, 30), Particles.dust(Color.AQUA))
        .spinDegrees(8)
        .rainbow(0.01f)
        .scaleOverTime(0.2, 1.0, Easing.EASE_OUT_ELASTIC)
        .duration(200)
        .start();
```

Presets en `Animations`: `aura`, `shield`, `helix`, `ringWave`, `burst`, `beam`,
`trail`, `orbit`, `vortex`, `heart`, `slash`, `lightning`, `lightningStrike`, `wings`,
`halo`, `tornado`, `blockExplosion`, `laser`, `text`, `sprite`, `footsteps`, `pulse`,
`gradientTrail`, `implode`, `lob`.

### Timeline

Encadena efectos sin anidar cinco `runTaskLater`, y todo dentro del mismo task global:

```java
Timeline.create()
        .run(() -> Sounds.ANVIL_LAND.playAt(loc, 1f, 0.6f))
        .play(Animations.ringWave(loc, particle, 0.5, 6, 20, 40))
        .wait(15)
        .play(Animations.blockExplosion(loc, Material.STONE, 4, 20, 60))
        .wait(20)
        .play(Animations.text(loc.add(0, 3, 0), "BOOM", particle, 0.14).duration(60))
        .repeat(3)
        .start();
```

## Hologramas

ArmorStands **reales**, no paquetes: la API de ArmorStand no cambió de 1.8 a 1.21,
así que esto corre en todas las versiones sin una línea de NMS.

```java
Hologram holo = new Hologram(loc)
        .head(Items.head("Notch"))
        .text("&b&lTIENDA")
        .text("&7Click derecho")
        .spawn();

HologramAnimations.rotate(holo.first(HeadLine.class), 5).start();  // rotating head
HologramAnimations.rainbow((TextLine) holo.get(1), "TIENDA", 2).start();
HologramAnimations.bob(holo, 0.15, 0.1).interval(2).start();
```

Otras: `wobble`, `dropIn`, `typewriter`, `wave`, `cycle`, `orbitHeads`, `follow`,
`healthBar`, `countdown`, `marquee`, `blink`, `lookAtNearest`, `revealLines`.

### Indicadores de daño

```java
FloatingText.damage(loc.add(0, 1.8, 0), 7.5);   // -7.5 en rojo, sube y se borra solo
FloatingText.critical(loc, 12);
FloatingText.spawn(loc, "&e+50 coins", 24, 1.0);
```

Cada llamada crea y destruye un ArmorStand: para un 1v1 va bien, para un evento de
100 jugadores conviene limitar por jugador.

## Cosméticos (globos y mini pets)

Escritos desde cero, no portados. Ver la nota de licencia al final.

```java
// onEnable
Aurora.init(this);
Cosmetics.registerDefaults();
getServer().getPluginManager().registerEvents(new CosmeticListener(), this);

// equipar
CosmeticManager.equip(player, CosmeticRegistry.get(CosmeticCategory.BALLOON, "rojo"));
CosmeticManager.toggle(player, Cosmetics.miniCreeper());

// persistencia: tú decides dónde guardar
Map<CosmeticCategory, String> saved = CosmeticManager.snapshot(player);  // -> Mongo
CosmeticManager.restore(player, saved);                                   // <- al entrar
```

### Globos

Física propia: inercia amortiguada + viento senoidal con ruido + restricción dura
de distancia al hombro. La inercia se recalcula **desde el desplazamiento real
después** de aplicar la restricción — eso es lo que hace que el globo rebote al
tensarse la cuerda en vez de quedarse tieso.

```java
new BalloonType("rojo", "&cGlobo Rojo", new ItemStack(Material.RED_WOOL))
        .leashLength(2.5)
        .height(2.2)
        .ambient(Particles.dust(Color.RED).size(0.7f), 8)
        .popColor(Color.RED);
```

Tres modos de cuerda: `ENTITY` (correa real, ancla = conejo bebé invisible),
`PARTICLE` (cuerda dibujada, cero mobs extra) y `NONE`.

### Efectos de partículas

29 efectos. Un efecto **no es una clase**, es una función `(jugador, posición, tick) → partículas`:

```java
new ParticleEffectType("mi_efecto", "&bMi Efecto", icon, (player, at, tick) -> {
        DUST.color(Color.AQUA).spawn(at.clone().add(0, 1, 0), SHAPE_RING.rotateY(tick * 0.1));
})
.interval(2)
.trigger(Trigger.MOVING);
```

En ProCosmetics cada efecto es una clase propia con su runnable — 36 archivos casi
idénticos. Aquí un efecto son 3 líneas, y como `EffectRenderer` es interfaz funcional
puedes registrar los tuyos sin tocar la librería.

Catálogo: fuego (aura, anillos, llama demoníaca), hielo (señor del hielo, copo, nube de
nieve), agua (nube de lluvia, paraguas), alas (ángel, vampiro, arcoíris), amor (enamorado,
corazón gigante), mágicos (encantado, aura legendaria, escudo, agujero negro, yin-yang),
fiesta (notas, confeti, hora de fiesta, estrella), oscuros (calavera, hélices de sangre,
caminar en sombras), rastros (colorido, tornado, círculo de runas, eléctrico).

**Rendimiento — lo que más importa aquí.** Un efecto de 60 partículas cada tick con 50
jugadores equipados son 60.000 partículas por segundo. Tres palancas:

- `interval(n)` — la más importante. A `interval(3)` bajas a un tercio y casi no se nota.
- `range(n)` — a quién se le manda; por defecto 32 bloques.
- `trigger(MOVING/IDLE)` — los rastros solo dibujan al moverse, las auras solo al parar.

Además `ParticleEffect` corta antes de dibujar si no hay nadie en rango o si el jugador
está en espectador. Y los builders y las figuras estáticas se precalculan una sola vez:
generar una esfera de 80 puntos cada tick es trigonometría desperdiciada.

### Mini pets

Dos modos de render, y aquí está lo importante para 1.8:

| Modo | Qué es | "Mini" en 1.8 |
|---|---|---|
| `MOB` | entidad real (lobo, pollo…) | solo vía `baby()` — `scale()` se ignora abajo de 1.20.5 |
| `HEAD` | ArmorStand pequeño con cabeza de casco | **sí**, se ve igual de chiquito en 1.8 que en 1.21 |

```java
new PetType("creeper", "&aMini Creeper", Items.head("creeper"))
        .head(Items.head("creeper"))     // modo HEAD
        .hover(0.25)
        .speed(0.3)
        .trail(Particles.dust(Color.LIME).size(0.6f), 6);
```

El movimiento lo hace Aurora con interpolación, **no el pathfinder de vanilla**:
mismo comportamiento exacto en todas las versiones, sin gastar ticks de IA y sin
pelear con el servidor por el control de la entidad. A cambio la mascota atraviesa
paredes en trayectos cortos — para un cosmético de lobby eso se ve mejor que una
mascota atorada en una esquina.

## Backend por paquetes (PacketEvents)

**PacketEvents es opcional.** Si está en runtime, los globos y los mini pets en modo
`HEAD` pasan a ser entidades falsas. Si no está, cae solo a entidades reales y nadie
se entera. No hay que configurar nada:

```java
Models.usePackets();        // true si PacketEvents está disponible
Models.usePackets(false);   // forzar entidades reales (para depurar)
```

Ganancia real del modo paquetes:

- No existen en el mundo: no cuentan para el límite de entidades, no las tickea el
  servidor, no aparecen en `/kill @e`, no las ve el anticheat.
- Se pueden mostrar a unos jugadores y a otros no — cosméticos privados, vanish, equipos.
- Cero carga en el main thread más allá de armar los paquetes.

Costo: el tracking de espectadores va a mano. `PacketArmorStandModel.refreshViewers()`
manda solo los deltas (spawn a los que entraron, destroy a los que salieron); sin eso
un jugador que se acerca nunca vería el globo, porque los paquetes no se reenvían solos
como sí pasa con una entidad real.

### MetaIndex — lo que PacketEvents NO resuelve

Los wrappers de PacketEvents unifican los **paquetes**, pero el índice y el tipo de cada
campo de metadata los mandas crudos. Y Mojang los recorrió varias veces metiendo campos
en medio de las clases base. Esto es lo que rompe a la gente que intenta esto a mano:

| Versión | Cambio | Flags de ArmorStand |
|---|---|---|
| 1.8 | Entity 0-3, Living 6-9 | **10** |
| 1.9 – 1.13 | +Silent, +NoGravity en Entity | **11** |
| 1.14 | +Pose en Entity(6), +BedLocation en Living | **13** |
| 1.15 – 1.16 | +StingerCount en Living | **14** |
| 1.17+ | +FrozenTicks en Entity(7) | **15** |

Además el tipo del custom name cambia en 1.13 (`String` → `Optional<Component>`) y el de
"name visible" en 1.9 (`Byte` → `Boolean`). Todo eso vive en `MetaIndex`: si algún día se
recorre otra vez, ese es el único archivo que se toca.

## Utilidades

- **`Colors`** — `hex("#FF8800")`, `lerp`, `gradient(paleta, t)`, `rainbow(hue)`, `jitter`,
  y paletas listas: `FIRE`, `ICE`, `TOXIC`, `VOID`.
- **`Sounds`** — sonidos multiversión. No usa el enum `Sound` porque los nombres cambiaron
  en 1.9, otra vez en 1.13, y en 1.21.3 `Sound` dejó de ser enum para volverse registro.
  Va por `playSound(Location, String, float, float)`, que existe igual desde 1.8.
  También trae `pitchOf(semitonos)` para armar melodías.
- **`Tasks`** — `later`, `sync`, `async`, `timer` sin cargar el `Plugin` a todos lados.
- **`Entities`** — compatibilidad de entidades: `disableAI` (setAI en 1.9+, goal selectors
  por reflection en 1.8), `scale` (solo 1.20.5+, te dice si aplicó), `baby`, `silence`,
  `invisible`, `leashTo`, `persist`. Nada lanza excepción: si no existe en la versión,
  no pasa nada.
- **`Easing`** — 8 curvas, incluidas `EASE_OUT_ELASTIC` y `EASE_OUT_BOUNCE`.

## Cómo resuelve las diferencias de versión

| Problema | Solución |
|---|---|
| Detección de versión | `Bukkit.getBukkitVersion()`, **no** el paquete de CraftServer — Paper dejó de relocalizarlo en 1.20.5 |
| No hay `Particle` API en 1.8 | `LegacyBackend` arma `PacketPlayOutWorldParticles` con `EnumParticle` |
| Mojang renombró medio enum en 1.20.5 | Cada `ParticleType` trae varios alias (`DUST`/`REDSTONE`, `HAPPY_VILLAGER`/`VILLAGER_HAPPY`…) y se resuelve el primero que exista |
| Colores | 1.13+ → `DustOptions`; 1.9-1.12 y 1.8 → el color va en los offsets con `count = 0` |
| Bloques/items en partículas | 1.13+ → `BlockData`/`ItemStack`; 1.9-1.12 → `MaterialData`; 1.8 → `int[]{id + (data << 12)}` |
| Cabezas | `PLAYER_HEAD` vs `SKULL_ITEM:3`, `setOwningPlayer` vs `setOwner` |
| Sonidos | claves como String (`random.levelup` / `entity.player.levelup` / `block.note_block.pling`), nunca el enum |
| Partículas que no existen en 1.8 (`END_ROD`, `SOUL`…) | fallback a `FLAME` con aviso en consola, no revienta |

## Compilar e integrar

El `pom.xml` compila contra **spigot-api 1.8.8** a propósito: lo moderno va por
reflection, así que el mismo jar sirve para todo. Para meterlo en Frost tienes dos
caminos, según tu setup de Sapphire-Dependencies:

1. Meterlo en el uber jar de Sapphire con relocación (`com.ricodevvv.aurora` →
   `club.frozed.libs.aurora`) y dejar el jar de Frost limpio.
2. Copiar el paquete directo al proyecto si prefieres no tener otro artefacto.

## Puntos a tunear

- **Offsets de `HeadLine`** (`yAdjust`: -1.45 normal, -1.05 pequeño). Son valores
  probados pero dependen del modelo; si te queda alto o bajo, `line.yAdjust(x)`.
- **`bob()`** teletransporta ArmorStands cada tick. Con muchos hologramas a la
  vista sube el tráfico; súbele el `interval`.
- Si algún día quieres holograma **por jugador**, reemplaza `HologramLine.spawnStand()`
  por tu capa de paquetes: el resto del sistema no se entera.

## Sobre ProCosmetics

El zip que subiste (`se.filledev`, ProCosmetics 2.0.6) es **GPL-3.0** — cabecera en cada
archivo, repo `github.com/FilleDev/ProCosmetics`. La GPL es viral: código suyo dentro de
Aurora haría que Aurora fuera GPL, y como Frost enlazaría Aurora, **Frost tendría que
publicarse con fuente bajo GPL también**.

Por eso este módulo está escrito desde cero. Además su implementación no era portable:

| Lo que usan | Desde | En 1.8 |
|---|---|---|
| `ItemDisplay` + `Matrix4f` | 1.19.4 | ArmorStand con casco |
| `Attribute.SCALE` | 1.20.5 | no existe → `baby()` o modo `HEAD` |
| `addPassenger` | 1.9 | `setPassenger` (uno solo) |
| `setSilent` | 1.9 | no hay flag de silencio |
| `Mob#setAI` | 1.9 | limpiar goal selectors por NMS |
| `player.getMainHand()` | 1.9 | no hay mano secundaria |

Lo mismo aplica a los efectos de partículas: los nombres de efectos y las ideas
(alas, tornado, agujero negro) no son propiedad de nadie, pero su código sí es GPL. Los
29 de aquí están escritos sobre el motor de shapes de Aurora, no traducidos del suyo.

Lo único que tomé fue el enfoque general de simulación del globo (inercia + amortiguación
+ restricción de distancia + viento), que es técnica estándar de gamedev y no invención
suya. Si prefieres usar ProCosmetics tal cual, es software libre y puedes correrlo —
lo que no conviene es mezclarlo con código tuyo que quieras mantener cerrado.

## Estado

Compila limpio con Java 17. No lo he podido probar en un servidor real desde aquí,
así que estos puntos van a querer ajuste en vivo:

- Offsets de `HeadLine` y altura de globo (`height`, `leashLength`).
- El ancla de correa en 1.8: `disableAI` cae a reflection sobre `EntityInsentient`, y si
  tu fork cambió esos campos no aplicará. No es grave (el conejo se teletransporta cada
  tick igual), pero puede hacer ruido — si molesta, usa `LeashMode.PARTICLE`.
- **`PacketArmorStandModel` no lo pude compilar contra PacketEvents real**, solo contra
  stubs míos. Los índices de metadata sí los verifiqué contra la documentación del
  protocolo, pero las **firmas de los constructores de los wrappers** (`SpawnEntity`,
  `EntityTeleport`, `AttachEntity`) se mueven entre releases de PacketEvents — la del
  `pom.xml` (2.9.4) puede no coincidir con la tuya. Es lo primero que revisaría al
  compilar de verdad.
- El modo `MOB` de las mascotas sigue usando entidades reales. Hacerlo por paquetes
  requiere el índice de "is baby" y los flags propios de cada mob (`MetaIndex.ageableBaby()`
  ya está, pero cada especie tiene lo suyo); el modo `HEAD` sí es 100% paquetes.
- `VIEW_RANGE_SQ` está fijo en 48 bloques. Si tu view-distance es menor, bájalo.
