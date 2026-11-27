# Example Projects & Reference Assets

[Wiki Home](../Home.md) | [简体中文](../zh/示例工程与动画参考.md)

The repository includes complete **Blockbench `.bbmodel` scene projects** and exported **`.animation.json`** files under [`examples/`](../../../examples/README.md) so modders and animators can inspect working setups directly.

> **Not Bundled in Release JARs**: `examples/` sits completely outside `common/` and `targets/`. It is excluded from compiled mod `.jar` and `-sources.jar` artifacts.

---

## Included Examples

### 1. Duel Clash (`duel_clash`)

- **Blockbench Project**: [`examples/duel_clash/duel_clash.bbmodel`](../../../examples/duel_clash/duel_clash.bbmodel)
- **Exported Animation**: [`examples/duel_clash/duel_clash.animation.json`](../../../examples/duel_clash/duel_clash.animation.json)
- **Clip Name**: `animation.example.duel_clash` (`1.6s`)

Demonstrates:
- Primary subject (`body` / `head` / `right_arm` / `left_arm`) + participant `victim` (`minecraft:player`).
- `interactive_animation:root` whole-actor displacement and `360°` spin.
- Both `linear` and `catmullrom` interpolation, plus `bend` channel authoring via `right_arm_bend`.
- Authored `camera` track (`position` & `rotation`).
- Primary `timeline` markers (`"impact"` at `0.45s`, `"release"` at `1.1s`).
- Primary `sound_effects` array (`minecraft:entity.iron_golem.attack` with `locator: "right_arm"`, plus `minecraft:block.anvil.land`) and `particle_effects` (`minecraft:crit` with `pre_effect_script: "impact_burst"`).
- Participant `sound_effects` (`minecraft:entity.player.attack.knockback`) and `particle_effects` (`minecraft:cloud`).

### 2. Door Interaction (`door_interaction`)

- **Blockbench Project**: [`examples/door_interaction/door_interaction.bbmodel`](../../../examples/door_interaction/door_interaction.bbmodel)
- **Exported Animation**: [`examples/door_interaction/door_interaction.animation.json`](../../../examples/door_interaction/door_interaction.animation.json)
- **Clip Name**: `animation.example.open_door` (`1.4s`)

Demonstrates:
- Static/block-anchored hinged `door_leaf` swing (`catmullrom` easing).
- Participant `actor` (`minecraft:player`) walking to and stepping through the doorway with `interactive_animation:root` + arm/leg rotation.
- `timeline` markers (`"door_open"`, `"step_through"`), `sound_effects` (`minecraft:block.iron_door.open`), and shorthand `particle_effects` (`"minecraft:crit"`).

---

## Workflow

### Editing in Blockbench

1. Install **AnimCue Studio** (`tools/blockbench/dist/animcue_studio.js` or from the Blockbench Plugin Store) in Blockbench `5.0.0+`.
2. Open either `.bbmodel` file directly to inspect the Outliner participant tags, camera rig, bone tracks, and Effects (`timeline` / `sound` / `particle`) track.
3. Export via the **AnimCue** sidebar Export button or **File → Export → AnimCue Animation...**.

### Playing from Mod Code

Place the `.animation.json` file inside **your own mod's** assets folder (for example `assets/mymod/animations/duel_clash.animation.json`), register it at startup, and handle or customize markers, sounds, and particles in `PlaybackOptions`:

```java
// 1. Register during mod initialization
InteractiveScenes.register(MyMod.class, "mymod:animations/duel_clash.animation.json");

// 2. Trigger on the server
var sceneId = new SceneId("mymod:animations/duel_clash.animation.json", "animation.example.duel_clash");
var subject = SceneSubject.entity(bossEntity);
var bindings = Bindings.named(Map.of("victim", targetPlayer));

var options = PlaybackOptions.DEFAULT
        .markers(marker -> {
            if ("release".equals(marker.instruction())) {
                marker.playback().useWorldCoordinates("victim");
            }
        })
        .sounds(kf -> {
            // Optional override; omit .sounds(...) to let AnimCue auto-play registered sounds
            SceneEffects.playSound(level, kf);
        })
        .particles(kf -> {
            // Optional override; omit .particles(...) to let AnimCue auto-spawn registered particles
            SceneEffects.spawnParticle(level, kf);
        });

InteractiveScenes.play(subject, sceneId, bindings, options);
```
