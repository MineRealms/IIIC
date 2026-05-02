# ImprovedMobs 工业集成系统 - 数值分析文档

## 系统架构概览

### 核心系统
1. **污染系统 (PollutionManager)** - 双层污染机制
2. **难度系统 (DifficultyManager + TriAxisDifficultyManager)** - 三轴难度计算
3. **威胁系统 (ThreatManager)** - 基于污染和电压的怪物生成
4. **炮塔系统** - 激光炮塔 + 火焰炮塔防御

---

## 1. 污染系统 (Pollution System)

### 1.1 双层污染设计

#### 临时污染 (Temporary Pollution)
- **存储**: 区块级别 (`Map<ChunkPos, Double>`)
- **产生源**: GregTech 机器运行
- **扩散**: 向相邻区块扩散
- **衰减**: 可被环境吸收（树叶、水、草）
- **作用**: 触发怪物攻击机器，不直接影响难度

#### 永久污染 (Permanent Pollution)
- **存储**: 全局单一数值 (`double permanentPollution`)
- **转化**: 临时污染超过阈值时转化
- **作用**: 直接增加 ImprovedMobs 全局难度
- **不可逆**: 代表长期环境破坏

### 1.2 污染产生数值

#### 机器污染公式
```java
// 基础公式: 0.01 * (1 + tier * 0.5) 污染/秒
// tier 0 (ULV) = 0.01/s
// tier 2 (MV)  = 0.02/s  
// tier 4 (EV)  = 0.03/s
// tier 6 (LuV) = 0.04/s
// tier 9 (UHV) = 0.055/s

// 多方块结构 ×3 倍
if (isMultiblock) {
    pollutionValue *= 3.0;
}
```

#### 实际污染速率表
| 电压等级 | Tier | 单机器 (污染/秒) | 多方块 (污染/秒) |
|---------|------|----------------|----------------|
| ULV     | 0    | 0.010          | 0.030          |
| LV      | 1    | 0.015          | 0.045          |
| MV      | 2    | 0.020          | 0.060          |
| HV      | 3    | 0.025          | 0.075          |
| EV      | 4    | 0.030          | 0.090          |
| IV      | 5    | 0.035          | 0.105          |
| LuV     | 6    | 0.040          | 0.120          |
| ZPM     | 7    | 0.045          | 0.135          |
| UV      | 8    | 0.050          | 0.150          |
| UHV     | 9    | 0.055          | 0.165          |

### 1.3 污染衰减数值

#### 自然衰减
```java
// 基础衰减: 0.05/秒
double reduction = 0.05;
```

#### 环境吸收
```java
// 每个方块的吸收率 (污染/秒)
leafCount * 0.0001      // 树叶: 0.0001
waterCount * 0.00005    // 水: 0.00005
grassCount * 0.0001     // 草/花: 0.0001

// 扫描范围: 中心区块 ±4 区块 (9×9 区块范围)
```

#### 环境吸收示例
- **1000 片树叶** = 0.1 污染/秒吸收
- **2000 个水方块** = 0.1 污染/秒吸收
- **完整森林区块** (约 5000 树叶) = 0.5 污染/秒吸收

### 1.4 污染转化数值

#### 临时 → 永久污染
```java
TEMP_TO_PERM_THRESHOLD = 200.0;  // 转化阈值
TEMP_TO_PERM_RATE = 0.001;       // 转化率 (0.1%)

// 当临时污染 > 200 时:
converted = current * 0.001;  // 每秒转化 0.1%
```

#### 永久污染 → 难度
```java
PERM_TO_DIFFICULTY_RATE = 0.1;

// 当永久污染 ≥ 0.1 时:
addDifficulty(permanentPollution);  // 添加到全局难度
permanentPollution = 0.0;           // 重置
```

---

## 2. 难度系统 (Difficulty System)

### 2.1 三轴难度模型

#### 公式
```java
D = globalMultiplier × (weightTime × T + weightVoltage × V + weightPollution × P)
```

#### 默认权重
```java
weightTime = 0.35       // 时间因子权重
weightVoltage = 0.35    // 电压因子权重
weightPollution = 0.30  // 污染因子权重
globalMultiplier = 2.8  // 全局倍率
```

### 2.2 时间轴 (Time Axis - T)

