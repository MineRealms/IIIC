# 工业战争系统重构分析 v2.0
> 基于实际代码的深度分析与重构建议

---

## 📊 当前系统架构分析

### 1. 污染系统（PollutionManager.java）

#### ✅ 已实现的优点
```java
// 指数污染生成（第204行）
double pollutionValue = 0.01 * (1 + Math.pow(tier, 1.3) * 0.25);
if (GTIntegration.isMultiblock(be)) pollutionValue *= 3.0;
```
- ✅ 使用指数增长（tier^1.3）
- ✅ 多方块3倍加成
- ✅ 污染扩散机制（第354-366行）

#### ❌ 存在的问题

**问题1: 自然衰减是固定值而非比例**
```java
// 第273行 - 错误实现
double reduction = TriAxisConfig.naturalDecayRate;  // 固定0.03/秒
```
**影响**: 前期污染消失太快，后期污染积累太慢

**建议修改**:
```java
// 改为比例衰减
double reduction = current * 0.002;  // 每秒0.2%
```

---

**问题2: 环境吸收没有上限**
```java
// 第275-277行 - 有上限但实现不合理
double envReduction = getSurroundingEnvironmentalReduction(cPos);
double envCap = current * 0.1;  // 上限是污染的10%
reduction += Math.min(envCap, envReduction);
```
**影响**: 环境吸收在低污染时几乎无效，高污染时才有用（逻辑反了）

**建议修改**:
```java
// 环境吸收应该有绝对上限
double envScore = getSurroundingEnvironmentalReduction(cPos);
double envAbsorb = Math.min(envScore * 0.002, current * 0.1);
```

---

**问题3: 污染转化机制过于简单**
```java
// 第263行 - 无条件转化
double converted = current * TriAxisConfig.tempToPermanentRate;  // 0.001
addPermanentPollution(converted);
```
**影响**: 
- 没有阈值，1点污染也会转化
- 转化率太低（0.1%），永久污染积累太慢

**建议修改**:
```java
// 只有高污染才转化
if (current > 200.0) {
    double converted = (current - 200.0) * 0.005;  // 超过阈值的部分转化
    addPermanentPollution(converted);
}
```

---

### 2. 难度系统（TriAxisDifficultyManager.java）

#### ❌ 核心问题：加法模型

```java
// 第69-73行 - 加法模型
double targetD = TriAxisConfig.globalMultiplier * (
    TriAxisConfig.weightTime * T +
    TriAxisConfig.weightVoltage * V +
    TriAxisConfig.weightPollution * P
);
```

**问题分析**:
- 权重: Time=0.35, Voltage=0.35, Pollution=0.30
- 全局倍率: 2.0
- 最大难度: 2.0 * (0.35 + 0.35 + 0.30) = 2.0

**实际效果**:
| 阶段 | T | V | P | 难度 |
|------|---|---|---|------|
| 早期 | 0.2 | 0.2 | 0.1 | 1.0 |
| 中期 | 0.5 | 0.5 | 0.3 | 2.6 |
| 后期 | 0.8 | 0.8 | 0.6 | 4.4 |

**问题**: 
1. 三轴互相稀释（污染高但科技低 = 难度中等）
2. 没有"工业爆炸"的感觉
3. 后期难度增长线性

---

**建议：乘法模型**

```java
// 基础难度（时间）
double Base = 0.5 + 1.5 * T;  // 0.5 ~ 2.0

// 科技倍率（电压）
double Scale = 1.0 + Math.pow(V * effectiveMaxTier, 1.2) * 0.15;

// 污染压力（Sigmoid）
double sigmoid = 1.0 / (1.0 + Math.exp(-P * pollutionDenominator / 150.0 + 2));
double Pressure = 1.0 + 3.0 * sigmoid;  // 1.0 ~ 4.0

// 最终难度（乘法）
double targetD = Base * Scale * Pressure;
```

**效果对比**:
| 阶段 | 旧模型 | 新模型 | 说明 |
|------|--------|--------|------|
| ULV无污染 | 1.0 | 0.5 | 更友好 |
| MV中污染 | 2.6 | 2.1 | 略降低 |
| HV高污染 | 4.4 | 6.8 | 压力感 |
| IV极污染 | 4.4 | 13.6 | 灾难级 |

