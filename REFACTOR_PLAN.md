# 工业战争系统重构实施计划
> 基于代码分析的具体实施方案

---

## 🎯 重构目标

将当前的"线性难度系统"改造为"工业驱动的动态对抗系统"

**核心体验**: 建得越大 → 污染越多 → 被打越狠 → 必须自动化防御

---

## 📋 实施阶段

### Phase 1: 难度系统重构（核心）⭐⭐⭐

**优先级**: 最高  
**影响范围**: 整个游戏体验  
**预计工作量**: 2-3小时

#### 1.1 修改难度计算模型

**文件**: `TriAxisDifficultyManager.java`

**当前代码** (第69-73行):
```java
double targetD = TriAxisConfig.globalMultiplier * (
    TriAxisConfig.weightTime * T +
    TriAxisConfig.weightVoltage * V +
    TriAxisConfig.weightPollution * P
);
```

**修改为**:
```java
// 1. 时间基础（Base）- 防止挂机
double Base = 0.5 + 1.5 * T;  // 0.5 ~ 2.0

// 2. 科技倍率（Scale）- 工业规模
double tierValue = V * TriAxisConfig.getEffectiveMaxTier();
double Scale = 1.0 + Math.pow(tierValue, 1.2) * 0.15;  // 1.0 ~ 4.5

// 3. 污染压力（Pressure）- 环境反噬
double P = newEma / TriAxisConfig.pollutionDenominator;
double sigmoid = 1.0 / (1.0 + Math.exp(-P + 2.0));
double Pressure = 1.0 + 3.0 * sigmoid;  // 1.0 ~ 4.0

// 4. 乘法融合
double targetD = Base * Scale * Pressure * TriAxisConfig.globalMultiplier;
```

#### 1.2 添加配置参数

**文件**: `TriAxisConfig.java`

```java
// 难度模型参数
public static double baseMin = 0.5;           // 最小基础难度
public static double baseMax = 2.0;           // 最大基础难度
public static double scaleExponent = 1.2;     // 科技倍率指数
public static double scaleMultiplier = 0.15;  // 科技倍率系数
public static double pressureMin = 1.0;       // 最小污染压力
public static double pressureMax = 4.0;       // 最大污染压力
public static double sigmoidShift = 2.0;      // Sigmoid偏移
```

#### 1.3 更新配置文件

**文件**: `DifficultyConfig.java`

添加新的配置项到 Forge 配置系统。

---

### Phase 2: 污染系统优化 ⭐⭐⭐

**优先级**: 高  
**影响范围**: 污染积累速度、环境反馈  
**预计工作量**: 1-2小时

#### 2.1 修改自然衰减为比例

**文件**: `PollutionManager.java` (第273行)

**当前代码**:
```java
double reduction = TriAxisConfig.naturalDecayRate;  // 固定0.03
```

**修改为**:
```java
double reduction = current * TriAxisConfig.naturalDecayRate;  // 比例衰减
```

**配置调整**:
```java
// TriAxisConfig.java
public static double naturalDecayRate = 0.002;  // 从0.03改为0.002（0.2%/秒）
```

#### 2.2 优化环境吸收机制

**文件**: `PollutionManager.java` (第275-277行)

**当前代码**:
```java
double envReduction = getSurroundingEnvironmentalReduction(cPos);
double envCap = current * 0.1;
reduction += Math.min(envCap, envReduction);
```

**修改为**:
```java
double envScore = getSurroundingEnvironmentalReduction(cPos);
// 环境吸收有绝对上限，但也受当前污染限制
double envAbsorb = Math.min(envScore * 0.002, current * 0.15);
reduction += envAbsorb;
```

#### 2.3 改进污染转化机制

**文件**: `PollutionManager.java` (第263行)

**当前代码**:
```java
double converted = current * TriAxisConfig.tempToPermanentRate;
addPermanentPollution(converted);
```

**修改为**:
```java
// 只有超过阈值的污染才转化
if (current > TriAxisConfig.tempToPermanentThreshold) {
    double excess = current - TriAxisConfig.tempToPermanentThreshold;
    double converted = excess * TriAxisConfig.tempToPermanentRate;
    addPermanentPollution(converted);
}
```

**配置调整**:
```java
// TriAxisConfig.java
public static double tempToPermanentThreshold = 200.0;  // 保持不变
public static double tempToPermanentRate = 0.005;       // 从0.001提高到0.005
```

---

### Phase 3: 威胁系统增强 ⭐⭐

**优先级**: 中高  
**影响范围**: 战斗频率、威胁强度  
**预计工作量**: 2-3小时

#### 3.1 实现动态刷怪率

**文件**: `ThreatManager.java` (第52-60行)

**当前代码**:
```java
if (pollution >= TriAxisConfig.hvZombieSpawnThreshold && 
    level.random.nextDouble() < TriAxisConfig.zombieSpawnChance) {
    spawnHostileZombie(level, chunkPos);
}
```

