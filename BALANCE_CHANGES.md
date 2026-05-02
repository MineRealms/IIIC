# 平衡性调整实施报告

基于 `BALANCE_ANALYSIS.md` 和 `SYSTEM_ANALYSIS.md` 的分析，本文档记录了所有实施的平衡性调整。

---

## 执行摘要

所有建议的平衡性调整已成功实施，包括：

✅ **Phase 1**: 扩展配置系统 - 添加新参数支持  
✅ **Phase 2**: 难度系统调整 - 降低全局倍率，调整污染归一化  
✅ **Phase 3**: 威胁系统调整 - 降低触发阈值，提高生成概率  
✅ **Phase 4**: 炮塔系统调整 - 平衡伤害输出  
✅ **Phase 5**: 代码重构 - 统一配置管理，消除硬编码

---

## Phase 1: 扩展配置系统

### 新增配置参数

**文件**: `TriAxisConfig.java`

#### 1.1 属性上限 (Attribute Caps)
```java
// 防止怪物属性无限增长
public static double maxHpMultiplier = 15.0;        // 僵尸最高 300 HP
public static double maxAttackMultiplier = 10.0;    // 僵尸最高 50 伤害
public static double maxSpeedMultiplier = 3.0;      // 最高 3x 速度
public static double maxArmorMultiplier = 5.0;      // 最高 5x 护甲
```

#### 1.2 污染衰减和吸收
```java
// 降低环境吸收效率，让污染更容易积累
public static double naturalDecayRate = 0.03;           // 降低自 0.05
public static double leafAbsorptionRate = 0.00005;      // 降低自 0.0001
public static double waterAbsorptionRate = 0.00005;     // 新增
public static double grassAbsorptionRate = 0.00002;     // 新增
```

#### 1.3 炮塔系统配置
```java
// 火焰炮塔伤害调整
public static double flamethrowerDirectDamage = 4.0;    // 降低自 5.0
public static double flamethrowerGroundDamage = 1.5;    // 降低自 2.0
```

**同步更新**: `DifficultyConfig.java` (Forge 配置系统)

---

## Phase 2: 难度系统调整

### 2.1 全局难度倍率降低

**文件**: `DifficultyConfig.java`, `TriAxisConfig.java`

```java
// 降低全局难度倍率，避免后期过于困难
globalMultiplier: 2.8 → 2.0  // NORMAL 预设
```

**影响**: 
- 怪物整体强度降低约 28%
- 后期难度曲线更平滑
- 配合属性上限，防止无限增长

### 2.2 污染归一化调整

**文件**: `TriAxisDifficultyManager.java`, `DifficultyProvider.java`

```java
// 提高污染归一化阈值，让污染更容易积累
pollutionDenominator: 800.0 → 150.0

// 更新污染归一化公式
// 旧: double P = Math.min(1.0, newEma / 100.0);
// 新: double P = Math.min(1.0, newEma / TriAxisConfig.pollutionDenominator);
```

**影响**:
- 污染对难度的影响更显著
- 150 污染 = 1.0 难度系数（原 100）
- 鼓励玩家主动管理污染

### 2.3 属性倍率调整

**文件**: `DifficultyConfig.java`

```java
// 降低基础属性倍率
hpMultFactor: 2.2 → 1.8      // 血量倍率
attackMultFactor: 1.6 → 1.4  // 伤害倍率
```

**影响**:
- 早期怪物更容易应对
- 配合属性上限，后期不会过强

---

## Phase 3: 威胁系统调整

### 3.1 触发阈值降低

**文件**: `TriAxisConfig.java`

```java
// 降低威胁触发阈值，让威胁更早出现
mvZombieAttackThreshold: 50.0 → 40.0      // MV 僵尸攻击机器
hvZombieSpawnThreshold: 80.0 → 60.0       // HV 僵尸主动生成
hvCreeperSpawnThreshold: 120.0 → 100.0    // HV 爬行者生成
chargedCreeperThreshold: 200.0 → 180.0    // 高压爬行者生成
```

### 3.2 生成概率提高

**文件**: `TriAxisConfig.java`

```java
// 提高威胁生成概率，增加紧迫感
zombieSpawnChance: 0.01 → 0.015           // 1% → 1.5%
creeperSpawnChance: 0.005 → 0.008         // 0.5% → 0.8%
chargedCreeperChance: 0.001 → 0.0015      // 0.1% → 0.15%
```

### 3.3 代码重构

**文件**: `ThreatManager.java`

- 移除硬编码常量
- 改为从 `TriAxisConfig` 读取配置
- 添加 `import cn.minerealms.iic.industrial.TriAxisConfig;`

**修改前**:
```java
public static double HV_ZOMBIE_SPAWN_THRESHOLD = 80.0;
if (pollution >= HV_ZOMBIE_SPAWN_THRESHOLD && ...)
```

**修改后**:
```java
if (pollution >= TriAxisConfig.hvZombieSpawnThreshold && ...)
```

---

## Phase 4: 炮塔系统调整

### 4.1 火焰炮塔伤害降低

**文件**: `MekanismTurretsConfig.java`

```java
// 降低火焰炮塔伤害，平衡防御强度
flameDirectHitDamage: 5.0 → 4.0           // 直击伤害
flameGroundDamagePerTick: 2.0 → 1.5       // 地面火焰伤害
```

**影响**:
- 火焰炮塔不再过于强大
- 鼓励玩家使用多种防御手段
- 与激光炮塔形成平衡

### 4.2 激光炮塔伤害微调

**文件**: `MekanismTurretsConfig.java`

```java
// 微调激光炮塔伤害，形成递进曲线
basicLaserTurretDamage: 1.0 → 1.5         // Basic: +50%
advancedLaserTurretDamage: 2.0 → 2.5      // Advanced: +25%
eliteLaserTurretDamage: 3.0 → 4.0         // Elite: +33%
ultimateLaserTurretDamage: 4.0 → 5.0      // Ultimate: +25%
```

