# Building & Multi-Version Architecture

[Wiki Home](../Home.md) | [简体中文](../zh/构建与多版本架构.md)

## Repository Layout (KltytonTemple)

AnimCue uses a shared-common, multi-target Gradle architecture:

```text
AnimCue/
├── common/
│   └── src/
│       ├── main/       # Loader- and version-independent API, asset parser, curves, server/client scene logic
│       └── compat/     # Version & loader bridges selected per target:
│           ├── minecraft/
│           │   ├── legacy/          # Direct rendering & GeckoLib 4 bridges (1.20.1, 1.21.1)
│           │   ├── modern/          # RenderState submission & GeckoLib 5 / KBR bridges (26.1.2, 26.2)
│           │   ├── registry_buffer/ # RegistryFriendlyByteBuf item codecs (1.21.1+)
│           │   ├── pal/             # PAL & PAL More Rotation layer bridges (1.21.1, 26.1.2, 26.2)
│           │   ├── 1.20.1/          # 1.20.1-specific mixins & preview helpers
│           │   ├── 1.21.1/          # 1.21.1-specific networking & PAL helpers
│           │   ├── 26.1.2/          # 26.1.2-specific model feature mixins
│           │   └── 26.2/            # 26.2-specific model feature mixins
│           ├── fabric/              # Fabric entrypoints (1.20.1, 1.21.1, modern)
│           ├── forge/               # Forge 1.20.1 entrypoints
│           └── neoforge/            # NeoForge entrypoints (1.21.1, modern)
├── targets/
│   ├── fabric-1.20.1/
│   ├── forge-1.20.1/
│   ├── fabric-1.21.1/
│   ├── neoforge-1.21.1/
│   ├── fabric-26.1.2/
│   ├── neoforge-26.1.2/
│   ├── fabric-26.2/
│   └── neoforge-26.2/
├── examples/           # Standalone Blockbench (.bbmodel) & animation examples (never packaged into JARs)
├── docs/wiki/          # Bilingual developer Wiki (EN & ZH)
└── tools/blockbench/   # Blockbench authoring workspace reference
```

---

## Building Any Target

Run the Gradle wrapper from the target directory you want to build:

```bash
cd targets/neoforge-1.21.1
../../gradlew build verifyDistribution
```

Built distribution artifacts are written to:
`targets/<loader>-<mc>/build/libs/interactive_animation-<loader>-<mc>-<version>.jar`

### Optional Development Properties

- `-PanimationCompatibility=true`: Includes supported PAL and PAL More Rotation dependencies in the development runtime for targets that support PAL (`1.21.1`, `26.1.2`, `26.2`).
- Dependency declarations in release metadata use compatible version ranges (no hard-pinned single release versions) and do not embed third-party dependencies via Jar-in-Jar.

---

## License

AnimCue is released under the **GNU Lesser General Public License v3.0 or later** (`LGPL-3.0-or-later`). See [LICENSE](../../../LICENSE) and [COPYING](../../../COPYING).