#### 计算公式
```java
mcDays = level.getDayTime() / 24000L;
tRaw = log(1 + mcDays / baseDays) / log(1 + targetDays / baseDays);
T = clamp(tRaw, 0.0, 1.0);

// 默认参数
baseDays = 30.0      // 基础天数
targetDays = 1200.0  // 目标天数 (达到峰值)
```

#### 时间难度曲线
| MC 天数 | T 值  | 贡献难度 (×0.35×2.8) |
|--------|-------|---------------------|
| 0      | 0.00  | 0.00                |
| 30     | 0.15  | 0.15                |
| 100    | 0.30  | 0.29                |
| 300    | 0.50  | 0.49                |
| 600    | 0.65  | 0.64                |
| 1200   | 1.00  | 0.98                |

### 2.3 电压轴 (Voltage Axis - V)

#### 计算公式
```java
medianTier = scanNearbyVoltageTierMedian(center, 64 blocks);
V = clamp(medianTier / maxIndustrialTier, 0.0, 1.0);

// 默认参数
maxIndustrialTier = 9  // UHV
scanRadiusBlocks = 64  // 扫描半径
```

#### 电压难度表
| 电压等级 | Tier | V 值  | 贡献难度 (×0.35×2.8) |
|---------|------|-------|---------------------|
| ULV     | 0    | 0.00  | 0.00                |
| LV      | 1    | 0.11  | 0.11                |
| MV      | 2    | 0.22  | 0.22                |
| HV      | 3    | 0.33  | 0.32                |
| EV      | 4    | 0.44  | 0.43                |
| IV      | 5    | 0.56  | 0.55                |
| LuV     | 6    | 0.67  | 0.66                |
| ZPM     | 7    | 0.78  | 0.76                |
| UV      | 8    | 0.89  | 0.87                |
| UHV     | 9    | 1.00  | 0.98                |

### 2.4 污染轴 (Pollution Axis - P)

#### 计算公式
```java
localPollution = getTemporaryPollution(chunkPos);
globalPollution = getPermanentPollution();
totalPollution = localPollution + globalPollution;

// EMA 平滑
newEma = (emaAlpha * totalPollution) + ((1 - emaAlpha) * currentEma);
P = min(1.0, newEma / 100.0);  // 线性归一化

// 默认参数
emaAlpha = 0.05  // EMA 平滑系数
```

#### 污染难度表
| 总污染 | P 值  | 贡献难度 (×0.30×2.8) |
|-------|-------|---------------------|
| 0     | 0.00  | 0.00                |
| 25    | 0.25  | 0.21                |
| 50    | 0.50  | 0.42                |
| 75    | 0.75  | 0.63                |
| 100   | 1.00  | 0.84                |
| 150   | 1.00  | 0.84                |
| 200+  | 1.00  | 0.84                |

### 2.5 玩家工业加成 (Player Industrial Bonus)

#### 计算公式
```java
// 扫描玩家周围 32 格机器
medianTier = weightedMedian(tiers, weights);
techScore = min(1.0, medianTier / maxIndustrialTier);
pollutionScore = 1.0 - exp(-pollutionEMA / pollutionDenominator);

industrialBonus = techWeight × techScore + pollutionWeight × pollutionScore;

// 默认参数
techWeight = 30.0              // 科技权重
pollutionWeight = 18.0         // 污染权重
pollutionDenominator = 800.0   // 污染饱和阈值
```

#### 工业加成表
| 场景 | 中位 Tier | 污染 | 科技分 | 污染分 | 总加成 |
|-----|----------|------|--------|--------|--------|
| 早期 LV | 1 | 10 | 3.3 | 0.22 | 3.5 |
| MV 基地 | 2 | 50 | 6.7 | 1.08 | 7.8 |
| HV 工厂 | 3 | 120 | 10.0 | 2.52 | 12.5 |
| EV 产线 | 4 | 200 | 13.3 | 4.02 | 17.3 |
| IV+ 大型 | 5 | 300 | 16.7 | 5.76 | 22.5 |

### 2.6 怪物属性倍率

#### 基础倍率公式
```java
hpMultiplier = 1.0 + (difficulty × hpMultFactor);
attackMultiplier = 1.0 + (difficulty × attackMultFactor);
speedMultiplier = 1.0 + (difficulty × speedMultFactor);
armorMultiplier = 1.0 + (difficulty × armorMultFactor);

// 默认参数
hpMultFactor = 2.2
attackMultFactor = 1.6
speedMultFactor = 0.8
armorMultFactor = 1.2
```