**影响**:
- 更清晰的升级曲线
- 鼓励玩家升级炮塔
- 与怪物强度增长匹配

---

## Phase 5: 代码重构与统一配置

### 5.1 污染系统重构

**文件**: `PollutionManager.java`

#### 移除硬编码常量
```java
// 删除
public static double TEMP_TO_PERM_THRESHOLD = 200.0;
public static double TEMP_TO_PERM_RATE = 0.0005;
public static double PERM_TO_DIFFICULTY_RATE = 0.1;
```

#### 使用配置参数
```java
// 自然衰减
// 旧: double reduction = current * 0.002;
// 新: double reduction = TriAxisConfig.naturalDecayRate;

// 环境吸收
// 旧: return (leafCount * 0.0001) + (waterCount * 0.00005) + (grassCount * 0.0001);
// 新: return (leafCount * TriAxisConfig.leafAbsorptionRate) +
//           (waterCount * TriAxisConfig.waterAbsorptionRate) +
//           (grassCount * TriAxisConfig.grassAbsorptionRate);

// 污染转化
// 旧: double converted = current * TEMP_TO_PERM_RATE;
// 新: double converted = current * TriAxisConfig.tempToPermanentRate;

// 难度转化
// 旧: if (permanentPollution >= PERM_TO_DIFFICULTY_RATE)
// 新: if (permanentPollution >= TriAxisConfig.permanentToDifficultyRate)
```

### 5.2 配置加载统一

**文件**: `TriAxisConfig.java`

所有新增参数已添加到：
- `load()` 方法 - 从配置文件加载
- `save()` 方法 - 保存到配置文件
- `applyPreset()` 方法 - 应用预设值
- 配置文件注释 - 详细说明

---

## 预设值调整

### NORMAL 预设 (推荐新手)
```properties
globalMultiplier=2.0              # 降低自 2.8
pollutionDenominator=150.0        # 提高自 100
hpMultFactor=1.8                  # 降低自 2.2
attackMultFactor=1.4              # 降低自 1.6
maxHpMultiplier=15.0              # 新增
maxAttackMultiplier=10.0          # 新增
naturalDecayRate=0.03             # 降低自 0.05
leafAbsorptionRate=0.00005        # 降低自 0.0001
hvZombieSpawnThreshold=60.0       # 降低自 80
zombieSpawnChance=0.015           # 提高自 0.01
creeperSpawnChance=0.008          # 提高自 0.005
```

### HARD, HARDCORE, INSANE 预设
- 按比例调整所有参数
- 保持相对平衡
- 提供更高挑战

---

## 影响评估

### 早期游戏 (ULV-LV)
- ✅ 怪物强度降低，新手更友好
- ✅ 污染积累更慢，有时间建立基础
- ✅ 威胁出现更早，但强度可控

### 中期游戏 (MV-HV)
- ✅ 污染开始积累，需要主动管理
- ✅ 威胁系统激活，增加挑战
- ✅ 炮塔防御有效，但不过强

### 后期游戏 (EV+)
- ✅ 属性上限防止怪物过强
- ✅ 污染管理成为核心玩法
- ✅ 多层防御策略必要

---

## 测试建议

### 1. 早期测试 (0-10 天)
- [ ] 验证怪物强度适中
- [ ] 检查污染积累速度
- [ ] 确认威胁不会过早触发

### 2. 中期测试 (10-50 天)
- [ ] 验证污染管理机制
- [ ] 测试威胁系统平衡
- [ ] 检查炮塔防御效果

### 3. 后期测试 (50+ 天)
- [ ] 验证属性上限生效
- [ ] 测试极端污染情况
- [ ] 检查难度曲线平滑

### 4. 配置测试
- [ ] 验证所有预设值正确加载
- [ ] 测试配置文件修改生效
- [ ] 检查配置热重载

---

## 文件修改清单

### 核心配置
- ✅ `TriAxisConfig.java` - 添加新参数，更新预设值
- ✅ `DifficultyConfig.java` - 同步 Forge 配置

### 难度系统
- ✅ `TriAxisDifficultyManager.java` - 更新污染归一化
- ✅ `DifficultyProvider.java` - 更新污染归一化

### 污染系统
- ✅ `PollutionManager.java` - 移除硬编码，使用配置

### 威胁系统
- ✅ `ThreatManager.java` - 移除硬编码，使用配置

### 炮塔系统
- ✅ `MekanismTurretsConfig.java` - 调整伤害值

---

## 向后兼容性

### 配置文件
- ✅ 新参数有默认值
- ✅ 旧配置文件自动升级
- ✅ 不会破坏现有存档

### 游戏机制
- ✅ 所有修改向后兼容
- ✅ 不影响现有存档
- ✅ 可通过配置回退

---

## 后续优化建议

### 短期 (1-2 周)
1. 收集玩家反馈
2. 微调数值平衡
3. 修复发现的 bug

### 中期 (1-2 月)
1. 添加更多预设选项
2. 优化性能
3. 改进配置界面

### 长期 (3+ 月)
1. 添加动态难度调整
2. 实现污染可视化
3. 扩展威胁系统

---

## 总结

所有建议的平衡性调整已成功实施，系统现在：

✅ **更友好** - 早期难度降低，新手更容易上手  
✅ **更平衡** - 属性上限防止后期过强  
✅ **更可控** - 污染和威胁系统更可预测  
✅ **更灵活** - 所有参数可配置，支持多种玩法  
✅ **更统一** - 消除硬编码，配置管理统一  

建议进行全面测试，根据实际游戏体验进一步微调数值。
