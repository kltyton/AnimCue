# AnimCue Art & Animation Examples

[English Documentation](../docs/wiki/en/Examples.md) | [中文示例文档](../docs/wiki/zh/示例工程与动画参考.md)

This directory contains **standalone developer reference projects** (Blockbench `.bbmodel` scene projects and exported `.animation.json` files) for the **AnimCue (`interactive_animation`)** library.

> **Important**: `examples/` lives outside `common/` and `targets/`. Nothing in this directory is compiled into or packaged inside AnimCue release `.jar` or `-sources.jar` artifacts.

## Included Examples

| Folder | Description | Key Features Covered |
| --- | --- | --- |
| [`duel_clash/`](duel_clash/) | Guardian dummy vs. player victim combat interaction | `interactive_animation:root` multi-turn flip (`360°`), `catmullrom` & `linear` bone curves, `bend` channel, authored `camera`, `timeline` markers, primary + participant `sound_effects` and `particle_effects` |
| [`door_interaction/`](door_interaction/) | Hinged door opening with a player actor stepping through | Static/block primary `door_leaf` swing, `actor` approach + walk cycle, `timeline` markers, `sound_effects` (`effect` + `locator`), shorthand `particle_effects` |

## How to Use in Your Own Mod

1. **Inspect or Edit in Blockbench (`5.0.0+`)**:
   - Install [`tools/blockbench/dist/animcue_studio.js`](../tools/blockbench/dist/animcue_studio.js) (or install **AnimCue Studio** from the Blockbench Plugin Store).
   - Open `duel_clash/duel_clash.bbmodel` or `door_interaction/door_interaction.bbmodel` directly in Blockbench.
2. **Copy into Your Mod Assets**:
   - Place the `.animation.json` file in your own mod's `src/main/resources/assets/<your_mod_id>/animations/` directory.
   - Register it once during common setup with `InteractiveScenes.register(YourMod.class, "<your_mod_id>:animations/<file>.animation.json")` and trigger it via `InteractiveScenes.play(...)` or `ClientScenes.playLocal(...)`.