#### 电压等级 HP 目标倍率
| 电压等级 | HP 倍率 | 僵尸 HP (基础 20) |
|---------|---------|------------------|
| ULV     | 1.0×    | 20               |
| LV      | 1.3×    | 26               |
| MV      | 1.8×    | 36               |
| HV      | 2.5×    | 50               |
| EV      | 3.5×    | 70               |
| IV      | 4.8×    | 96               |
| LuV     | 6.5×    | 130              |
| ZPM     | 8.5×    | 170              |
| UV      | 11.0×   | 220              |
| UHV     | 14.0×   | 280              |

#### 难度 5.0 时的怪物属性
```java
difficulty = 5.0

HP:     1.0 + (5.0 × 2.2) = 12.0× (僵尸 240 HP)
攻击:   1.0 + (5.0 × 1.6) = 9.0×  (僵尸 45 伤害)
速度:   1.0 + (5.0 × 0.8) = 5.0×
护甲:   1.0 + (5.0 × 1.2) = 7.0×
```

---

## 3. 威胁系统 (Threat System)

### 3.1 威胁阈值

```java
MV_ZOMBIE_ATTACK_THRESHOLD = 50.0    // MV: 僵尸攻击机器
HV_ZOMBIE_SPAWN_THRESHOLD = 80.0     // HV: 主动生成僵尸
HV_CREEPER_SPAWN_THRESHOLD = 120.0   // HV: 主动生成苦力怕
CHARGED_CREEPER_THRESHOLD = 200.0    // 极高污染: 闪电苦力怕
```

### 3.2 生成概率

```java
ZOMBIE_SPAWN_CHANCE = 0.01        // 1% 每秒
CREEPER_SPAWN_CHANCE = 0.005      // 0.5% 每秒
CHARGED_CREEPER_CHANCE = 0.001    // 0.1% 每秒
```

### 3.3 威胁触发条件

| 阶段 | 电压等级 | 污染阈值 | 威胁类型 | 概率/秒 | 期望生成间隔 |
|-----|---------|---------|---------|---------|-------------|
| MV  | Tier 2+ | ≥50     | 僵尸攻击机器 (被动) | - | - |
| HV  | Tier 3+ | ≥80     | 主动生成僵尸 | 1% | 100 秒 |
| HV  | Tier 3+ | ≥120    | 主动生成苦力怕 | 0.5% | 200 秒 |
| 极限 | Tier 3+ | ≥200    | 闪电苦力怕 | 0.1% | 1000 秒 |

### 3.4 AI 优先级

```java
// 僵尸攻击机器 AI
zombie.goalSelector.addGoal(1, new ZombieDestroyMachineGoal(zombie));

// 苦力怕攻击机器 AI
creeper.goalSelector.addGoal(1, new CreeperTargetMachineGoal(creeper));

// 优先级 1 = 最高优先级，优先于攻击玩家
```

---

## 4. 炮塔系统 (Turret System)

### 4.1 激光炮塔 (Laser Turrets)

#### 数值表
| 等级 | 伤害 | 冷却 (tick) | 射程 (格) | 能量容量 (FE) | DPS |
|-----|------|------------|----------|--------------|-----|
| Basic | 4.0 | 50 (2.5s) | 15 | 10,000 | 1.6 |
| Advanced | 8.0 | 40 (2.0s) | 25 | 40,000 | 4.0 |
| Elite | 12.0 | 30 (1.5s) | 35 | 90,000 | 8.0 |
| Ultimate | 17.0 | 35 (1.75s) | 45 | 160,000 | 9.7 |

#### 配置代码
```java
// LaserDamageConfig.java
basicDamage = 4.0F
advancedDamage = 8.0F
eliteDamage = 12.0F
ultimateDamage = 17.0F

// MekanismTurretsConfig.java
basicLaserTurretCooldown = 50
basicLaserTurretRange = 15D
basicLaserTurretEnergyCapacity = 10000
// ... (其他等级类似)
```

### 4.2 火焰炮塔 (Flamethrower Turret)

