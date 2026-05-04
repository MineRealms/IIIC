# Integrated Industrial Craft (IIC) - 项目指南

## 项目概述

**Mod名称**: Integrated Industrial Craft (IIC)  
**Mod ID**: `integratedindustrialcraft`  
**版本**: 1.0.0  
**Minecraft版本**: 1.20.1 Forge  
**开发语言**: Java 21

## 核心功能

IIC是一个工业整合mod，为GregTech整合包提供三大核心系统：

### 1. 三轴难度系统 (Tri-Axis Difficulty)
- **污染轴 (Pollution)**: 工业污染影响怪物强度
- **工业轴 (Industrial)**: 机器电压等级影响怪物强度
- **时间轴 (Time)**: 游戏时间推进影响怪物强度

### 2. 污染系统 (Pollution System)
- **临时污染**: 机器运行产生，可自然衰减和环境净化
- **永久污染**: 临时污染超过阈值转化，影响游戏阶段
- **环境净化**: 草方块、树叶、水等自然方块吸收污染

### 3. 炮塔系统 (Turret System)
- Mekanism激光炮塔集成
- 基于污染和工业等级的自动防御

---

## Mod集成 (Integration)

### 必需依赖
- **GregTech CEu Modern** (`gtceu`) - 主要集成对象
- **LDLib** - GT依赖库
- **Mekanism** - 炮塔系统

### 可选集成
- **Spore** (`spore`) - 怪物强化系统
  - API: `SporeIntegration.buffSporeMob()`
  - 异步扫描: `SporeAsyncWorker.processSporeBuffAsync()`
  
- **EnhancedVisuals** (`enhancedvisuals`) - 污染视觉效果
  - API: `EnhancedVisualsHelper.triggerLightEffects()` (轻度污染)
  - API: `EnhancedVisualsHelper.triggerModerateEffects()` (中度污染)
  - API: `EnhancedVisualsHelper.triggerHeavyEffects()` (重度污染)
  - API: `EnhancedVisualsHelper.triggerSevereEffects()` (严重污染)
  
- **Xaero's World Map** (`xaeroworldmap`) - 污染地图叠加层
  - Mixin注入: `GuiMapMixin`
  - 渲染器: `PollutionOverlayRenderer`

---

## 主要API

### 污染管理 API
```java
// 获取区块污染
double pollution = PollutionManager.getTemporaryPollution(ChunkPos pos);
double permanent = PollutionManager.getPermanentPollution(ChunkPos pos);

// 添加污染
PollutionManager.addPollution(ServerLevel level, BlockPos pos, double amount);

// 清除污染缓存
PollutionOverlayAPI.clearCache();
```

### 难度系统 API
```java
// 获取区块难度
double difficulty = DifficultyProvider.getChunkDifficulty(ServerLevel level, ChunkPos pos);

// 扫描附近机器电压等级
double tier = MachineScanner.scanNearbyVoltageTier(ServerLevel level, BlockPos pos);
```

### 配置系统 API
```java
// 访问配置
TriAxisConfig.basePollutionPerSecond
TriAxisConfig.pollutionTierExponent
TriAxisConfig.multiblockPollutionMultiplier

// Debug日志
IndustrialLogger.setDebugEnabled(true);
IndustrialLogger.debug("message");
```

---

## 事件注册

### Forge事件总线
```java
@Mod.EventBusSubscriber(modid = "integratedindustrialcraft")
public class EventHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) { }
    
    @SubscribeEvent
    public static void onLivingSpawn(MobSpawnEvent.FinalizeSpawn event) { }
}
```

### Mixin注入
- **EnhancedVisuals**: `VisualManagerMixin` - 注入污染视觉效果
- **Xaero's World Map**: `GuiMapMixin` - 添加污染叠加层按钮
- **GregTech**: 通过反射访问机器数据，避免硬依赖

---

## 配置文件

### 主配置文件
**位置**: `config/integratedindustrialcraft.json`

**关键配置项**:
```json
{
  "basePollutionPerSecond": 0.21,
  "pollutionTierExponent": 1.3,
  "multiblockPollutionMultiplier": 2.0,
  "grassBlockAbsorption": 0.0015,
  "leavesAbsorption": 0.0025,
  "waterAbsorption": 0.0012,
  "pollutionConversionThreshold": 200.0,
  "pollutionConversionRate": 0.005
}
```

### 预设配置
- **PEACEFUL**: 低难度，适合休闲玩家
- **NORMAL**: 标准难度
- **HARD**: 高难度
- **INSANE**: 极限难度

---

## 数据存储

### 世界保存数据
- **污染数据**: `data/integratedindustrialcraft/pollution.dat`
- **难度缓存**: 内存缓存，基于ChunkPos

### 日志文件
- **主日志**: `logs/iic-debug.log` (需开启debug模式)
- **Mixin追踪**: `logs/iic-mixin-status.log`

---

## 性能优化

### 异步处理
- **机器扫描**: `SporeAsyncWorker` 使用线程池异步扫描
- **污染计算**: 每秒更新，避免每tick计算

### 缓存系统
- **难度缓存**: `TriAxisDifficultyManager` 使用ChunkPos缓存
- **污染缓存**: `PollutionOverlayAPI` 客户端缓存

### LOD优化
- **地图渲染**: 根据缩放级别过滤低污染区块
  - 远距离: 只显示污染 > 50
  - 极远距离: 只显示污染 > 100

---

## 开发工具

### Debug模式
```java
// 启用debug日志
IndustrialLogger.setDebugEnabled(true);

// 检查debug状态
if (IndustrialLogger.isDebugEnabled()) {
    IndustrialLogger.debug("Debug message");
}
```

### Mixin追踪
```java
// 标记Mixin已加载
MixinLoadTracker.markLoaded("MixinName");

// 标记Mixin已应用
MixinLoadTracker.markApplied("MixinName");

// 检查Mixin状态
boolean applied = MixinLoadTracker.isApplied("MixinName");
```

---

## 常见问题

### Q: 如何调整污染产生速率？
A: 修改 `basePollutionPerSecond` 和 `pollutionTierExponent` 配置项。

### Q: 如何禁用某个集成？
A: 移除对应的mod即可，IIC会自动检测并禁用相关功能。

### Q: 污染地图叠加层不显示？
A: 确保安装了Xaero's World Map，并检查Mixin是否成功加载（查看 `logs/iic-mixin-status.log`）。

### Q: Java版本错误？
A: IIC需要Java 21编译，但可以在Java 17+运行。如遇到版本问题，请升级游戏使用的Java版本。

---

## 技术架构

### 核心模块
```
cn.minerealms.iic
├── difficulty/          # 难度系统
│   ├── DifficultyProvider
│   ├── MachineScanner
│   └── TriAxisDifficultyManager
├── pollution/           # 污染系统
│   ├── PollutionManager
│   └── visual/          # 视觉效果
├── integration/         # Mod集成
│   ├── gregtech/
│   ├── spore/
│   └── enhancedvisuals/
├── turrets/             # 炮塔系统
├── industrial/          # 工业系统
│   ├── IndustrialLogger
│   └── TriAxisConfig
└── mixin/               # Mixin注入
    ├── enhancedvisuals/
    └── xaeromap/
```

### 数据流
```
机器运行 → 污染产生 → 临时污染累积 → 永久污染转化
                ↓
        环境净化 + 自然衰减
                ↓
        影响怪物强度 + 视觉效果
```

---

## 许可证

本项目遵循 MIT 许可证。

## 贡献者

- **CARIERX** - 主要开发者
- **Claude Opus 4.6** - AI辅助开发

---

**最后更新**: 2026-05-04
