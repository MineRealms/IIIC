# Difficulty System Plan B Implementation

## 概述

方案 B 让 IIC 的三轴难度系统完全接管 ImprovedMobs 的难度计算，提供更精细和可控的难度曲线。

## 核心改动

### 1. DifficultyProvider 修改

**文件**: `src/main/java/cn/minerealms/iic/difficulty/DifficultyProvider.java`

**改动**:
- `getType()` 现在返回 `Config.IntegrationType.ON`（可配置）
- 添加 `scaleToImprovedMobsRange()` 方法，将 0-36 范围缩放到 0-250
- 输出日志包含原始值和缩放后的值

**缩放逻辑**:
```
理论最大值 = baseMax × (1 + maxTier^scaleExponent × scaleMultiplier) × pressureMax × globalMultiplier
           = 2.0 × (1 + 9^1.2 × 0.15) × 4.0 × 2.8
           = 2.0 × 2.95 × 4.0 × 2.8
           = 66.08

缩放后 = (原始难度 / 理论最大值) × targetMaxDifficulty
       = (原始难度 / 66.08) × 250
```

### 2. TriAxisConfig 新增参数

**文件**: `src/main/java/cn/minerealms/iic/industrial/TriAxisConfig.java`

**新参数**:

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `takeoverImprovedMobsDifficulty` | boolean | true | 是否接管 ImprovedMobs 难度系统 |
| `targetMaxDifficulty` | double | 250.0 | 最大难度目标值（ImprovedMobs 期望范围） |
| `realWorldDaysToMax` | double | 30.0 | 达到最大难度的现实世界天数 |

### 3. 时间曲线调整

**文件**: `src/main/java/cn/minerealms/iic/difficulty/TriAxisDifficultyManager.java`

**改动**:
- 时间计算现在基于 `realWorldDaysToMax` 而不是 `targetDays`
- 1 现实天 = 72 MC 天（24小时 × 3 MC天/小时）
- 30 现实天 = 2160 MC 天

**公式**:
```java
double mcDaysPerRealDay = 24.0 * 3.0; // 72 MC days per real day
double targetMCDays = realWorldDaysToMax * mcDaysPerRealDay; // 30 × 72 = 2160
double tRaw = Math.log1p(mcDays / baseDays) / Math.log1p(targetMCDays / baseDays);
double T = clamp(tRaw, 0.0, 1.0);
```

## 难度预期

### 时间因素（无工业发展）

| 现实时间 | MC 天数 | T 因子 | Base | 缩放后 Difficulty |
|----------|---------|--------|------|-------------------|
| 0 天 | 0 | 0.00 | 0.50 | ~4.7 |
| 5 天 | 360 | 0.42 | 1.13 | ~10.7 |
| 10 天 | 720 | 0.56 | 1.34 | ~12.7 |
| 15 天 | 1080 | 0.65 | 1.48 | ~14.0 |
| 20 天 | 1440 | 0.71 | 1.57 | ~14.9 |
| 30 天 | 2160 | 0.81 | 1.72 | ~16.3 |
| 60 天 | 4320 | 0.93 | 1.90 | ~18.0 |

### HV 阶段（Tier 3，中等污染）

假设：
- 时间: 10 天（T = 0.56）
- 电压: HV (Tier 3, V = 3/9 = 0.33)
- 污染: 100（P = 100/800 = 0.125）

计算：
```
Base = 0.5 + (2.0 - 0.5) × 0.56 = 1.34
Scale = 1.0 + (3 × 0.33)^1.2 × 0.15 = 1.0 + 1.14 = 2.14
Pressure = 1.0 + (4.0 - 1.0) × sigmoid(0.125) = 1.0 + 3.0 × 0.53 = 2.59
Raw D = 1.34 × 2.14 × 2.59 × 2.8 = 21.0
Scaled D = (21.0 / 66.08) × 250 = 79.5
```

**结果**: HV 阶段 difficulty ≈ **80**

### UV 阶段（Tier 8，高污染）

假设：
- 时间: 30 天（T = 0.81）
- 电压: UV (Tier 8, V = 8/9 = 0.89)
- 污染: 300（P = 300/800 = 0.375）

