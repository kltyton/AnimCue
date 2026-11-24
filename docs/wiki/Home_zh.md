# 合拍（AnimCue）Wiki

[English](Home.md) | [简体中文](Home_zh.md) · [返回 README](../../README_zh.md)

欢迎阅读 **合拍（AnimCue）**（模组 ID：`interactive_animation`）官方开发文档。合拍是一个面向 Minecraft 实体、物品与方块的跨版本、跨加载器多参与者互动动画运行库。

## 文档目录

1. **[快速开始](zh/快速开始.md)**
   支持的 Minecraft 版本与加载器矩阵、前置依赖接入、资源注册以及第一个多角色互动场景示例。
2. **[动画格式规范（`interactive_animation:interactions`）](zh/动画格式规范.md)**
   完整说明 `interactive_animation:interactions` 顶层结构、参与者声明、`interactive_animation:root` 坐标约定、骨骼通道（`position` / `rotation` / `scale` / `bend`）、Catmull-Rom 样条、`camera` 轨道与 `timeline` 事件标记。
3. **[播放与绑定 API](zh/播放与绑定API.md)**
   详解 `InteractiveScenes`、`SceneId`、`SceneSubject`、`GeckoSubjects`、`Bindings`、`PlaybackOptions`、`SceneHandle`、`AlignmentResult` 与 `AnimationControls`。
4. **[客户端渲染与镜头](zh/客户端渲染与镜头.md)**
   详解 `ClientScenes`、`ClientAssets`、自定义预览模型、镜头模式（`AUTO` / `VANILLA` / `FOLLOW` / `LOCKED`）、玩家第二层皮肤同步、PAL / PAL More Rotation 弯曲以及 GeckoLib / KBR 渲染桥接。
5. **[构建与多版本架构](zh/构建与多版本架构.md)**
   工程分层（`common/src/main`、`common/src/compat`、`targets/*`）、8 个目标的构建方式、兼容参数与开源协议说明。

## 核心设计理念

- **单文件单一事实源**：主体动画与全部参与者轨道保存在同一个标准 Bedrock 动画 JSON 中，无需拆分额外的运行时清单。
- **服务端权威联机同步**：服务端负责真实实体位移、对位判定、碰撞扫描与控制权托管，客户端平滑插值骨骼姿态与镜头过渡。
- **非侵入式生态接入**：原生 GeckoLib 控制器、PAL 动画层以及原版实体/方块/物品渲染器在场景外及场景协同中均保持完整能力。