---

### 3. 威胁系统（ThreatManager.java）

#### ❌ 核心问题：随机概率太低

```java
// 第52-53行
if (pollution >= 60.0 && level.random.nextDouble() < 0.015) {  // 1.5%
    spawnHostileZombie(level, chunkPos);
}
```

**问题**:
- 每秒1.5%概率 = 平均67秒才刷一只
- 污染200时还是1.5% = 没有压力感
- 单只刷新 = 没有威胁

**建议修改**:
```java
// 基于污染的动态刷怪率
double spawnRate = 0.01 * (1 + pollution / 200.0);  // 污染越高越频繁
if (level.random.nextDouble() < spawnRate) {
    // 波次生成
    int waveSize = Math.min(3 + (int)(pollution / 50.0), 15);
    for (int i = 0; i < waveSize; i++) {
        spawnHostileZombie(level, chunkPos);
    }
}
```

**效果**:
| 污染 | 旧概率 | 新概率 | 波次大小 | 说明 |
|------|--------|--------|----------|------|
| 60 | 1.5% | 1.3% | 4只 | 小骚扰 |
| 100 | 1.5% | 1.5% | 5只 | 中等威胁 |
| 200 | 1.5% | 2.0% | 7只 | 持续压力 |
| 400 | 1.5% | 3.0% | 11只 | 高强度 |

---

### 4. Horde系统（HordeIntegrationManager.java）

#### ✅ 已实现的优点
- 完整的反射集成
- 污染触发机制（第44-46行）
- 小股袭扰系统（第49-53行）
- 电压等级倍率（第66-77行）

#### ❌ 存在的问题

**问题1: 触发概率固定**
```java
// 第46行
public static double pollutionHordeTriggerChance = 0.05;  // 固定5%
```
**影响**: 污染400和污染150触发概率一样

**建议修改**:
```java
// 动态触发概率
double triggerChance = 0.02 + (pollution / 200.0) * 0.08;  // 2% ~ 10%
```

---

**问题2: 间隔固定**
```java
// 第45行
public static double pollutionHordeCheckInterval = 600.0;  // 固定30秒
```
**影响**: 后期工业规模大但攻击频率不变

**建议修改**:
```java
// 基于污染的动态间隔
double baseInterval = 600.0;
double interval = baseInterval / (1.0 + pollution / 200.0);  // 污染越高间隔越短
```

**效果**:
| 污染 | 间隔 | 说明 |
|------|------|------|
| 50 | 30秒 | 偶尔骚扰 |
| 150 | 20秒 | 持续压力 |
| 300 | 15秒 | 高频攻击 |
| 600 | 10秒 | 战争状态 |

---

## 🎯 核心重构建议

### 优先级1: 难度计算改为乘法模型

**文件**: `TriAxisDifficultyManager.java`

**当前问题**: 加法模型导致三轴互相稀释

