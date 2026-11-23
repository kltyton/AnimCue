# 合拍 · AnimCue

运行库面向 Minecraft 26.2 和 26.1.2 Fabric／NeoForge、1.21.1 Fabric／NeoForge、1.20.1 Fabric／Forge。工程按 KltytonTemple 组织。共享场景逻辑位于 `common/src/main`，渲染、网络和动画 API 的版本差异位于 `common/src/compat`。

模组 ID 为 `interactive_animation`。本项目以 [LGPL-3.0-or-later](LICENSE) 发布。

用 [合拍创作工具](tools/blockbench/README.md) 在 Blockbench 的同一个时间轴编辑多个参与者，再导出一个运行端可读取的互动动画文件。

## 动画资源

把 `kltyton:interactions` 加到现有 GeckoLib、PAL 或 Bedrock 动画 JSON 的顶层，以主动画名关联参与者。主动画照常使用 `bones` 和 `timeline`。

```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.dragon.grab": {
      "animation_length": 1.2,
      "bones": {},
      "timeline": { "0.25": "grab", "0.6": "release" }
    }
  },
  "kltyton:interactions": {
    "animation.dragon.grab": {
      "participants": [
        {
          "role": "victim",
          "type": "entity",
          "id": "minecraft:player",
          "bones": {
            "kltyton:root": {
              "position": { "0.0": [0, 0, -16], "0.6": [0, 16, -16], "1.2": [0, 0, -80] },
              "rotation": { "0.6": [0, 0, 0], "1.2": [0, 0, 720] }
            },
            "head": { "rotation": { "0.25": [15, 0, 0] } }
          }
        }
      ]
    }
  }
}
```

- `kltyton:root` 表示整个参与者。位置用模型单位，16 单位等于一格。旋转用度，可以超过一圈。普通骨骼继续用 Bedrock 的通道与关键帧格式。可选的骨骼映射方向为「动画骨骼名 → 实际模型骨骼名」。
- `type` 决定缺省素材，可选 `entity`、`item`、`block`、`model`。缺省是 `model`，由客户端的 `ClientAssets.registerModel` 注册对应模型工厂。世界中的实际实体、物品栈或方块状态可以覆盖缺省素材。
- 可选 `camera` 与 `participants` 同级，直接写 Bedrock 的 `position` 和 `rotation`。`AUTO` 优先读这条镜头轨道，否则参与演出的玩家使用整体跟随。可以显式选择 `VANILLA`、`FOLLOW` 或 `LOCKED`。

## 播放

服务器通过 `InteractiveScenes.register(OwnerMod.class, "example:geckolib/animations/dragon.animation.json")` 注册其拥有的资源。客户端资源重载从现有动画目录读取同一文件。播放时用完整资源路径与动画名构造 `SceneId`。

```java
var id = new SceneId("example:geckolib/animations/dragon.animation.json", "animation.dragon.grab");
var subject = GeckoSubjects.entity(dragon, "skills", "grab", transitionTicks);
var options = PlaybackOptions.DEFAULT
        .alignment(true, true, 0.0625, result -> {
            if (!result.aligned()) playMissAnimation(result);
        })
        .markers(marker -> {
            if (marker.instruction().equals("release")) marker.playback().useWorldCoordinates("victim");
        });

InteractiveScenes.play(subject, id, Bindings.named(Map.of("victim", player)), options);
```

`InteractiveScenes.play(subject, id, player)` 按 JSON 数组顺序绑定。客户端使用 `ClientScenes.playLocal(subject, id)` 预览缺省素材，也可按名称或顺序绑定实例。联机播放绑定实际实体后才驱动真实位移。实体没有整体轨道时仍可播放关节轨道。

`GeckoSubjects` 提供实体、物品和方块实体入口。GeckoLib 5 也可传控制器以获取其过渡 tick。其他已有播放器可通过 `SceneSubject.playback` 提供启动、结束和过渡时间。抓取技能可在服务端确认命中时绑定目标，让前摇阶段的玩家仍可躲避。

默认允许无过渡时直接对位，路径检查默认关闭。对位选项独立于攻击、交互和镜头。驱动实际实体时默认锁定其 AI 或玩家移动输入。主体 AI 可单独暂停，模组也可注册 `AnimationControls.Strategy` 适配自定义控制器。控制权随播放释放，`SceneHandle.cancel()` 可提前结束。

## 版本层

- GeckoLib 4 和 5 分别在原生控制器取样后应用参与者通道。KBR 增强的 GeckoLib 模型走同一个渲染入口。
- 26.x 的 KBR 附加包实体渲染器存在时，互动通道也会在原生姿势合成后应用到主体和额外几何 pass。开发环境使用各目标配置的 GeckoLib。`-PanimationCompatibility=true` 可加载受支持版本的 PAL 和 More Rotation。
- 带有 PAL 的目标通过 PAL 动画层应用玩家姿势。有 MoreRotation 时适配多轴弯曲。1.20.1 的玩家使用原版模型通道。
- `minecraft/modern` 用于 26.x 的渲染提交，`minecraft/legacy` 用于 1.20.1 和 1.21.1 的直接渲染。`registry_buffer` 用于 1.21.1 起的物品编解码，`pal` 用于可安装 PAL 的三个 Minecraft 版本。