#### 数值表
| 参数 | 数值 | 说明 |
|-----|------|------|
| 直击伤害 | 5.0 | 火焰弹直接命中 |
| 地面伤害/tick | 2.0 | 持续燃烧伤害 |
| 燃烧持续时间 | 100 tick (5秒) | 地面火焰持续 |
| 伤害半径 | 3.0 格 | AOE 范围 |
| 射程 | 32.0 格 | 最大射程 |
| 冷却 | 5 tick (0.25s) | 射击间隔 |
| 燃料容量 | 16,000 mB | 储罐容量 |
| 燃料消耗 | 10 mB/shot | 每次射击 |

#### DPS 计算
```java
// 单目标持续伤害
directHit = 5.0 (一次性)
groundDamage = 2.0 × 100 tick = 200.0 (5秒内)
totalDamage = 205.0

// 射速
shotsPerSecond = 20 / 5 = 4
sustainedDPS = 4 × 5.0 = 20.0 (仅直击)

// AOE 伤害 (3格半径内所有敌人)
aoeMultiplier = π × 3² ≈ 28 格²
potentialDPS = 20.0 × (敌人数量)
```

#### 配置代码
```java
// MekanismTurretsConfig.java
flameThrowerTurretCooldown = 5
flameThrowerTurretDamage = 1.0  // 每 tick 伤害
flameThrowerTurretFuelCapacity = 16000
flameThrowerTurretRange = 32.0
flameThrowerTurretFuelPerShot = 10

flameDirectHitDamage = 5.0
flameGroundDamagePerTick = 2.0
flameGroundDuration = 100
flameDamageRadius = 3.0
```

---

## 5. 游戏阶段平衡分析

### 5.1 MV 阶段 (20-30 台机器)

#### 污染产生
```
20 台 MV 单机器: 20 × 0.02 = 0.4 污染/秒
30 台 MV 单机器: 30 × 0.02 = 0.6 污染/秒
稳态污染: 30-60 (考虑衰减)
```

#### 难度贡献
```
电压轴 (V=0.22): 0.22 × 0.35 × 2.8 = 0.22
污染轴 (P=0.50): 0.50 × 0.30 × 2.8 = 0.42
时间轴 (T=0.30): 0.30 × 0.35 × 2.8 = 0.29
总难度: 0.93
```

#### 威胁等级
- **污染 ≥50**: 僵尸开始攻击机器 (被动)
- **无主动生成**: HV 阶段才触发
- **防御需求**: Basic 激光炮塔 (DPS 1.6) 足够

### 5.2 HV 阶段 (40-60 台机器)

#### 污染产生
```
40 台 HV 单机器: 40 × 0.025 = 1.0 污染/秒
10 台 HV 多方块: 10 × 0.075 = 0.75 污染/秒
总产生: 1.75 污染/秒
稳态污染: 80-150
```

#### 难度贡献
```
电压轴 (V=0.33): 0.33 × 0.35 × 2.8 = 0.32
污染轴 (P=1.00): 1.00 × 0.30 × 2.8 = 0.84
时间轴 (T=0.50): 0.50 × 0.35 × 2.8 = 0.49
总难度: 1.65
```

#### 威胁等级
- **污染 ≥80**: 主动生成僵尸 (1%/秒, 期望 100 秒)
- **污染 ≥120**: 主动生成苦力怕 (0.5%/秒, 期望 200 秒)
- **防御需求**: Advanced 激光炮塔 (DPS 4.0) + 火焰炮塔 (AOE)

### 5.3 IV-LuV 阶段 (100+ 台机器)

#### 污染产生
```
60 台 EV 单机器: 60 × 0.03 = 1.8 污染/秒
20 台 IV 多方块: 20 × 0.105 = 2.1 污染/秒
总产生: 3.9 污染/秒
稳态污染: 150-300
```

#### 难度贡献
```
电压轴 (V=0.56): 0.56 × 0.35 × 2.8 = 0.55
污染轴 (P=1.00): 1.00 × 0.30 × 2.8 = 0.84
时间轴 (T=0.65): 0.65 × 0.35 × 2.8 = 0.64
总难度: 2.03
```

#### 威胁等级
- **污染 ≥200**: 闪电苦力怕 (0.1%/秒, 期望 1000 秒)
- **永久污染转化**: 开始累积全局难度
- **防御需求**: Elite 激光炮塔 (DPS 8.0) + 多层防御