**重构方案**:
```java
public static DifficultyState calculateLocalDifficulty(ServerLevel level, BlockPos center) {
    // 1. 时间基础（Base）
    long mcDays = level.getDayTime() / 24000L;
    double tRaw = Math.log1p(mcDays / TriAxisConfig.baseDays) / 
                  Math.log1p(TriAxisConfig.targetDays / TriAxisConfig.baseDays);
    double T = Mth.clamp(tRaw, 0.0, 1.0);
    double Base = 0.5 + 1.5 * T;  // 0.5 ~ 2.0
    
    // 2. 科技倍率（Scale）
    double V = 0.0;
    if (TriAxisConfig.hasGTCEu()) {
        int medianTier = MachineScanner.scanNearbyVoltageTierMedianSafely(level, center, 
                                                                          TriAxisConfig.scanRadiusBlocks);
        V = (double) medianTier / TriAxisConfig.getEffectiveMaxTier();
    } else {
        V = T;
    }
    double Scale = 1.0 + Math.pow(V * 14.0, 1.2) * 0.15;  // 1.0 ~ 4.5
    
    // 3. 污染压力（Pressure）
    double localPollution = PollutionManager.getTemporaryPollution(
        new net.minecraft.world.level.ChunkPos(center));
    double globalPollution = PollutionManager.getPermanentPollution();
    double totalPollution = localPollution + globalPollution;
    
    double currentEma = pollutionEmaCache.getOrDefault(center, 0.0);
    double newEma = (TriAxisConfig.emaAlpha * totalPollution) + 
                    ((1.0 - TriAxisConfig.emaAlpha) * currentEma);
    pollutionEmaCache.put(center, newEma);
    
    double P = newEma / TriAxisConfig.pollutionDenominator;
    double sigmoid = 1.0 / (1.0 + Math.exp(-P + 2.0));
    double Pressure = 1.0 + 3.0 * sigmoid;  // 1.0 ~ 4.0
    
    // 4. 乘法融合
    double targetD = Base * Scale * Pressure * TriAxisConfig.globalMultiplier;
    
    // 5. 平滑与限速
    double currentD = difficultyCache.getOrDefault(center, 0.0);
    double delta = targetD - currentD;
    delta = Mth.clamp(delta, -TriAxisConfig.maxChangePerSec, TriAxisConfig.maxChangePerSec);
    double finalD = currentD + delta;
    difficultyCache.put(center, finalD);
    
    return new DifficultyState(finalD, T, V, P);
}
```

---

### 优先级2: 污染衰减改为比例

**文件**: `PollutionManager.java` 第273行

**当前代码**:
```java
double reduction = TriAxisConfig.naturalDecayRate;  // 固定0.03
```

**修改为**:
```java
double reduction = current * 0.002;  // 比例衰减0.2%
```

---

### 优先级3: 威胁系统改为波次生成

**文件**: `ThreatManager.java` 第46-62行

**当前代码**:
```java
if (avgVoltageTier >= 3.0) {
    if (pollution >= 60.0 && level.random.nextDouble() < 0.015) {
        spawnHostileZombie(level, chunkPos);
    }
}
```

**修改为**:
```java
if (avgVoltageTier >= 3.0) {
    // 动态刷怪率
    double spawnRate = TriAxisConfig.zombieSpawnChance * (1.0 + pollution / 200.0);
    
    if (level.random.nextDouble() < spawnRate) {
        // 波次大小
        int waveSize = Math.min(3 + (int)(pollution / 50.0), 15);
        
        for (int i = 0; i < waveSize; i++) {
            spawnHostileZombie(level, chunkPos);
        }
        
        IndustrialLogger.debugPollution(String.format(
            "Spawned zombie wave: size=%d, pollution=%.1f, tier=%.1f",
            waveSize, pollution, avgVoltageTier));
    }
}
```

---

### 优先级4: Horde间隔动态化

**文件**: `HordeIntegrationManager.java`

**添加方法**:
```java
/**
 * 计算基于污染的动态Horde间隔
 */
private static long calculateDynamicInterval(ServerPlayer player) {
    ChunkPos chunkPos = player.chunkPosition();
    double pollution = PollutionManager.getTemporaryPollution(chunkPos);
    
    double baseInterval = pollutionHordeCheckInterval;  // 600 ticks
    double dynamicInterval = baseInterval / (1.0 + pollution / 200.0);
    
    return (long) Math.max(dynamicInterval, 120.0);  // 最短6秒
}
```

**修改触发逻辑**（第XXX行）:
```java
// 使用动态间隔
long dynamicInterval = calculateDynamicInterval(player);
if (!isOnCooldown(player, playerHordeCooldowns, dynamicInterval)) {
    // 触发逻辑...
}
```

---

## 📈 预期效果对比

### 难度曲线

| 阶段 | 旧系统 | 新系统 | 变化 |
|------|--------|--------|------|
| ULV Day10 无污染 | 1.2 | 0.6 | -50% 更友好 |
| MV Day30 中污染 | 2.8 | 2.5 | -11% 略降 |
| HV Day50 高污染 | 4.2 | 7.2 | +71% 压力感 |
| EV Day100 极污染 | 4.8 | 15.6 | +225% 灾难级 |

### 威胁频率

