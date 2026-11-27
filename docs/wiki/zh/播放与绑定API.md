# 播放与绑定 API

[Wiki 首页](../Home_zh.md) | [English](../en/Playback-API.md)

合拍的公共服务端/通用 API 位于 `com.kltyton.interactiveanimation.api`，GeckoLib 便捷入口位于 `com.kltyton.interactiveanimation.compat.geckolib`。

---

## `InteractiveScenes`

负责注册服务端资源文件并启动联机同步场景：

```java
InteractiveScenes.register(Class<?> resourceOwner, String animationFile);

SceneHandle InteractiveScenes.play(SceneSubject subject, SceneId scene, Object... participants);
SceneHandle InteractiveScenes.play(SceneSubject subject, SceneId scene, Bindings bindings, PlaybackOptions options);
```

- `animationFile` 必须是以 `.json` 结尾的完整资源路径，例如 `"mymod:animations/duel.animation.json"`。
- 客户端在资源重载时会自动读取同一资源路径。

---

## `SceneId`

通过完整文件路径与动画名唯一确定一条互动场景：

```java
var id = new SceneId("mymod:animations/duel.animation.json", "animation.duel.clash");
```

---

## `SceneSubject` 与 `GeckoSubjects`

`SceneSubject` 描述主动画的拥有者，提供每帧空间基准 `Frame(position, yaw)`、存活校验、关联实体及主控制器回调（`play` / `stop` / `transitionTicks`）。

### 内置工厂方法

```java
SceneSubject.entity(Entity entity)
SceneSubject.block(BlockEntity block, float yaw)
SceneSubject.item(ItemStack stack, Entity holder)
```

对接自定义主控制器时使用 `playback(...)`：

```java
SceneSubject subject = SceneSubject.entity(boss)
        .playback(transitionTicks, () -> startNativeAnim(), () -> stopNativeAnim());
```

对于需要自定义运动轨迹的场景（如脚本化的载具登乘路径），可直接构造 `new SceneSubject(level, frameSupplier, validSupplier, entity, transitionTicks, nativePlayback, play, stop)`。

### GeckoLib 便捷入口（`GeckoSubjects`）

```java
// 实体（GeckoLib 4 与 5 通用）
GeckoSubjects.entity(geoEntity, "controllerName", "triggerName", serverTransitionTicks);

// 实体直接传入控制器获取过渡 tick（GeckoLib 5）
GeckoSubjects.entity(geoEntity, controller, "triggerName");

// 方块实体与物品
GeckoSubjects.block(geoBlockEntity, yaw, "controllerName", "triggerName", transitionTicks);
GeckoSubjects.item(stack, holder, instanceId, "controllerName", "triggerName", transitionTicks);
```

---

## `Bindings`

将运行时的 `Entity`、`ItemStack` 或 `BlockState` 绑定到参与者角色，并支持按角色配置骨骼重映射：

```java
Bindings.defaults()                          // 仅使用 JSON 中的缺省预览素材
Bindings.ordered(entityA, entityB)           // 按 participants 数组顺序依次绑定
Bindings.named(Map.of("rider", player))      // 按角色名精确绑定
```

### 骨骼映射

无需修改动画文件即可将动画中的骨骼名映射到目标模型的实际骨骼名：

```java
var bindings = Bindings.named(Map.of("hero", player))
        .mapBones("hero", Map.of("torso", "body"));
```

映射方向为：**动画骨骼名 → 实际模型骨骼名**。

---

## `PlaybackOptions`

基于 `PlaybackOptions.DEFAULT` 链式构造的不可变播放配置：

