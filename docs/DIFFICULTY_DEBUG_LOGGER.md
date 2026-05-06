# DifficultyDebugLogger 使用文档

## 概述

`DifficultyDebugLogger` 用于详细记录三轴难度系统每2分钟的所有参数变化，帮助诊断难度计算异常问题。

## 输出文件

- **位置**: `logs/difficulty-debug.csv`
- **格式**: CSV (逗号分隔)
- **记录间隔**: 2分钟 (2400 ticks)

## 使用方法

### 1. 启动服务器

日志文件会在第一次记录时自动创建。无需额外配置。

### 2. 查看日志

使用任意CSV查看工具:

```bash
# Windows - 使用Excel打开
start difficulty-debug.csv

# 查看前100行
Select-Object -First 100 logs\difficulty-debug.csv

# 使用PowerShell查看
Import-Csv logs\difficulty-debug.csv | Select-Object -First 20
```

## CSV 列说明

| 列名 | 说明 | 示例值 |
|------|------|--------|
| Timestamp | 记录时间 | 2026-05-05 10:30:00 |
| LogIndex | 日志序号 | 1, 2, 3... |
| RealTimeMin | 实际运行分钟数 | 120.50 |
| GameTick | 游戏tick数 | 144000 |
| MCDays | Minecraft天数 | 6 |
| ChunkX | 玩家所在区块X | 0 |
| ChunkZ | 玩家所在区块Z | 0 |
| TimeFactorT | 时间因子 (0-1) | 0.015 |
| Base | 基础难度 (0.5-2.0) | 0.52 |
| VoltageFactorV | 电压因子 (0-1) | 0.0 |
| Scale | 规模乘数 (1.0-4.5) | 1.0 |
| PollutionFactorP | 污染因子 (0-1) | 0.0 |
| EMAPollution | EMA平滑污染值 | 0.0 |
| PressureMultiplier | 压力乘数 (1.0-4.0) | 1.0 |
| GlobalMultiplier | 全局乘数 | 2.8 |
| RawDifficulty | 原始三轴难度 | 1.456 |
| TheoreticalMax | 理论最大值 | 65.72 |
| CachedDifficulty | 缓存的难度值 | 1.456 |
| ScaledDifficulty | 缩放到IM范围 | 5.54 |
| LocalPollution | 临时污染 | 0.0 |
| GlobalPollution | 永久污染 | 0.0 |
| Config_realWorldDaysToMax | 配置: 现实天数到最大 | 30.0 |
| Config_baseDays | 配置: 对数基数 | 30.0 |
| Config_baseMin | 配置: 最小基础 | 0.5 |
| Config_baseMax | 配置: 最大基础 | 2.0 |
| Config_scaleExponent | 配置: 电压指数 | 1.2 |
| Config_scaleMultiplier | 配置: 电压乘数 | 0.15 |
| Config_pressureMin | 配置: 最小压力 | 1.0 |
| Config_pressureMax | 配置: 最大压力 | 4.0 |
| Config_sigmoidShift | 配置: Sigmoid偏移 | 2.0 |
| Config_pollutionDenominator | 配置: 污染分母 | 800.0 |
| Config_targetMaxDifficulty | 配置: 目标最大难度 | 250.0 |
| Config_maxChangePerSec | 配置: 每秒最大变化 | 0.015 |
| Config_hasGTCEu | 配置: 是否检测到GT | false |
| State_timeFactor | 状态: 时间因子 | 0.015 |
| State_voltageFactor | 状态: 电压因子 | 0.0 |
| State_pollutionFactor | 状态: 污染因子 | 0.0 |

## 诊断问题

### 问题1: 难度异常高

**现象**: 服务器刚开几分钟，difficulty就到100+

**排查步骤**:
1. 打开 `logs/difficulty-debug.csv`
2. 查看 `ScaledDifficulty` 列
3. 查看 `RawDifficulty` 是否异常高
4. 如果 `RawDifficulty` 很高但 `TimeFactorT` 很低:
   - 检查 `CachedDifficulty` 列 - 可能缓存累积了旧值
   - 检查是否是 `hasGTCEu=false` 但 `VoltageFactorV` 很高

### 问题2: V=T 导致错误

**现象**: 时间轴1%，电压轴也是1%（应该0%）

**原因**: 代码bug - fallback错误设为 `V=T`

**查看**:
- `VoltageFactorV` ≈ `TimeFactorT`
- `Config_hasGTCEu=false`

### 问题3: 缓存不更新

**现象**: difficulty几乎不变

**排查**:
- 查看 `CachedDifficulty` 是否等于 `RawDifficulty`
- 检查 `Config_maxChangePerSec` 是否太小 (默认0.015)

## 公式参考

```
T = log1p(mcDays / baseDays) / log1p(targetMCDays / baseDays)
Base = baseMin + (baseMax - baseMin) * T

V = medianTier / effectiveMaxTier
Scale = 1.0 + pow(V * maxTier, scaleExponent) * scaleMultiplier

P = emaPollution / pollutionDenominator
Pressure = pressureMin + (pressureMax - pressureMin) * sigmoid(P - sigmoidShift)

Raw = Base * Scale * Pressure * globalMultiplier
TheoreticalMax = baseMax * (1 + maxTier^scaleExp * scaleMult) * pressureMax * globalMultiplier
Scaled = Raw / TheoreticalMax * targetMaxDifficulty
```

## 示例分析

### 正常情况 (无机器)
```
RealTimeMin: 120, MCDays: 6
TimeFactorT: 0.015, Base: 0.52
VoltageFactorV: 0.0, Scale: 1.0
PollutionFactorP: 0.0, Pressure: 1.0
GlobalMultiplier: 2.8
RawDifficulty: 0.52 * 1.0 * 1.0 * 2.8 = 1.456
TheoreticalMax: 65.72
ScaledDifficulty: 1.456 / 65.72 * 250 = 5.54
```

### 异常情况 (V=T bug)
```
RealTimeMin: 120, MCDays: 6
TimeFactorT: 0.015 (但显示为1.5%)
VoltageFactorV: 0.015 (错误! 应该是0.0!)
Config_hasGTCEu: false
-> Scale = 1.0 + 0.015 * 1.2^1.2 * 0.15 = 1.003
-> RawDifficulty 被放大
```

### 修复后预期
- `TimeFactorT`: 约 0.01-0.05 (取决于服务器运行时长)
- `VoltageFactorV`: 0.0 (无GT CEu时)
- `PollutionFactorP`: 0.0 (无污染时)
- `ScaledDifficulty` < 10 (初期)