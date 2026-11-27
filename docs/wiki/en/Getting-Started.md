# Getting Started

[Wiki Home](../Home.md) | [简体中文](../zh/快速开始.md)

## Mod Identifiers & Compatibility

- **Mod ID**: `interactive_animation`
- **Root Package**: `com.kltyton.interactiveanimation`
- **License**: `LGPL-3.0-or-later`

### Target Matrix

| Target Directory | Minecraft | Loader |
| --- | --- | --- |
| `targets/fabric-26.2` | 26.2 | Fabric |
| `targets/neoforge-26.2` | 26.2 | NeoForge |
| `targets/fabric-26.1.2` | 26.1.2 | Fabric |
| `targets/neoforge-26.1.2` | 26.1.2 | NeoForge |
| `targets/fabric-1.21.1` | 1.21.1 | Fabric |
| `targets/neoforge-1.21.1` | 1.21.1 | NeoForge |
| `targets/fabric-1.20.1` | 1.20.1 | Fabric |
| `targets/forge-1.20.1` | 1.20.1 | Forge |

## Declaring AnimCue as a Dependency

### Gradle (Sighs Maven)

```groovy
repositories {
    maven {
        name = "Sighs Maven"
        url = uri("https://maven.sighs.cc/repository/maven-public/")
    }
}

dependencies {
    // Examples: interactive_animation-neoforge-1.21.1, interactive_animation-fabric-1.21.1,
    //           interactive_animation-forge-1.20.1, interactive_animation-fabric-26.2
    implementation "com.kltyton:interactive_animation-<loader>-<mc>:0.1.2"
}
```

### Mod Metadata

Install the AnimCue JAR matching your Minecraft version and loader alongside your mod. Declare a compatible version range in your mod metadata:

**NeoForge (`META-INF/neoforge.mods.toml`)**:
```toml
[[dependencies.your_mod]]
modId = "interactive_animation"
type = "required"
versionRange = "[0.1.2,)"
ordering = "AFTER"
side = "BOTH"
```

**Forge 1.20.1 (`META-INF/mods.toml`)**:
```toml
[[dependencies.your_mod]]
modId = "interactive_animation"
mandatory = true
versionRange = "[0.1.2,)"
ordering = "AFTER"
side = "BOTH"
```

**Fabric (`fabric.mod.json`)**:
```json
"depends": {
  "interactive_animation": ">=0.1.2"
}
```

## Minimal End-to-End Example

### 1. Place the Animation Asset

Save your Bedrock animation JSON under your mod's client assets directory, for example:
`assets/example/animations/grab.animation.json`

```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.example.grab": {
      "animation_length": 1.2,
      "bones": {},
      "timeline": {
        "0.25": "hit",
        "0.85": "release"
      }
    }
  },
  "interactive_animation:interactions": {
    "animation.example.grab": {
      "participants": [
        {
          "role": "target",
          "type": "entity",
          "id": "minecraft:player",
          "bones": {
            "interactive_animation:root": {
              "position": { "0.0": [0, 0, -16], "0.6": [0, 12, -16], "1.2": [0, 0, -32] }
            },
            "head": {
              "rotation": { "0.0": [0, 0, 0], "0.4": [15, 0, 0], "1.2": [0, 0, 0] }
            }
          }
        }
      ]
    }
  }
}
```

### 2. Register the Asset on Startup

Call `InteractiveScenes.register` during common mod initialization so the server reads the packaged JSON directly from your mod JAR:

```java
InteractiveScenes.register(ExampleMod.class, "example:animations/grab.animation.json");
```

On the client, AnimCue automatically discovers and reloads animation JSON files containing `interactive_animation:interactions` through the resource manager.

### 3. Play the Scene on the Server

```java
var sceneId = new SceneId("example:animations/grab.animation.json", "animation.example.grab");
var subject = SceneSubject.entity(bossEntity);
var bindings = Bindings.named(Map.of("target", targetPlayer));
var options = PlaybackOptions.DEFAULT
        .markers(marker -> {
            if ("release".equals(marker.instruction())) {
                marker.playback().useWorldCoordinates("target");
            }
        });

SceneHandle handle = InteractiveScenes.play(subject, sceneId, bindings, options);
```

See [Animation Format](Animation-Format.md) and [Playback & Binding API](Playback-API.md) for complete reference details.
