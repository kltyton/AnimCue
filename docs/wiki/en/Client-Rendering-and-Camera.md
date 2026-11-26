# Client Rendering & Camera

[Wiki Home](../Home.md) | [简体中文](../zh/客户端渲染与镜头.md)

Client-side helpers live in `com.kltyton.interactiveanimation.client.playback` and `com.kltyton.interactiveanimation.client.render`.

---

## `ClientScenes`

### Local Preview Playback

Preview default assets or client-bound objects directly on the render/client game thread without server packets:

```java
SceneHandle handle = ClientScenes.playLocal(subject, sceneId);
SceneHandle bound = ClientScenes.playLocal(subject, sceneId, Bindings.named(Map.of("prop", stack)), options);
```

### Querying Active Client Actors

Mods building custom camera rigs, vehicle render hooks, or contact filters can inspect active client scenes at any time:

- `ClientActor ClientScenes.actor(Entity entity, float partialTick)`
  Returns the interpolated `ClientActor` for `entity` if it is currently driven by a scene (either as primary or participant), or `null`.
- `Entity ClientScenes.subjectFor(Entity participant, String role)`
  Returns the tracked subject entity for a participant currently playing `role` (used by mods such as *Superb Warfare: Boarding* to coordinate camera tracking and collision contact).
- `List<ClientActor> ClientScenes.extras(float partialTick, Predicate<Entity> submitted)`
  Returns non-world preview actors (default `item`, `block`, or `model` participants) for extra render passes.
- `ClientScene.CameraPose ClientScenes.camera(Entity player, float partialTick)`
  Returns the current scene camera pose if a scene is controlling the camera for `player`.

---

## `ClientAssets`

When a participant uses `"type": "model"`, register its client-side preview factory during client initialization:

```java
ClientAssets.registerModel("example:ritual_blade", level -> new ItemStack(ExampleItems.RITUAL_BLADE.get()));
```

The factory must return an `Entity`, `ItemStack`, or `BlockState` instance that Minecraft's renderer pipeline can draw.

---

## Camera Modes (`PlaybackOptions.CameraMode`)

| Mode | Behavior |
| --- | --- |
| `AUTO` | Uses the scene's `camera` track if authored; otherwise applies smooth follow framing for participating players. |
| `VANILLA` | Leaves camera control to vanilla Minecraft or the calling mod's custom camera controller. |
| `FOLLOW` | Keeps the current first- or third-person view while following the participant's root motion. Ignores the authored `camera` track. |
| `LOCKED` | Locks camera orientation to the scene's authored `camera` track. |

---

## Player Bones, Outer Skin Layers & Bending

- **Player Bone Names**: Standard humanoid bones (`head`, `body` / `torso`, `left_arm`, `right_arm`, `left_leg`, `right_leg`) are synchronized across both inner geometry and second-layer player skin overlays (`hat`, `jacket`, `left_sleeve`, `right_sleeve`, `left_pants`, `right_pants`).
- **PAL & PAL More Rotation (`1.21.1`, `26.1.2`, `26.2`)**: When Player Animation Library is installed, player poses are applied through PAL's animation layer (`PalSceneLayer` / `PalScenePose`). When PAL More Rotation is also installed, multi-axis `bend` curves on `body` / limbs are forwarded automatically via `PalMoreBends`.
- **1.20.1 Legacy Path**: Applies poses through vanilla player model channels, allowing downstream mods to bridge bend tracks through playerAnimator / bendy-lib when present.

---

## GeckoLib & KBR Render Integration

- **GeckoLib 4 (`1.20.1`, `1.21.1`) & GeckoLib 5 (`26.1.2`, `26.2`)**: Participant bone channels are applied after GeckoLib's native controller sampling, preserving native idle/walk/wheel/turret animations on untouched bones.
- **KBR Renderers (`26.x`)**: When KBR addon entity renderers are present, AnimCue applies interactive bone channels after native pose composition on both the main body and extra geometry passes.
- **Custom Model Hooks (`LegacyActorRenderer`)**: On `1.20.1` and `1.21.1`, custom entity renderers can read `LegacyActorRenderer.current()` and register post-draw cleanup callbacks via `LegacyActorRenderer.restoreAfterDraw(Runnable)` to animate non-GeckoLib rigs (such as SimpleBedrockModel vehicles).
