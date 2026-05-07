# I3C - Improved Integrated Industrial Craft 技术实现文档

<div align="center">

**基于工业进度的动态难度缩放模组**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.1.3-orange.svg)](https://files.minecraftforge.net/)

</div>

---

## 目录

1. [项目概述](#1-项目概述)
2. [核心架构](#2-核心架构)
3. [三轴难度系统](#3-三轴难度系统)
4. [污染系统](#4-污染系统)
5. [威胁系统](#5-威胁系统)
6. [集成模组](#6-集成模组)
7. [炮塔系统](#7-炮塔系统)
8. [指令系统](#8-指令系统)
9. [配置系统](#9-配置系统)
10. [API 参考](#10-api-参考)

---

## 1. 项目概述

### 1.1 模组信息

| 属性 | 值 |
|------|-----|
| 模组ID | `integratedindustrialcraft` |
| 缩写 | I3C |
| 依赖 | ImprovedMobs 1.20.1 |
| Minecraft | 1.20.1 |
| Forge | 47.1.3 |
| 包名 | `cn.minerealms.iic` |

### 1.2 设计理念

I3C 模拟 Factorio 的污染与难度系统理念，将工业进度与游戏难度紧密绑定：

- **机器越多 → 污染越高 → 难度越大**
- **电压越高 → 威胁越强 → 怪物越强**
- **时间越长 → 基础难度越高**

### 1.3 联动模组

```
┌─────────────────────────────────────────────────────────┐
│                    ImprovedMobs                         │
│                    (基础依赖)                            │
└─────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ GregTech CEu  │  │    Spore      │  │   Mekanism    │
│ 机器检测      │  │  虫巢集成     │  │  能量系统    │
│ 电压等级      │  │  进化反馈    │  │  激光炮塔    │
└───────────────┘  └───────────────┘  └───────────────┘
```

---

## 2. 核心架构

### 2.1 包结构

```
cn.minerealms.iic/
├── api/                    # 公共API接口
│   ├── I3CAPI.java         # 主API入口
│   └── PollutionOverlayAPI.java  # 污染覆盖API
├── industrial/             # 工业配置核心
│   ├── TriAxisConfig.java   # 三轴配置管理
│   └── IndustrialLogger.java  # 日志系统
├── difficulty/             # 难度系统
│   ├── DifficultyProvider.java  # DifficultyGetter实现
│   ├── TriAxisDifficultyManager.java  # 三轴计算
│   ├── MachineScanner.java   # 机器扫描
│   └── DifficultySmoother.java  # 平滑处理
├── pollution/              # 污染系统
│   ├── PollutionManager.java  # 污染管理
│   └── visual/            # 污染视觉效果
├── threat/                # 威胁系统
│   ├── ThreatManager.java  # 威胁触发
│   └── horde/            # 尸潮集成
├── integration/            # 模组集成
│   ├── gregtech/          # GT CEu集成
│   │   ├── GTIntegration.java  # 机器检测
│   │   └── GTPollutionScanner.java  # GT污染扫描
│   └── spore/            # Spore集成
│       ├── SporeIntegration.java  # 虫巢集成
│       └── HivemindProximityManager.java  # 虫巢距离
├── ai/                   # 增强AI
│   ├── ZombieDestroyMachineGoal.java  # 僵尸攻机器
│   └── CreeperTargetMachineGoal.java  # 苦力怕攻机器
├── command/               # 指令系统
│   ├── IICCommand.java   # 根指令
│   ├── PollutionCommand.java  # 污染指令
│   ├── DifficultyCommand.java  # 难度指令
│   └── ...
├── turrets/               # 炮塔系统
│   └── common/
│       ├── block_entity/  # 方块实体
│       ├── entity/       # 实体（激光）
│       └── registry/      # 注册
├── client/               # 客户端
│   └── hud/              # HUD显示
└── core/                 # 核心配置
    └── config/           # 配置类
```

### 2.2 入口类

```java
// IntegratedIndustrialCraft.java
@Mod(IntegratedIndustrialCraft.MODID)
public class IntegratedIndustrialCraft {
    public static final String MODID = "integratedindustrialcraft";
    
    // 在setup中注册:
    // 1. 网络包处理器
    // 2. 难度提供者 (如果ImprovedMobs加载)
    // 3. TriAxis配置
    // 4. 方块/实体/物品注册
}
```

---

## 3. 三轴难度系统

### 3.1 核心公式

```
D = Base(T) × Scale(V) × Pressure(P) × GlobalMultiplier
```

| 轴 | 名称 | 范围 | 描述 |
|----|------|------|------|
| T | Time | 0.5-2.0 | 基于游戏时间的基础难度 |
| V | Voltage | 1.0-4.5 | 基于电压等级的缩放系数 |
| P | Pollution | 1.0-4.0 | 基于污染的压力系数 |
| Global | 全局 | 2.0-3.5 | 最终乘数 |

### 3.2 时间轴 (Time Axis)

**公式**:
```
mcDaysPerRealDay = 24 × 3  // 1现实天 = 3 MC天
targetMCDays = realWorldDaysToMax × mcDaysPerRealDay
tRaw = log1p(mcDays / baseDays) / log1p(targetMCDays / baseDays)
T = clamp(tRaw, 0.0, 1.0)
Base = baseMin + (baseMax - baseMin) × T
```

**默认参数**:
- `baseMin = 0.5`
- `baseMax = 2.0`
- `baseDays = 30.0`
- `targetDays = 800-1200` (根据难度预设)
- `realWorldDaysToMax = 30` 天

**设计目标**: 30个现实天后达到满时间难度

### 3.3 电压轴 (Voltage Axis)

**公式**:
```
V = clamp(medianTier / effectiveMaxTier, 0.0, 1.0)
tierValue = V × maxTier
Scale = 1.0 + pow(tierValue, scaleExponent) × scaleMultiplier
```

**默认参数**:
- `scaleExponent = 1.2`
- `scaleMultiplier = 0.15`
- `maxIndustrialTier = 9` (UHV)

**电压等级表**:

| Tier | 电压 | 名称 | Scale (≈) |
|------|------|------|----------|
| 0 | 8 EU/t | ULV | 1.0 |
| 1 | 32 EU/t | LV | 1.1 |
| 2 | 128 EU/t | MV | 1.3 |
| 3 | 512 EU/t | HV | 1.6 |
| 4 | 2048 EU/t | EV | 2.0 |
| 5 | 8192 EU/t | IV | 2.5 |
| 6 | 32768 EU/t | LuV | 3.1 |
| 7 | 131072 EU/t | ZPM | 3.8 |
| 8 | 524288 EU/t | UV | 4.5 |
| 9 | 2097152 EU/t | UHV | 5.2+ |

### 3.4 污染轴 (Pollution Axis)

**公式**:
```
// EMA平滑
newEma = α × pollution + (1-α) × oldEma
P = newEma / pollutionDenominator

// Sigmoid曲线
sigmoid = 1 / (1 + exp(-P + sigmoidShift))
Pressure = pressureMin + (pressureMax - pressureMin) × sigmoid
```

**默认参数**:
- `emaAlpha = 0.05` (平滑系数)
- `pollutionDenominator = 800.0`
- `pressureMin = 1.0`
- `pressureMax = 4.0`
- `sigmoidShift = 2.0`

**曲线特性**:
- 污染=0时: Pressure = 1.0 (无影响)
- 污染=denominator/2时: Pressure ≈ 1.5
- 污染=denominator时: Pressure ≈ 2.5
- 污染=2×denominator时: Pressure ≈ 4.0

### 3.5 难度计算流程

```java
// TriAxisDifficultyManager.calculateLocalDifficulty()
public static DifficultyState calculateLocalDifficulty(Level level, BlockPos center) {
    // 1. 时间轴计算
    long mcDays = level.getDayTime() / 24000L;
    double T = calculateTimeFactor(mcDays);
    double Base = baseMin + (baseMax - baseMin) * T;
    
    // 2. 电压轴计算
    int medianTier = MachineScanner.scanNearbyVoltageTierMedian(level, center);
    double V = medianTier / effectiveMaxTier;
    double Scale = 1.0 + pow(V * maxTier, exponent) * multiplier;
    
    // 3. 污染轴计算
    double localPollution = PollutionManager.getTemporaryPollution(chunk);
    double globalPollution = PollutionManager.getPermanentPollution();
    double P = (localPollution + globalPollution) / denominator;
    double Pressure = calculatePressure(P);
    
    // 4. 乘法融合
    double D = Base * Scale * Pressure * globalMultiplier;
    
    // 5. 限速平滑
    D = applyRateLimiting(D, chunk);
    
    return new DifficultyState(D, T, V, P);
}
```

---

## 4. 污染系统

### 4.1 双层设计

```
┌─────────────────────────────────────────────────────────┐
│                    污染系统                             │
│  ┌─────────────────┐    ┌─────────────────┐          │
│  │  临时污染       │    │  永久污染       │          │
│  │  (Chunk-based) │───▶│  (Global)       │          │
│  └─────────────────┘    └─────────────────┘          │
│         │                       │                      │
│         ▼                       ▼                      │
│  ┌──────────────┐       ┌──────────────┐             │
│  │ 可被环境吸收 │       │ 直接增加    │             │
│  │ 树/水/草    │       │  难度       │             │
│  └──────────────┘       └──────────────┘             │
└─────────────────────────────────────────────────────────┘
```

### 4.2 污染生成公式

**机器污染**:
```
pollutionValue = basePollutionPerSecond × (tier + 1)^tierExponent

// 默认值
basePollutionPerSecond = 0.21
tierExponent = 1.3

// 实例: EV多方块 (tier=4)
= 0.21 × (4+1)^1.3 × 2.0 (multiblock)
= 0.21 × 8.55 × 2.0
= 3.59 /秒
// 24小时 = 310 污染
```

**污染表**:

| 电压 | 单方块 | 多方块 | 24h污染 |
|------|--------|-------|--------|---------|
| ULV | 0.21 | 0.42 | 18 |
| LV | 0.35 | 0.70 | 30 |
| MV | 0.55 | 1.10 | 48 |
| HV | 0.84 | 1.68 | 73 |
| EV | 1.24 | 2.48 | 107 |
| IV | 1.78 | 3.56 | 154 |
| LuV | 2.52 | 5.04 | 218 |
| ZPM | 3.50 | 7.00 | 302 |
| UV | 4.80 | 9.60 | 415 |
| UHV | 6.46 | 12.92 | 559 |

### 4.3 临时污染处理

**自然衰减** (比例衰减):
```
naturalDecay = currentPollution × naturalDecayRate
// naturalDecayRate = 0.002 (0.2%/秒)
```

**环境吸收**:
```
grassBlockAbsorption = 0.0015 /块/秒   (典型区块~200块 → 0.3/秒)
leavesAbsorption = 0.0025 /块/秒     (典型区块~150块 → 0.375/秒)
waterAbsorption = 0.0012 /块/秒     (典型区块~50块 → 0.06/秒)

// 典型自然区块吸收: ~0.735/秒
// 1台HV机器需要: 73 / 0.735 ≈ 100秒 ≈ 1.7分钟
```

**扩散**:
```
diffusionRate = 0.15
// 每秒15%污染扩散到相邻区块
```

### 4.4 临时→永久转化

```
if (currentPollution > tempToPermanentThreshold) {
    excess = currentPollution - threshold;
    converted = excess × tempToPermanentRate;
    addPermanentPollution(converted);
}

// 默认值
tempToPermanentThreshold = 200.0
tempToPermanentRate = 0.005 (0.5%/秒)
```

---

## 5. 威胁系统

### 5.1 威胁等级表

| 阶段 | 电压 | 污染阈值 | 威胁类型 |
|------|------|----------|---------|
| ULV-LV | 0-1 | - | 无威胁 |
| MV | 2 | ≥50 | 僵尸攻击机器 |
| HV | 3+ | ≥60 | 僵尸生成 |
| HV | 3+ | ≥100 | 苦力怕生成 |
| EV+ | 4+ | ≥180 | 闪电苦力怕 |

### 5.2 威胁触发条件

**僵尸攻击机器** (MV阶段):
```java
// ZombieDestroyMachineGoal
if (pollution >= 50 && medianTier >= 2) {
    // 僵尸寻找16格内GT机器
    // 靠近后破坏方块
}
```

**主动生成** (HV+阶段):
```java
// ThreatManager.checkAndTriggerThreats()
double zombieRate = baseZombieRate * (1 + pollution / divisor);
if (pollution >= hvZombieThreshold && random.nextDouble() < zombieRate) {
    spawnZombieWave(size);
}

double creeperRate = baseCreeperRate * (1 + pollution / divisor);
if (pollution >= hvCreeperThreshold && random.nextDouble() < creeperRate) {
    spawnCreeperWave(size);
    if (pollution >= chargedThreshold) {
        spawnChargedCreeper();
    }
}
```

**默认概率**:
- `zombieSpawnChance = 0.015` (1.5%/秒)
- `creeperSpawnChance = 0.008` (0.8%/秒)
- `chargedCreeperChance = 0.0015` (0.15%/秒)

### 5.3 波次大小计算

```java
waveSize = baseSize + pollution/divisor + tier/divisor
// waveBaseSize = 3
// wavePollutionDivisor = 50  (每50污染+1)
// waveTierDivisor = 2    (每2等级+1)
// waveMaxSize = 15
```

---

## 6. 集成模组

### 6.1 GregTech CEu Modern

**检测方式**: 反射+运行时检测
```java
// GTIntegration.java
Class.forName("com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity");
Class.forName("com.gregtechceu.gtceu.api.capability.IWorkable");
Class.forName("com.gregtechceu.gtceu.api.capability.IEnergyContainer");
```

**机器检测**:
- `isGTMachine(be)`: 检查是否为MetaMachineBlockEntity
- `getVoltageTier(be)`: 获取电压等级
  - 优先: 配方EU/t
  - 次优: 多方块能量仓
  - 末优: 机器定义
- `hasEnergyOrActive(be)`: 检查是否有能量或工作中
- `isMultiblock(be)`: 检查是否多方块

### 6.2 Spore (真菌孢子)

**检测方式**: 反射
```java
// SporeIntegration.java
Class.forName("com.Harbinger.Spore.ExtremelySusThings.SporeSavedData");
Class.forName("com.Harbinger.Spore.Sentities.Organoids.Proto");
```

**功能**:
- 获取虫巢数量/生物质/宿主
- 污染反馈: 污染加速虫巢生长
- 进化计算: 基于虫巢+生物质+污染+电压
- 生物增强: 污染/电压/进化阶段加成

**进化公式**:
```
evolution = (hivemindWeight × H/20) + 
            (biomassWeight × B/30000) + 
            (hostWeight × Ho/1000) + 
            (pollutionWeight × P/1500) + 
            (voltageWeight × T/9)

phase = min(10, evolution × 10)
```

### 6.3 Mekanism

**能量系统**: 使用Forge标准能量接口
```java
be.getCapability(ForgeCapabilities.ENERGY);
```

**炮塔能量**:
```
┌─────────────────────────────────────┐
│  激光炮塔 (4等级)                │
│  ┌─────────────────────────────┐   │
│  │ Basic    10000  EU        │   │
│  │ Advanced 25000  EU        │   │
│  │ Elite    60000  EU        │   │
│  │ Ultimate 150000 EU        │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 7. 炮塔系统

### 7.1 炮塔类型

| 类型 | 等级 | 伤害 | 射程 | 射速 | 能量 | 特殊 |
|------|------|------|------|------|------|------|
| 激光 | Basic | 10 | 16 | 10/t | 10000 | 单体 |
| 激光 | Advanced | 18 | 20 | 8/t | 25000 | 单体 |
| 激光 | Elite | 28 | 24 | 6/t | 60000 | 追踪 |
| 激光 | Ultimate | 45 | 30 | 4/t | 150000 | 追踪+穿甲 |
| 火焰 | Basic | 4 | 12 | 5/t | 燃料 | AOE |

### 7.2 激光渲染

```java
// LaserEntity.java
// 持续性激光束渲染
// 客户端每帧渲染线段
// 服务端计算命中与伤害
// 使用GeckoLib同步动画状态
```

### 7.3 能量管理

```java
// MTEnergyStorage.java
// 实现IEnergyStorage接口
// 存储/提供能量
// 射线检测消耗能量
```

---

## 8. 指令系统

### 8.1 根指令

```
/im <子指令>
```

### 8.2 污染指令

```
/im pollution add <amount> [pos]
/im pollution remove <amount> [pos]
/im pollution set <amount> [pos]
/im pollution clear [radius]
/im pollution permanent add <amount>
/im pollution permanent remove <amount>
/im pollution permanent set <amount>
/im pollution status [pos]
```

### 8.3 难度指令

```
/im difficulty [player]
/im difficulty set <value>
/im difficulty reset
```

### 8.4 配置指令

```
/im config preset <NORMAL|HARD|HARDCORE|INSANE|CUSTOM>
/im config reload
/im config get <key>
/im config set <key> <value>
```

### 8.5 其他指令

```
/im scan              # 扫描附近机器
/im scan <radius>     # 扫描指定半径
/im hud on|off      # HUD显示
/im debug on|off    # 调试日志
/im horde status    # 尸潮状态
/im horde trigger   # 触发尸潮
/im spawn zombie <count> [pos]
/im spawn creeper <count> [pos]
```

---

## 9. 配置系统

### 9.1 配置文件

**主配置**: `config/triaxis-difficulty.properties`

**炮塔配置**: `config/iic/turrets.toml`

### 9.2 难度预设

| 预设 | 全局乘数 | 污染阈值 | 时间曲线 | 威胁概率 |
|------|---------|---------|---------|----------|
| NORMAL | 2.0 | 150 | 800天 | 标准 |
| HARD | 2.5 | 150 | 1000天 | 提高 |
| HARDCORE | 2.8 | 150 | 1200天 | 高 |
| INSANE | 3.5 | 150 | 1500天 | 极限 |

### 9.3 关键参数

```properties
# 难度预设
difficultyPreset=NORMAL

# 难度目标
targetMaxDifficulty=250.0
realWorldDaysToMax=30

# 时间曲线
baseDays=30.0
targetDays=800.0

# 乘法模型
baseMin=0.5
baseMax=2.0
scaleExponent=1.2
scaleMultiplier=0.15
pressureMin=1.0
pressureMax=4.0
sigmoidShift=2.0
globalMultiplier=2.0

# 污染系统
basePollutionPerSecond=0.21
pollutionTierExponent=1.3
multiblockPollutionMultiplier=2.0
tempToPermanentThreshold=200.0
tempToPermanentRate=0.005
naturalDecayRate=0.002

# 威胁系统
zombieSpawnChance=0.015
creeperSpawnChance=0.008
chargedCreeperChance=0.002

# 属性乘数
hpMultFactor=1.8
attackMultFactor=1.4
speedMultFactor=0.8
armorMultFactor=1.2
maxHpMultiplier=15.0
maxAttackMultiplier=10.0
```

---

## 10. API 参考

### 10.1 污染API

```java
import cn.minerealms.iic.api.I3CAPI;

// 获取临时污染
double pollution = I3CAPI.getTemporaryPollution(chunkPos);

// 添加临时污染
I3CAPI.addTemporaryPollution(chunkPos, amount);

// 清理污染
I3CAPI.cleanPollutionInRadius(level, center, radius, amount);
```

### 10.2 难度API

```java
import io.github.flemmli97.improvedmobs.api.difficulty.DifficultyFetcher;
import io.github.flemmli97.improvedmobs.api.difficulty.DifficultyGetter;

// 注册难度提供者
DifficultyFetcher.add(new ResourceLocation("modid", "name"), new MyDifficultyGetter());

// 实现接口
public class MyDifficultyGetter implements DifficultyGetter {
    @Override
    public float getDifficulty(Level level, Vec3 pos) {
        // 返回0-250难度值
        return calculatedDifficulty;
    }
    
    @Override
    public Config.IntegrationType getType() {
        return Config.IntegrationType.ADD;  // 或ON
    }
}
```

### 10.3 污染覆盖API

```java
import cn.minerealms.iic.api.PollutionOverlayAPI;

// 更新污染缓存
PollutionOverlayAPI.updatePollutionCache(chunkPos, pollution);

// 获取颜色
int color = PollutionOverlayAPI.getPollutionColor(pollution);
```

---

## 附录: 游戏平衡参考

### 难度等级

| 难度值 | 描述 | 怪物属性 |
|-------|------|---------|
| 0-25 | 基础难度 | 标准 |
| 25-50 | 轻微增强 | +10% 属性 |
| 50-75 | 中等难度 | +25% 属性,破坏方块 |
| 75-100 | 高难度 | +50% 属性 |
| 100-150 | 极难 | +100% 属性,主动攻击 |
| 150-200 | 专家 | +150% 属性,主动攻击机器 |
| 200-250 | 大师 | +200% 属性,闪电苦力怕 |

### 污染警告

| 污染值 | 颜色 | 威胁 |
|--------|------|------|
| 0-50 | 绿 | 安全 |
| 50-80 | 黄 | 低 |
| 80-120 | 橙 | 中 |
| 120-200 | 红 | 高 |
| 200+ | 紫 | 极限 |

---

*文档版本: 1.0*
*最后更新: 2026*