| 配置方法 | 默认值 | 说明 |
| --- | --- | --- |
| `alignment(snap, checkPath, tolerance, handler)` | `snap=true`, `checkPath=false`, `tolerance=0.0625` | 控制无过渡直对位、接近路径检查、对位容差（单位：格）及对位结果回调 `Consumer<AlignmentResult>`。 |
| `controls(lockDriven, subjectAi, attack, interaction)` | `lockDriven=true`，其余 `false` | 控制是否锁定受驱动参与者的移动/AI，以及是否暂停主体 AI、攻击与交互输入。 |
| `camera(CameraMode)` | `CameraMode.AUTO` | 可选 `AUTO`、`VANILLA`、`FOLLOW`、`LOCKED`。 |
| `carrier(Entity)` | `null` | 指定可选的运动载体实体。 |
| `markers(Consumer<Marker>)` | 空回调 | 接收主动画 `timeline` 触发的 `Marker(playback, instruction, seconds)` 事件。 |
| `sounds(Consumer<SoundKeyframe>)` | 自动播放（非原生主轨／参与者轨） | 拦截 `sound_effects` 触发的 `SoundKeyframe(playback, role, effect, locator, seconds, position)`（传入 `ignored -> {}` 可禁用自动播放）。 |
| `particles(Consumer<ParticleKeyframe>)` | 自动播放（非原生主轨／参与者轨） | 拦截 `particle_effects` 触发的 `ParticleKeyframe(playback, role, effect, locator, script, seconds, position)`。 |

### `SceneEffects`

可在自定义 `.sounds(...)` / `.particles(...)` 回调中复用合拍内置的注册表声音播放与粒子生成逻辑：

```java
SceneEffects.playSound(level, soundKeyframe);
SceneEffects.playSound(level, sourceEntity, position, "minecraft:block.anvil.land", 1.0F, 1.0F);
SceneEffects.spawnParticle(level, particleKeyframe);
SceneEffects.spawnParticle(level, position, "minecraft:crit");
SceneEffects.spawnParticle(level, position, "mymod:custom_kbr_fx", "v.size=0.75;");
SceneEffects.spawnParticle(level, sourceEntity, position, "mymod:custom_kbr_fx", "v.size=0.75;");
```

未注册/非法 ID 以及无法直接作为 `ParticleOptions` 实例化的参数化粒子会安全返回 `false` 而不抛异常；当 `keyframe.playback()` 为 `null` 时，`playSound(level, keyframe)` 会跳过实体绑定查找并使用 `SoundSource.NEUTRAL` 在 `keyframe.position()` 处播放。

当运行环境同时安装了 **Kltyton Bedrock Runtime**（`kltyton_bedrock_runtime`，兼容 Lite 与完整版）时，`SceneEffects.spawnParticle`（以及参与者轨与非原生 `nativePlayback = false` 主轨的默认粒子关键帧路径）在原版 `PARTICLE_TYPE` 未命中时会自动软兼容回退至 KBR `particle_definitions/*.json` 的暴雪 Billboard 与 3D 模型粒子，并自动传入可选的 `script` MoLang 初始化表达式；未安装 KBR 时零依赖、零报错，未知或仅 KBR 可用的粒子 ID 仍安全返回 `false`。

### `AlignmentResult`

```java
record AlignmentResult(String role, Status status, Vec3 initial, Vec3 target, Vec3 reached)
```

`Status` 枚举包括：`ALIGNED`、`MISALIGNED`、`BLOCKED`、`CANCELLED`。提供 `aligned()`、`initialOffset()`、`remainingOffset()` 辅助方法。

---

## `SceneHandle`

由 `InteractiveScenes.play(...)` 与 `ClientScenes.playLocal(...)` 返回的播放句柄：

- `State state()` — 返回 `APPROACHING`、`PLAYING`、`FINISHED` 或 `CANCELLED`。
- `Map<String, AlignmentResult> alignmentResults()` — 查询各角色对位结果。
- `Object binding(String role)` — 获取角色当前绑定的运行时实例。
- `void cancel()` — 立即终止场景并释放全部控制权租约。
- `void useWorldCoordinates(String role)` — 将指定参与者从主锚点局部坐标切换为世界坐标（适合在 `timeline` 标记中触发投掷/脱手）。
- `void follow(String role, Entity carrier)` — 切换指定参与者跟随特定载体实体。

---

## `AnimationControls`

挂载在实体实例上的引用计数式控制权托管（涵盖 `ai`、`movement`、`attacks`、`interaction`）。多个重叠场景的控制项会自动合并，直到最后一个租约释放才恢复实体原状态。

带有自定义 AI 或状态机的实体模组可通过 `AnimationControls.register` 注册适配策略：

```java
AnimationControls.register(MyModEntities.CUSTOM_BOSS.get(), (entity, controls) -> {
    var boss = (CustomBossEntity) entity;
    if (controls.ai()) boss.setScriptPaused(true);
    return () -> {
        if (controls.ai()) boss.setScriptPaused(false);
    };
});
```