### 5.4 ZPM-UHV 阶段 (200+ 台机器)

#### 污染产生
```
100 台 LuV 单机器: 100 × 0.04 = 4.0 污染/秒
50 台 ZPM 多方块: 50 × 0.135 = 6.75 污染/秒
总产生: 10.75 污染/秒
稳态污染: 300-500+
```

#### 难度贡献
```
电压轴 (V=0.89): 0.89 × 0.35 × 2.8 = 0.87
污染轴 (P=1.00): 1.00 × 0.30 × 2.8 = 0.84
时间轴 (T=1.00): 1.00 × 0.35 × 2.8 = 0.98
总难度: 2.69
永久污染累积: +0.5~1.0/分钟
```

#### 威胁等级
- **极限威胁**: 所有威胁类型高频触发
- **全局难度飙升**: 永久污染快速累积
- **防御需求**: Ultimate 激光炮塔 (DPS 9.7) + 密集炮塔网

---

## 6. 关键数值总结

### 6.1 污染系统
- **机器污染**: 0.01~0.055/秒 (单机), 0.03~0.165/秒 (多方块)
- **自然衰减**: 0.05/秒
- **环境吸收**: 0.0001/方块 (树叶/草), 0.00005/方块 (水)
- **转化阈值**: 200 (临时→永久)
- **转化率**: 0.1%/秒

### 6.2 难度系统
- **三轴权重**: 时间 0.35, 电压 0.35, 污染 0.30
- **全局倍率**: 2.8
- **时间曲线**: 1200 天达到峰值
- **电压归一化**: Tier 9 (UHV) = 1.0
- **污染归一化**: 100 污染 = 1.0

### 6.3 威胁系统
- **MV 阈值**: 50 (被动攻击)
- **HV 阈值**: 80 (僵尸), 120 (苦力怕)
- **极限阈值**: 200 (闪电苦力怕)
- **生成概率**: 1% (僵尸), 0.5% (苦力怕), 0.1% (闪电)

### 6.4 炮塔系统
- **激光 DPS**: 1.6 → 4.0 → 8.0 → 9.7
- **火焰 DPS**: 20.0 (单目标), AOE 3格半径
- **射程**: 15 → 25 → 35 → 45 (激光), 32 (火焰)

---

## 7. 配置文件位置

### 主配置
- `config/iic-common.toml` - 所有系统配置
  - `[pollution]` - 污染系统参数
  - `[difficulty]` - 难度计算参数
  - `[threat]` - 威胁系统参数
  - `[horde]` - Horde 集成参数

### 炮塔配置
- `config/mekanism_turrets-common.toml` - 炮塔参数

### 代码配置类
- `DifficultyConfig.java` - 难度系统
- `PollutionConfig.java` - 污染系统
- `ThreatConfig.java` - 威胁系统
- `MekanismTurretsConfig.java` - 炮塔系统

---

## 8. 与其他模组的联动

### 8.1 ImprovedMobs 联动
- **难度提供者**: `DifficultyProvider` 实现 `DifficultyGetter` 接口
- **难度注入**: 通过 `DifficultyFetcher.add()` 注册
- **属性增强**: HP, 攻击, 速度, 护甲倍率
- **AI 增强**: 攻击机器 AI (优先级 1)

### 8.2 Spore 联动
- **污染反馈**: 高污染加速 Hivemind 生长
- **触发阈值**: 总污染 > 100
- **反馈机制**: `SporeIntegration.applyPollutionFeedback()`

### 8.3 GregTech CEu 联动
- **机器检测**: 反射检测 `MetaMachine` 和 `MetaMachineBlockEntity`
- **电压等级**: 支持 ULV~UHV (Tier 0~9)
- **多方块支持**: 从 Energy Hatch 读取电压等级
- **污染源**: GT 污染系统集成 (`GTPollutionScanner`)

### 8.4 The Hordes 联动
- **污染触发**: 污染 ≥150 触发 Horde
- **难度转换**: Difficulty → Horde Intensity
- **机器目标**: Horde 僵尸攻击机器
- **电压缩放**: 基于电压等级调整 Horde 强度

---

**文档版本**: 1.0  
**生成时间**: 2026-04-30  
**适用版本**: ImprovedMobs 1.20.1 (IIC 集成)
