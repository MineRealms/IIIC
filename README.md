# I3C - Improved Integrated Industrial Craft

<div align="center">

**基于工业进度的动态难度缩放模组**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.1.3-orange.svg)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 目录

1. [项目概述](#1-项目概述)
2. [核心特性](#2-核心特性)
3. [联动模组](#3-联动模组)
4. [安装](#4-安装)
5. [指令系统](#5-指令系统)
6. [配置](#6-配置)
7. [游戏平衡](#7-游戏平衡)
8. [开发者API](#8-开发者api)

---

## 1. 项目概述

### 1.1 简介

I3C（Improved Integrated Industrial Craft）是 Improved Mobs 的扩展模组，基于工业进度动态调整游戏难度。模拟 Factorio 的污染与难度系统理念，将工业进度与游戏难度紧密绑定。

**核心机制**：机器越多 → 污染越高 → 难度越大

### 1.2 依赖

| 组件 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Forge | 47.1.3 |
| Improved Mobs | 1.20.1 |

### 1.3 包信息

- **模组ID**: `integratedindustrialcraft`
- **包名**: `cn.minerealms.iic`
- **入口类**: `IntegratedIndustrialCraft`

---

## 2. 核心特性

### 2.1 三轴难度系统

```
D = Base(T) × Scale(V) × Pressure(P) × GlobalMultiplier
```

| 轴 | 名称 | 描述 |
|----|------|------|
| T | Time | 游戏时间（0.5-2.0），30天达到满值 |
| V | Voltage | 电压等级（1.0-4.5），指数增长 |
| P | Pollution | 污染压力（1.0-4.0），Sigmoid曲线 |

### 2.2 双层污染系统

```
┌─────────────────────────────────────────┐
│  临时污染          │  永久污染            │
│  (Chunk-based)   │  (Global)           │
│  可被环境吸收     │  直接增加难度       │
└─────────────────────────────────────────┘
```

**污染公式**:
```
pollution = 0.21 × (tier+1)^1.3 × multiblockMultiplier

// 示例: EV多方块 (tier=4)
// = 0.21 × 8.55 × 2.0 = 3.59/秒
// 24小时 = 310 污染
```

**环境吸收**:
- 草地: ~0.3/秒 (200块)
- 树叶: ~0.375/秒 (150块)
- 水: ~0.06/秒 (50块)

### 2.3 威胁系统

| 阶段 | 电压 | 污染阈值 | 威胁 |
|------|------|----------|------|
| MV | 2 | ≥50 | 僵尸攻击机器 |
| HV | 3+ | ≥60 | 主动僵尸生成 |
| HV | 3+ | ≥100 | 主动苦力怕生成 |
| EV+ | 4+ | ≥180 | 闪电苦力怕 |

### 2.4 增强AI

- **ZombieDestroyMachineGoal**: 僵尸攻击机器（16格内）
- **CreeperTargetMachineGoal**: 苦力怕定向爆炸

### 2.5 炮塔系统

| 类型 | 等级 | 伤害 | 射程 |
|------|------|------|------|
| 激光 | Basic | 10 | 16 |
| 激光 | Advanced | 18 | 20 |
| 激光 | Elite | 28 | 24 |
| 激光 | Ultimate | 45 | 30 |
| 火焰 | Basic | 4+火 | 12 |

---

## 3. 联动模组

### 3.1 必需依赖

- **ImprovedMobs**: 难度缩放基础

### 3.2 可选集成

```
┌─────────────────────────────────────────┐
│               ImprovedMobs              │
│               (基础依赖)                │
└─────────────────────────────────────────┘
          │              │           │
          ▼              ▼           ▼
    ┌──────────┐   ┌──────────┐  ┌──────────┐
    │GregTech  │   │  Spore   │  │ Mekanism │
    │  CEu    │   │孢子集成  │  │ 能量系统 │
    │机器检测 │   │虫巢进化  │  │激光炮塔 │
    │电压等级 │   │污染反馈  │  │        │
    └──────────┘   └──────────┘  └────────┘
```

| 模组 | 功能 |
|------|------|
| GregTech CEu Modern | 机器检测、电压等级、污染生成 |
| Spore | 虫巢邻近加速、污染反馈、进化系统 |
| Mekanism | 炮塔能量系统 |

---

## 4. 安装

### 4.1 文件结构

```
mods/
├── integratedindustrialcraft-*.jar    # 主模组
├── improvedmobs-*.jar              # 依赖
├── gtceu-*.jar                     # 推荐
├── spore-*.jar                    # 可选
└── mekanism-*.jar                # 可选
```

### 4.2 配置文件

- 主配置: `config/triaxis-difficulty.properties`
- 炮塔配置: `config/iic/turrets.toml`

### 4.3 首次运行

首次运行后自动生成默认配置文件，可编辑 `config/triaxis-difficulty.properties` 自定义所有参数。

---

## 5. 指令系统

### 5.1 根指令

```
/im <子指令>
```

### 5.2 污染指令

```
/im pollution add <amount> [pos]      # 添加临时污染
/im pollution remove <amount> [pos] # 移除临时污染
/im pollution set <amount> [pos]     # 设置临时污染
/im pollution clear [radius]       # 清除污染
/im pollution permanent add <amt> # 添加永久污染
/im pollution status [pos]        # 污染状态
```

### 5.3 难度指令

```
/im difficulty                    # 查看难度
/im difficulty set <value>       # 设置难度
/im difficulty reset             # 重置难度
```

### 5.4 配置指令

```
/im config preset <preset>          # 设置预设 (NORMAL/HARD/HARDCORE/INSANE/CUSTOM)
/im config reload                # 重载配置
/im config get <key>            # 获取配置值
/im config set <key> <value>    # 设置配置值
```

### 5.5 其他指令

```
/im scan [radius]                # 扫描机器
/im hud on|off                  # HUD显示
/im debug on|off              # 调试日志
/im horde status               # 尸潮状态
/im spawn zombie <count> [pos]   # 生成僵尸
/im spawn creeper <count> [pos]  # 生成苦力怕
```

---

## 6. 配置

### 6.1 难度预设

| 预设 | 全局乘数 | 威胁 | 时间曲线 |
|------|---------|------|----------|
| NORMAL | 2.0 | 标准 | 800天 |
| HARD | 2.5 | 提高 | 1000天 |
| HARDCORE | 2.8 | 高 | 1200天 |
| INSANE | 3.5 | 极限 | 1500天 |

### 6.2 关键参数

```properties
# 难度系统
difficultyPreset=NORMAL
targetMaxDifficulty=250.0
realWorldDaysToMax=30

# 三轴参数
baseMin=0.5
baseMax=2.0
scaleExponent=1.2
scaleMultiplier=0.15
pressureMax=4.0
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
```

---

## 7. 游戏平衡

### 7.1 难度等级

| 难度值 | 怪物属性 | 威胁 |
|-------|---------|------|
| 0-25 | 基础 | 无 |
| 25-50 | +10% | 轻微 |
| 50-75 | +25%,破坏方块 | 中等 |
| 75-100 | +50% | 高 |
| 100-150 | +100% | 很高 |
| 150-200 | +150%,攻击机器 | 极限 |
| 200-250 | +200%,闪电苦力怕 | 大师 |

### 7.2 游戏阶段

| 阶段 | 机器数 | 污染 | 难度 |
|------|--------|------|------|
| MV | 20-30 | 30-60 | +0.5~1.0 |
| HV | 40-60 | 80-150 | +1.5~2.5 |
| IV-LuV | 100+ | 150-300 | +3.0~5.0 |
| ZPM-UHV | 200+ | 300+ | +5.0+ |

---

## 8. 开发者API

### 8.1 污染API

```java
import cn.minerealms.iic.api.I3CAPI;

// 获取临时污染
double pollution = I3CAPI.getTemporaryPollution(chunkPos);

// 添加临时污染
I3CAPI.addTemporaryPollution(chunkPos, amount);

// 清理污染
I3CAPI.cleanPollutionInRadius(level, center, radius, amount);
```

### 8.2 难度API

```java
import io.github.flemmli97.improvedmobs.api.difficulty.DifficultyFetcher;
import io.github.flemmli97.improvedmobs.api.difficulty.DifficultyGetter;

// 注册难度提供者
DifficultyFetcher.add(
    new ResourceLocation("modid", "name"),
    new MyDifficultyGetter()
);

// 实现接口
public class MyDifficultyGetter implements DifficultyGetter {
    @Override
    public float getDifficulty(Level level, Vec3 pos) {
        return calculatedDifficulty; // 0-250
    }
    
    @Override
    public Config.IntegrationType getType() {
        return Config.IntegrationType.ADD; // 或ON
    }
}
```

---

# English

## Overview

I3C (Improved Integrated Industrial Craft) is a dynamic difficulty scaling addon for ImprovedMobs, integrating industrial mods to create a cohesive progression system.

## Key Features

- Tri-Axis Difficulty System (Time × Voltage × Pollution)
- Dual-Layer Pollution (Temporary + Permanent)
- Voltage-Based Threat System
- Enhanced Mob AI (attack machines)
- Laser Turret System (4 tiers)
- Full API for mod integration

## Commands

```
/im difficulty          - Check difficulty
/im pollution status    - Check pollution
/im scan               - Scan machines
/im config preset     - Set preset
/im debug on|off       - Toggle debug
```

## Configuration

Edit `config/triaxis-difficulty.properties` to customize difficulty, pollution, and threat parameters. Four presets available: NORMAL, HARD, HARDCORE, INSANE.

---

## License

MIT License - See [LICENSE](LICENSE).

## Support

- [GitHub Issues](https://github.com/MineRealms/IIIC/issues)
- [Discord](https://discord.gg/minerealms)