# Playback & Binding API

[Wiki Home](../Home.md) | [简体中文](../zh/播放与绑定API.md)

All public server/common API types live in `com.kltyton.interactiveanimation.api` (with GeckoLib helpers in `com.kltyton.interactiveanimation.compat.geckolib`).

---

## `InteractiveScenes`

Entry point for registering packaged scene files and starting server-authoritative scenes.

```java
InteractiveScenes.register(Class<?> resourceOwner, String animationFile);

SceneHandle InteractiveScenes.play(SceneSubject subject, SceneId scene, Object... participants);
SceneHandle InteractiveScenes.play(SceneSubject subject, SceneId scene, Bindings bindings, PlaybackOptions options);
```

- `animationFile` must be a valid resource path ending in `.json`, e.g. `"mymod:animations/duel.animation.json"`.
- The client reads the same resource path automatically during resource reload.

---

## `SceneId`

Identifies one scene inside an animation file:

```java
var id = new SceneId("mymod:animations/duel.animation.json", "animation.duel.clash");
```

---

## `SceneSubject` & `GeckoSubjects`

A `SceneSubject` supplies the spatial origin (`Frame(position, yaw)`), validity predicate, optional backing `Entity`, and optional primary animation controller callbacks (`play` / `stop` / `transitionTicks`).

### Built-In Factories

```java
SceneSubject.entity(Entity entity)
SceneSubject.block(BlockEntity block, float yaw)
SceneSubject.item(ItemStack stack, Entity holder)
```

Attach an existing animation controller via `playback(...)`:

```java
SceneSubject subject = SceneSubject.entity(boss)
        .playback(transitionTicks, () -> startNativeAnim(), () -> stopNativeAnim());
```

For custom moving origins (such as scripted vehicle trajectories), construct `new SceneSubject(level, frameSupplier, validSupplier, entity, transitionTicks, nativePlayback, play, stop)` directly.

### GeckoLib Helpers (`GeckoSubjects`)

```java
// Entity (GeckoLib 4 & 5)
GeckoSubjects.entity(geoEntity, "controllerName", "triggerName", serverTransitionTicks);

// Entity with controller reference (GeckoLib 5)
GeckoSubjects.entity(geoEntity, controller, "triggerName");

// BlockEntity & ItemStack
GeckoSubjects.block(geoBlockEntity, yaw, "controllerName", "triggerName", transitionTicks);
GeckoSubjects.item(stack, holder, instanceId, "controllerName", "triggerName", transitionTicks);
```

---

## `Bindings`

Maps runtime objects (`Entity`, `ItemStack`, or `BlockState`) to participant roles and optional bone remapping tables.

```java
Bindings.defaults()                          // Use default preview assets only
Bindings.ordered(entityA, entityB)           // Bind in JSON participants array order
Bindings.named(Map.of("rider", player))      // Bind explicitly by role name
```

### Bone Remapping

Remap authored bone names in the JSON to target model bone names without editing the animation asset:

```java
var bindings = Bindings.named(Map.of("hero", player))
        .mapBones("hero", Map.of("torso", "body"));
```

Direction: **authored bone name → actual model bone name**.

---

## `PlaybackOptions`

Immutable configuration record starting from `PlaybackOptions.DEFAULT`:

| Option / Builder | Default | Description |
| --- | --- | --- |
| `alignment(snap, checkPath, tolerance, handler)` | `snap=true`, `checkPath=false`, `tolerance=0.0625` | Controls initial alignment, optional approach path sweep, tolerance (blocks), and `Consumer<AlignmentResult>` callback. |
| `controls(lockDriven, subjectAi, attack, interaction)` | `lockDriven=true`, others `false` | Locks movement/AI of driven participants, optionally pauses subject AI, attacks, or interactions. |
| `camera(CameraMode)` | `CameraMode.AUTO` | `AUTO`, `VANILLA`, `FOLLOW`, or `LOCKED`. |
| `carrier(Entity)` | `null` | Optional carrier entity whose motion the scene follows. |
| `markers(Consumer<Marker>)` | no-op | Receives `Marker(playback, instruction, seconds)` callbacks from the primary animation's `timeline`. |

### `AlignmentResult`

```java
record AlignmentResult(String role, Status status, Vec3 initial, Vec3 target, Vec3 reached)
```

`Status` values: `ALIGNED`, `MISALIGNED`, `BLOCKED`, `CANCELLED`. Helper methods: `aligned()`, `initialOffset()`, `remainingOffset()`.

---

## `SceneHandle`

Returned by `InteractiveScenes.play(...)` and `ClientScenes.playLocal(...)`:

- `State state()` — `APPROACHING`, `PLAYING`, `FINISHED`, or `CANCELLED`.
- `Map<String, AlignmentResult> alignmentResults()` — Alignment outcome per participant role.
- `Object binding(String role)` — Bound live instance for a role.
- `void cancel()` — Stops the scene immediately and releases all control leases.
- `void useWorldCoordinates(String role)` — Detaches a participant from subject-relative coordinates into world space (useful for throws/releases triggered from a `timeline` marker).
- `void follow(String role, Entity carrier)` — Switches a participant to follow a specific carrier entity.

---

## `AnimationControls`

Reference-counted control leases attached to live entities (`ai`, `movement`, `attacks`, `interaction`). Multiple overlapping scenes combine cleanly and restore original state only when the last lease closes.

Mods with custom entity AI/controllers can integrate via `AnimationControls.register`:

```java
AnimationControls.register(MyModEntities.CUSTOM_BOSS.get(), (entity, controls) -> {
    var boss = (CustomBossEntity) entity;
    if (controls.ai()) boss.setScriptPaused(true);
    return () -> {
        if (controls.ai()) boss.setScriptPaused(false);
    };
});
```