**修改为**:
```java
// 动态刷怪率：污染越高越频繁
double baseRate = TriAxisConfig.zombieSpawnChance;
double spawnRate = baseRate * (1.0 + pollution / 200.0);

if (pollution >= TriAxisConfig.hvZombieSpawnThreshold && 
    level.random.nextDouble() < spawnRate) {
    
    // 波次生成：污染越高波次越大
    int waveSize = calculateWaveSize(pollution, avgVoltageTier);
    spawnHostileWave(level, chunkPos, waveSize, false);
}
```

#### 3.2 添加波次生成方法

**文件**: `ThreatManager.java`

```java
/**
 * 计算波次大小
 */
private static int calculateWaveSize(double pollution, double avgVoltageTier) {
    // 基础波次：3只
    int base = 3;
    
    // 污染加成：每50污染+1只
    int pollutionBonus = (int)(pollution / 50.0);
    
    // 电压加成：每2级+1只
    int tierBonus = (int)(avgVoltageTier / 2.0);
    
    int total = base + pollutionBonus + tierBonus;
    
    // 上限：15只（防止性能问题）
    return Math.min(total, 15);
}

/**
 * 生成一波敌对生物
 */
private static void spawnHostileWave(ServerLevel level, ChunkPos chunkPos, 
                                     int count, boolean includeCreepers) {
    for (int i = 0; i < count; i++) {
        if (includeCreepers && level.random.nextDouble() < 0.3) {
            boolean charged = level.random.nextDouble() < 0.1;
            spawnHostileCreeper(level, chunkPos, charged);
        } else {
            spawnHostileZombie(level, chunkPos);
        }
    }
    
    IndustrialLogger.debugPollution(String.format(
        "Spawned hostile wave: %d mobs at chunk %s", count, chunkPos));
}
```

#### 3.3 添加配置参数

**文件**: `TriAxisConfig.java`

```java
// 波次系统配置
public static int waveBaseSize = 3;           // 基础波次大小
public static int wavePollutionDivisor = 50;  // 污染加成除数
public static int waveTierDivisor = 2;        // 电压加成除数
public static int waveMaxSize = 15;           // 最大波次大小
public static double waveCreeperChance = 0.3; // 爬行者比例
```

---

### Phase 4: Horde系统动态化 ⭐⭐

**优先级**: 中  
**影响范围**: 大规模攻击频率  
**预计工作量**: 1-2小时

#### 4.1 实现动态触发概率

**文件**: `HordeIntegrationManager.java` (第46行)

**当前代码**:
```java
public static double pollutionHordeTriggerChance = 0.05;  // 固定5%
```

**修改为动态计算**:

在 `checkPollutionTriggeredHorde` 方法中:
```java
// 动态触发概率：2% ~ 10%
double baseTriggerChance = 0.02;
double pollutionFactor = pollution / 200.0;
double triggerChance = baseTriggerChance + pollutionFactor * 0.08;
triggerChance = Math.min(triggerChance, 0.10);

if (level.random.nextDouble() < triggerChance) {
    // 触发尸潮
}
```

#### 4.2 实现动态检查间隔

**文件**: `HordeIntegrationManager.java`

添加新方法:
```java
/**
 * 计算动态检查间隔
 */
public static long calculateHordeCheckInterval(double pollution) {
    double baseInterval = pollutionHordeCheckInterval;  // 600 ticks
    double factor = 1.0 + pollution / 200.0;
    long interval = (long)(baseInterval / factor);
    
    // 最小间隔：5秒（100 ticks）
    return Math.max(interval, 100L);
}
```

在 tick 方法中使用:
```java
long interval = calculateHordeCheckInterval(pollution);
if (tickCounter % interval == 0) {
    checkPollutionTriggeredHorde(player, level);
}
```

---

### Phase 5: 怪物属性优化 ⭐

**优先级**: 低  
**影响范围**: 怪物强度曲线  
**预计工作量**: 1小时

#### 5.1 修改属性倍率公式

**问题**: ImprovedMobs 使用线性倍率，后期会指数爆炸

**解决方案**: 在 `DifficultyProvider.java` 中限制难度输出

```java
@Override
public float getDifficulty(ServerLevel level, Vec3 pos) {
    float totalDifficulty = calculateTotalDifficulty(level, pos);
    
    // 对数压缩：防止指数爆炸
    // difficulty 10 → 10
    // difficulty 50 → 20
    // difficulty 100 → 25
    float compressed = (float)(Math.log1p(totalDifficulty) * 10.0);
    
    return compressed;
}
```

**效果**:
| 原始难度 | 压缩后 | 怪物HP倍率 | 怪物攻击倍率 |
|----------|--------|------------|--------------|
| 5 | 5 | 1.8x | 1.4x |
| 10 | 10 | 2.6x | 1.8x |
| 20 | 13 | 3.1x | 2.1x |
| 50 | 20 | 4.2x | 2.6x |
| 100 | 25 | 5.0x | 3.0x |

---

## 📊 配置参数总览

### 新增配置参数

