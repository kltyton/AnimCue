# AnimCue Studio

Blockbench plugin for authoring **AnimCue** multi-actor animations. Works on Blockbench Desktop and Web (5.0.0+), with English and Chinese UI following Blockbench's language setting.

[中文说明](README_zh.md)

## Install

1. Open `File → Plugins` in Blockbench.
2. Click **Load Plugin from File** and pick [dist/animcue_studio.js](dist/animcue_studio.js).
3. If you animate players with elbow/knee/torso bends, also install **PAL Bend Player Tools** (`pal_bend_player_tools.js` 0.4.2+). Both plugins link up automatically.

## Usage

1. Open your main model (`.bbmodel`), or create a blank scene from `File → New → AnimCue Scene`.
2. In the **AnimCue** panel, click **+** to add actors from:
   - `.bbmodel` or Bedrock `.geo.json` (with optional `.png` texture)
   - Built-in **PAL Bend Player** or **PAL DragonCore Player** (when PAL Bend Player Tools is installed)
   - **Vanilla Entity** templates
   Single-texture projects automatically open a multi-texture copy so each actor keeps its own texture.
3. Double-click an actor (or click the move icon) to jump to Frame 0 with its root selected and drag it into place.
   - Keyframe the actor root (`kltyton:root`) for whole-actor movement and turning.
   - Keyframe child bones (and `*_bend` helpers on PAL rigs) for limb motion.
   - Use the **⋮** menu on each actor to reorder bindings, import an existing PAL / Emote / Bedrock animation onto that actor, duplicate, or remove.
4. Click the camera icon to add a scene camera (`kltyton:interactions.<anim>.camera`) and preview through it at any frame. Click the flag icon to insert a `timeline` event marker.
5. Click the export icon in the panel (or `File → Export → AnimCue Animation...`) to save a single `.animation.json`. Use the import icon (or drag an AnimCue `.animation.json` into Blockbench) to load it back for editing.

## Build

```bash
node build.cjs
```