| 污染 | 旧系统 | 新系统 | 变化 |
|------|--------|--------|------|
| 60 | 67秒/只 | 77秒/波(4只) | 4倍 |
| 100 | 67秒/只 | 67秒/波(5只) | 5倍 |
| 200 | 67秒/只 | 50秒/波(7只) | 9倍 |
| 400 | 67秒/只 | 33秒/波(11只) | 20倍 |

### Horde间隔

| 污染 | 旧系统 | 新系统 | 变化 |
|------|--------|--------|------|
| 50 | 30秒 | 26秒 | -13% |
| 150 | 30秒 | 16秒 | -47% |
| 300 | 30秒 | 12秒 | -60% |
| 600 | 30秒 | 8秒 | -73% |

---

## 🔧 配置参数调整

### TriAxisConfig.java 需要修改的默认值

```java
// 难度系统
public static double globalMultiplier = 1.5;  // 降低自2.0（因为改为乘法）
public static double pollutionDenominator = 150.0;  // 保持

// 污染系统
public static double naturalDecayRate = 0.002;  // 改为比例（原0.03固定值）
public static double tempToPermanentRate = 0.005;  // 提高自0.001
public static double tempToPermanentThreshold = 200.0;  // 新增阈值

// 威胁系统
public static double zombieSpawnChance = 0.01;  // 基础概率（会动态调整）
public static int zombieWaveMinSize = 3;  // 新增：最小波次
public static int zombieWaveMaxSize = 15;  // 新增：最大波次
```

---

## 🎮 玩家体验变化

### 早期（ULV-LV, 0-20天）
- **旧**: 难度1.2，偶尔单只僵尸
- **新**: 难度0.6，几乎无威胁
- **体验**: 更友好，有时间学习

### 中期（MV-HV, 20-50天）
- **旧**: 难度2.8，单只僵尸骚扰
- **新**: 难度2.5，小波次攻击（4-5只）
- **体验**: 需要建立基础防御

### 后期（EV-IV, 50-100天）
- **旧**: 难度4.2，单只僵尸
- **新**: 难度7.2，中波次攻击（7-9只）
- **体验**: 必须自动化防御

### 极后期（LuV+, 100+天）
- **旧**: 难度4.8，单只僵尸
- **新**: 难度15.6，大波次攻击（11-15只）+ Horde 8秒一次
- **体验**: 全面战争状态

---

## 🚀 实施步骤

### Step 1: 难度系统重构（核心）
1. 修改 `TriAxisDifficultyManager.calculateLocalDifficulty()`
2. 改为乘法模型
3. 测试难度曲线

### Step 2: 污染系统优化
1. 修改 `PollutionManager.processPollutionDecayAndScanning()`
2. 比例衰减 + 阈值转化
3. 测试污染积累

### Step 3: 威胁系统增强
1. 修改 `ThreatManager.checkAndTriggerThreats()`
2. 波次生成 + 动态概率
3. 测试刷怪频率

### Step 4: Horde系统动态化
1. 修改 `HordeIntegrationManager`
2. 动态间隔 + 动态触发
3. 测试Horde频率

---

## ⚠️ 风险评估

### 高风险
- **难度乘法模型**: 可能导致后期过难，需要大量测试
- **波次生成**: 可能导致性能问题（15只同时刷新）

### 中风险
- **比例衰减**: 可能导致前期污染消失太快
- **动态间隔**: 可能导致Horde过于频繁

### 低风险
- **配置调整**: 都有配置项，可以随时调整

---

## 📝 总结

### 当前系统的核心问题
1. ❌ **加法难度模型** - 三轴互相稀释，没有"工业爆炸"感
2. ❌ **固定衰减** - 前期污染消失太快，后期积累太慢
3. ❌ **单只刷怪** - 没有威胁感，性能浪费
4. ❌ **固定概率** - 污染高低没区别

### 重构后的改进
1. ✅ **乘法难度模型** - 后期指数增长，真正的灾难
2. ✅ **比例衰减** - 污染越多越难处理
3. ✅ **波次生成** - 有威胁感，性能更好
4. ✅ **动态系统** - 污染越高压力越大

### 下一步
选择一个优先级开始实施，我可以提供完整的代码实现。