```java
// TriAxisConfig.java

// === 难度模型参数 ===
public static double baseMin = 0.5;
public static double baseMax = 2.0;
public static double scaleExponent = 1.2;
public static double scaleMultiplier = 0.15;
public static double pressureMin = 1.0;
public static double pressureMax = 4.0;
public static double sigmoidShift = 2.0;

// === 污染系统参数 ===
public static double naturalDecayRate = 0.002;  // 改为比例
public static double envAbsorptionMultiplier = 0.002;
public static double envAbsorptionCap = 0.15;
public static double tempToPermanentRate = 0.005;  // 提高

// === 波次系统参数 ===
public static int waveBaseSize = 3;
public static int wavePollutionDivisor = 50;
public static int waveTierDivisor = 2;
public static int waveMaxSize = 15;
public static double waveCreeperChance = 0.3;

// === Horde动态参数 ===
public static double hordeTriggerChanceMin = 0.02;
public static double hordeTriggerChanceMax = 0.10;
public static long hordeIntervalMin = 100L;  // 5秒
```

---

## 🧪 测试计划

### 测试场景1: 早期游戏（ULV-LV）
- 时间: 0-10天
- 科技: ULV-LV
- 污染: 0-50
- **预期**: 几乎无攻击，偶尔1-2只僵尸

### 测试场景2: 中期游戏（MV-HV）
- 时间: 10-30天
- 科技: MV-HV
- 污染: 50-150
- **预期**: 每分钟1-2波，每波3-5只

### 测试场景3: 后期游戏（EV-IV）
- 时间: 30-60天
- 科技: EV-IV
- 污染: 150-300
- **预期**: 每30秒1波，每波5-10只，偶尔Horde

### 测试场景4: 极限游戏（LuV+）
- 时间: 60+天
- 科技: LuV+
- 污染: 300+
- **预期**: 持续攻击，每波10-15只，频繁Horde

---

## 📈 预期效果对比

### 难度曲线

| 阶段 | 当前系统 | 重构后 | 变化 |
|------|----------|--------|------|
| ULV无污染 | 1.0 | 0.5 | -50% 更友好 |
| MV低污染 | 1.8 | 1.2 | -33% 略降 |
| HV中污染 | 2.6 | 3.4 | +31% 压力感 |
| EV高污染 | 3.2 | 6.8 | +113% 挑战性 |
| IV极污染 | 3.8 | 13.6 | +258% 灾难级 |

### 攻击频率

| 污染 | 当前系统 | 重构后 | 变化 |
|------|----------|--------|------|
| 60 | 67秒/只 | 50秒/波(4只) | 5倍 |
| 100 | 67秒/只 | 40秒/波(5只) | 8倍 |
| 200 | 67秒/只 | 25秒/波(7只) | 19倍 |
| 400 | 67秒/只 | 17秒/波(11只) | 39倍 |

---

## ⚠️ 风险评估

### 高风险项
1. **难度模型改动** - 影响整个游戏平衡
   - 缓解: 保留旧模型作为配置选项
   - 测试: 多阶段充分测试

2. **性能问题** - 波次生成可能导致卡顿
   - 缓解: 严格限制波次大小（15只上限）
   - 优化: 异步生成，分帧处理

### 中风险项
1. **配置兼容性** - 旧配置文件可能失效
   - 缓解: 提供配置迁移工具
   - 默认值: 保守设置

2. **平衡性** - 可能过难或过简单
   - 缓解: 提供多个预设（Easy/Normal/Hard）
   - 调整: 快速迭代数值

---

## 🚀 实施顺序

### 第一周: 核心重构
- [ ] Phase 1: 难度系统重构
- [ ] Phase 2: 污染系统优化
- [ ] 基础测试

### 第二周: 威胁增强
- [ ] Phase 3: 威胁系统增强
- [ ] Phase 4: Horde系统动态化
- [ ] 集成测试

### 第三周: 优化与测试
- [ ] Phase 5: 怪物属性优化
- [ ] 性能优化
- [ ] 全面测试
- [ ] 平衡调整

---

## 📝 实施检查清单

### 代码修改
- [ ] TriAxisDifficultyManager.java - 乘法模型
- [ ] PollutionManager.java - 比例衰减
- [ ] PollutionManager.java - 环境吸收
- [ ] PollutionManager.java - 污染转化
- [ ] ThreatManager.java - 动态刷怪率
- [ ] ThreatManager.java - 波次生成
- [ ] HordeIntegrationManager.java - 动态触发
- [ ] DifficultyProvider.java - 难度压缩

### 配置文件
- [ ] TriAxisConfig.java - 新增参数
- [ ] DifficultyConfig.java - Forge配置
- [ ] 预设值调整

### 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 性能测试
- [ ] 平衡测试

### 文档
- [ ] 更新配置说明
- [ ] 更新玩家指南
- [ ] 更新开发文档

---

**准备好开始实施了吗？我可以帮你：**
1. 生成完整的代码实现
2. 创建配置文件模板
3. 编写测试用例
4. 制作数值平衡表

告诉我你想从哪个阶段开始！