计算：
```
Base = 0.5 + (2.0 - 0.5) × 0.81 = 1.72
Scale = 1.0 + (8 × 0.89)^1.2 × 0.15 = 1.0 + 8.5 × 0.15 = 2.28
Pressure = 1.0 + (4.0 - 1.0) × sigmoid(0.375) = 1.0 + 3.0 × 0.88 = 3.64
Raw D = 1.72 × 2.28 × 3.64 × 2.8 = 40.1
Scaled D = (40.1 / 66.08) × 250 = 151.7
```

**结果**: UV 阶段 difficulty ≈ **152**

## 怪物破坏方块触发条件

根据 ImprovedMobs 源码分析：

```java
if (difficulty >= Config.CommonConfig.difficultyBreak && 
    mob.getRandom().nextFloat() < Config.CommonConfig.breakerChance.get(map))
```

**默认配置**:
- `difficultyBreak`: 通常为 **50-100**
- `breakerChance`: 默认 `"0.3"`（30% 概率）

**结论**:
- **Difficulty ≥ 50**: 怪物有 30% 概率获得破坏方块能力
- **HV 阶段（difficulty ≈ 80）**: 怪物会开始破坏方块
- **UV 阶段（difficulty ≈ 152）**: 怪物破坏方块能力完全激活

## 配置建议

### 快速发展（15天到UV）

```properties
takeoverImprovedMobsDifficulty=true
targetMaxDifficulty=250.0
realWorldDaysToMax=15.0
globalMultiplier=3.5
```

### 正常发展（30天到UV）

```properties
takeoverImprovedMobsDifficulty=true
targetMaxDifficulty=250.0
realWorldDaysToMax=30.0
globalMultiplier=2.8
```

### 慢速发展（60天到UV）

```properties
takeoverImprovedMobsDifficulty=true
targetMaxDifficulty=250.0
realWorldDaysToMax=60.0
globalMultiplier=2.0
```

### 降低 HV 阶段难度

如果 HV 阶段太难，可以调整：

```properties
# 降低污染影响
pollutionDenominator=1000.0  # 从 800 提升到 1000

# 降低电压影响
scaleMultiplier=0.10  # 从 0.15 降低到 0.10

# 降低全局倍率
globalMultiplier=2.0  # 从 2.8 降低到 2.0
```

## 测试验证

### 测试步骤

1. **启动新存档**
2. **挂机测试**（无工业发展）:
   - 挂机 5 小时（约 15 MC 天）
   - 预期 difficulty: ~5-10
   - 怪物应该还比较弱

3. **HV 阶段测试**:
   - 建造 40-60 台 HV 机器
   - 运行一段时间产生污染（100+）
   - 预期 difficulty: 70-90
   - 怪物应该开始破坏方块

4. **UV 阶段测试**:
   - 建造 100+ 台 UV 机器
   - 污染达到 300+
   - 预期 difficulty: 150-180
   - 怪物应该非常强

### 调试命令

```
/im industrial status  # 查看当前难度状态
/im industrial debug on  # 开启调试日志
```

## 与原系统对比

| 特性 | 原系统（ADD模式） | 新系统（ON模式） |
|------|------------------|------------------|
| 难度来源 | ImprovedMobs 基础 + IIC 加成 | 完全由 IIC 控制 |
| 挂机增长 | 每 2 分钟 +0.1 | 基于时间曲线，可配置 |
| 输出范围 | 0-36（太小） | 0-250（匹配 ImprovedMobs） |
| 时间控制 | 不可控 | 可配置（realWorldDaysToMax） |
| HV 难度 | 不可预测 | 可精确调整 |

## 注意事项

1. **配置文件会在首次运行时自动生成新参数**
2. **旧存档需要删除 `run/config/triaxis-difficulty.properties` 重新生成**
3. **如果觉得难度不合适，优先调整 `globalMultiplier` 和 `realWorldDaysToMax`**
4. **不要同时大幅调整多个参数，逐个测试效果**

## 回滚到 ADD 模式

如果需要回到原来的模式：

```properties
takeoverImprovedMobsDifficulty=false
```

这样 IIC 会回到 ADD 模式，难度会叠加到 ImprovedMobs 的基础难度上。
