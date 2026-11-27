# AnimCue Wiki

[English](Home.md) | [简体中文](Home_zh.md) · [Back to README](../../README.md)

Welcome to the official documentation for **AnimCue** (`interactive_animation`), a multi-version, multi-loader interactive animation runtime for Minecraft.

## Documentation Pages

1. **[Getting Started](en/Getting-Started.md)**
   Supported Minecraft/loader targets, adding AnimCue as a dependency, registering animation files, and playing your first multi-actor scene.
2. **[Animation Format (`interactive_animation:interactions`)](en/Animation-Format.md)**
   Complete specification of `interactive_animation:interactions`, participant definitions, `interactive_animation:root` transforms, bone channels (`position`, `rotation`, `scale`, `bend`), Catmull-Rom splines, `camera` tracks, `timeline` markers, `sound_effects`, and `particle_effects`.
3. **[Playback & Binding API](en/Playback-API.md)**
   Reference for `InteractiveScenes`, `SceneId`, `SceneSubject`, `GeckoSubjects`, `Bindings`, `PlaybackOptions` (including sound/particle handlers and `SceneEffects`), `SceneHandle`, `AlignmentResult`, and `AnimationControls`.
4. **[Client Rendering & Camera](en/Client-Rendering-and-Camera.md)**
   Reference for `ClientScenes`, `ClientAssets`, custom preview models, camera modes (`AUTO`, `VANILLA`, `FOLLOW`, `LOCKED`), player outer skin layers, PAL / PAL More Rotation bending, and GeckoLib / KBR hooks.
5. **[Example Projects & Reference Assets](en/Examples.md)**
   Ready-to-open Blockbench `.bbmodel` projects and exported `.animation.json` examples under `examples/` (excluded from library `.jar` artifacts).
6. **[Building & Multi-Version Architecture](en/Building-and-Architecture.md)**
   Repository layout (`common/src/main`, `common/src/compat`, `targets/*`, `examples/`), building all 8 loader/version targets, optional compatibility properties, and licensing.

## Core Design Principles

- **Single-File Source of Truth**: Both primary and participant animations live inside one standard Bedrock animation JSON file—no split runtime manifests required.
- **Server-Authoritative Multiplayer**: Server playback drives real entity movement, alignment checks, collision sweeps, and control leases while clients smoothly interpolate visuals and bone transforms.
- **Non-Intrusive Integration**: Existing GeckoLib controllers, PAL animation layers, and vanilla entity/block/item renderers continue to work normally outside and alongside interactive scenes.
