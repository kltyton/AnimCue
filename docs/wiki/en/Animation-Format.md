# Animation Format (`interactive_animation:interactions`)

[Wiki Home](../Home.md) | [简体中文](../zh/动画格式规范.md)

AnimCue extends standard Bedrock / GeckoLib / PAL animation JSON files (`format_version: "1.8.0"`) by adding a single top-level `interactive_animation:interactions` object. Files without `interactive_animation:interactions` are ignored by AnimCue.

## Top-Level Structure

```json
{
  "format_version": "1.8.0",
  "animations": {
    "<animation_name>": {
      "animation_length": 2.0,
      "bones": { ... },
      "timeline": {
        "0.25": "marker_a",
        "1.00": ["marker_b", "marker_c"]
      }
    }
  },
  "interactive_animation:interactions": {
    "<animation_name>": {
      "participants": [ ... ],
      "camera": { ... }
    }
  }
}
```

### Validation Rules (`SceneReader`)

- Every key inside `interactive_animation:interactions` must match an existing animation key inside `animations`.
- Each scene must declare between **1 and 64** entries in `participants`.
- Every participant must have a unique, non-empty `role` string.
- If `animation_length` is present on the primary animation, no bone track, camera track, `timeline` marker, `sound_effects` keyframe, or `particle_effects` keyframe may exceed `animation_length` (tolerance `1e-5`). If omitted, the scene length is computed from the last keyframe or marker timestamp.

---

## Participants

Each object in `participants` supports:

| Field | Required | Type | Description |
| --- | :---: | --- | --- |
| `role` | Yes | `string` | Unique role name used by `Bindings.named(...)`, `SceneHandle`, and `ClientScenes.subjectFor(...)`. |
| `type` | No | `string` | Default preview asset type: `entity`, `item`, `block`, or `model` (default: `model`). Case-insensitive. |
| `id` | Yes | `string` | Resource identifier for the default asset (e.g. `minecraft:player`, `minecraft:iron_sword`, or a custom ID registered via `ClientAssets.registerModel`). |
| `bones` | No | `object` | Map of bone name → channel tracks (`position`, `rotation`, `scale`, `bend`). |
| `sound_effects` | No | `object` | Timed sound keyframes using the same format as primary `sound_effects` (dispatched with `role` set to the participant's role). |
| `particle_effects` | No | `object` | Timed particle keyframes using the same format as primary `particle_effects` (dispatched with `role` set to the participant's role). |

Live instances passed through `Bindings` override the default preview asset at playback time.

---

## `interactive_animation:root` Whole-Actor Track

The reserved bone name `interactive_animation:root` drives whole-participant spatial displacement and rotation relative to the primary `SceneSubject.Frame`:

- **`position`**: Model units (**16 units = 1 Minecraft block**), transformed by the subject's `yaw`.
- **`rotation`**: Euler angles in **degrees**. Rotation values are not clamped to `[-180, 180]`, so multi-turn spins (e.g. `0` → `720`) retain their authored winding.
- **`scale`**: Optional scale vector (default `[1, 1, 1]`).

Participants without a `interactive_animation:root` track still apply their skeletal bone tracks without moving their world entity position.

---

## Bone Channels & Interpolation (`BoneTrack` / `AnimationCurve`)

Each bone entry under `bones` may declare any combination of four channels:

- `position` (default `[0, 0, 0]`)
- `rotation` (default `[0, 0, 0]`)
- `scale` (default `[1, 1, 1]`)
- `bend` (default `[0, 0, 0]`, used for limb/torso bend adapters such as PAL More Rotation or legacy bend bridges)

### Keyframe Formats

Each channel accepts:
- A static 3-element numeric array `[x, y, z]` or scalar number (expanded to `[v, v, v]`),
- A single keyframe object with `pre` / `post`, or
- A timestamp map (`"0.0"`, `"0.25"`, ...) where every timestamp is a finite non-negative number in seconds.

Individual keyframes support discontinuous `pre` / `post` values and `lerp_mode`:

```json
"left_leg": {
  "rotation": {
    "0.0": [-30, 0, 0],
    "0.25": {
      "post": [28, 0, 0],
      "lerp_mode": "catmullrom"
    },
    "0.5": [-30, 0, 0]
  },
  "bend": {
    "0.0": [18, 0, 0],
    "0.25": {
      "post": [64, 0, 0],
      "lerp_mode": "catmullrom"
    },
    "0.5": [18, 0, 0]
  }
}
```

Supported `lerp_mode` values:
- `"linear"` (default): linear interpolation between `previous.post` and `next.pre`.
- `"catmullrom"`: Catmull-Rom cubic spline interpolation across neighboring keyframes.

All curve components must be finite numbers (Molang string expressions are not supported inside `interactive_animation:interactions` tracks).

---

## Camera Track

An optional `camera` object may be placed alongside `participants` using the same channel structure (`position` and `rotation` in Bedrock coordinates/degrees):

```json
"interactive_animation:interactions": {
  "animation.example.intro": {
    "participants": [ ... ],
    "camera": {
      "position": { "0.0": [0, 28, -48], "1.5": [16, 24, -40] },
      "rotation": { "0.0": [12, 0, 0], "1.5": [8, -20, 0] }
    }
  }
}
```

When `PlaybackOptions.CameraMode.AUTO` is selected, AnimCue drives the camera from this track when present, or falls back to smooth follow camera for participating players.

---

## Timeline, Sound, and Particle Keyframes

The primary animation's `timeline` map fires `PlaybackOptions.Marker` callbacks at the corresponding playback timestamp. Each timestamp may contain either a single string instruction or an array of string instructions:

```json
"timeline": {
  "0.16": "door_open",
  "0.85": ["settle", "mount"]
}
```

Standard Bedrock / GeckoLib `sound_effects` and `particle_effects` keyframes are also supported on both the primary animation and individual entries in `participants` (accepting an object, array, or plain `effect` ID string per timestamp):

```json
"sound_effects": {
  "0.25": {
    "effect": "minecraft:entity.experience_orb.pickup",
    "locator": "mouth"
  },
  "0.80": "minecraft:block.anvil.land"
},
"particle_effects": {
  "0.40": {
    "effect": "minecraft:crit",
    "locator": "right_hand",
    "pre_effect_script": "slash"
  }
}
```

By default, registered sound events and simple particle types play automatically at the owner's position for participant tracks and non-native primary tracks (`nativePlayback = false`), while native controllers (such as GeckoLib via `GeckoSubjects`) keep owning primary sound/particle keyframes to avoid duplicate firing. When Kltyton Bedrock Runtime (`kltyton_bedrock_runtime`, Lite or full) is installed alongside AnimCue, default particle keyframes also automatically dispatch KBR `particle_definitions/*.json` billboard and 3D model particles with `pre_effect_script` / `script` MoLang initialization expressions.
