# 平衡性分析报告 - Deep Research 建议评估

基于 `deep-research-report.md` 的深度分析，本文档对比当前系统实现与建议修改，评估可行性和优先级。

---

## 执行摘要

Deep Research 报告基于 Factorio 污染机制进行了系统性对比分析，提出了多项数值平衡建议。总体评估：

- ✅ **污染系统**: 数值合理，但环境吸收可能过强
- ⚠️ **难度系统**: 全局倍率偏高，后期可能过于困难
- ⚠️ **威胁系统**: 触发概率偏低，可能缺乏紧迫感
- ✅ **炮塔系统**: 数值基本合理，火焰塔略强

---

## 1. 污染系统分析

### 1.1 污染产生速率

**当前实现** (`PollutionManager.java`):
```java
// 单机污染: 0.01 * (1 + tier * 0.5) /秒
// 多方块: ×3
// ULV=0.01/s, MV=0.02/s, EV=0.03/s, UHV=0.055/s
```

**报告评估**:
- 与 Factorio 石炉 (2 pu/min) 对比，MV 机器 1.2/min 略高但同数量级 ✅
- UHV 多方块 9.9/min 比 Factorio 高级化学厂 (4 pu/min) 略高 ⚠️

**建议修改**:
```
UHV: 0.055 → 0.06/s
```

**评估**: 
- **可行性**: 高 - 仅需修改 `TriAxisConfig` 中的系数
- **优先级**: 低 - 当前数值已经合理，微调影响不大
- **建议**: 保持当前值，等待实际测试反馈

---

### 1.2 环境衰减与吸收

**当前实现** (`PollutionManager.java:processPollutionDecayAndScanning`):
```java
// 自然衰减: 0.05/s per chunk
// 树叶吸收: 0.0001/叶 (5000叶 = 0.5/s)
// 水方块吸收: 0.00005/水 (1000水 = 0.05/s)
```

**报告评估**:
- 完整森林区块 (5000叶) 吸收 0.5/s，可能**过度抑制污染** ⚠️
- 若多机器产生 2-3/s，污染仍会积累，但速度较慢

**建议修改**:
```
自然衰减: 0.05 → 0.02-0.03/s
树叶吸收: 0.0001 → 0.00005/叶
```

**评估**:
- **可行性**: 高 - 配置参数调整
- **优先级**: 中 - 影响污染积累速度，进而影响威胁触发
- **建议**: **采纳** - 降低吸收效率可以让污染系统更有存在感

**实施方案**:
```properties
# config/triaxis-difficulty.properties
pollution.decay.natural=0.03  # 从 0.05 降低
pollution.absorption.leaves=0.00005  # 从 0.0001 降低
```

---

### 1.3 污染转化逻辑

**当前实现** (`PollutionManager.java:convertToPermanentPollution`):
```java
// 临时污染 > 200 时，每秒转化 0.1% 为永久污染
double converted = currentPollution * 0.001;
// 永久污染 ≥ 0.1 时转为 difficulty 并清零
if (permanentPollution >= 0.1) {
    double difficultyIncrease = permanentPollution * 0.1;
    permanentPollution = 0.0;
}
```

**报告评估**:
- "无限累积然后重置"机制**较复杂，不易直观理解** ⚠️
- 污染 200/s 时每秒转 0.2 点，全局 0.1 点时触发难度增幅

**建议修改**:
- 简化为 "每 X 污染触发 1 点难度"
- 或永久污染直接累积，无需清零

**评估**:
- **可行性**: 中 - 需要重构转化逻辑
- **优先级**: 低 - 当前逻辑虽复杂但功能正常
- **建议**: **暂不采纳** - 当前机制已经实现并测试，重构风险大于收益

**理由**:
1. 当前逻辑已经过测试，功能稳定
2. 复杂度主要在代码层面，玩家感知的是"污染高→难度增加"
3. 配置文件已有详细注释，开发者可理解
4. 若未来发现玩家反馈难以理解，再考虑简化

---

## 2. 难度系统分析

### 2.1 三轴权重与全局倍率

**当前实现** (`TriAxisDifficultyManager.java`):
```java
// 权重: Time=0.35, Voltage=0.35, Pollution=0.30
// 全局倍率: 2.8
// 最终难度 = (T*0.35 + V*0.35 + P*0.30) * 2.8
```

**报告评估**:
- 权重分配均衡 ✅
- **全局倍率 2.8 偏大**，导致中期难度系数 1.0 时最终 D≈2.8 ⚠️
- 怪物属性按 `1 + D × 系数` 会迅速飙高

**建议修改**:
```
全局倍率: 2.8 → 2.0-3.0 (根据测试调整)
```

**评估**:
- **可行性**: 高 - 单个配置参数
- **优先级**: 高 - 直接影响整体难度曲线
- **建议**: **采纳** - 降低到 2.0-2.5，观察测试反馈

**实施方案**:
```properties
# config/triaxis-difficulty.properties
difficulty.global_multiplier=2.0  # 从 2.8 降低
```

---

### 2.2 污染轴归一化

**当前实现** (`TriAxisDifficultyManager.java:calculatePollutionAxis`):
```java
// 总污染 / 100 线性归一，P ≥ 1 后饱和
double rawP = totalPollution / 100.0;
double P = Math.min(rawP, 1.0);
```

**报告评估**:
- 100 污染对应 P=1.0，之后饱和，**高污染环境失去难度增幅** ⚠️

**建议修改**:
```
归一化分母: 100 → 150-200
```

**评估**:
- **可行性**: 高 - 配置参数调整
- **优先级**: 中 - 影响后期污染系统的有效性
- **建议**: **采纳** - 提高到 150，让高污染区持续有效

**实施方案**:
```properties
# config/triaxis-difficulty.properties
pollution.normalization_divisor=150  # 从 100 提高
```

---

### 2.3 怪物属性倍率

**当前实现** (`IndustrialDifficultyGetter.java`):
```java
// HP: 1 + D × 2.2
// 攻击: 1 + D × 1.6
// 速度: 1 + D × 0.8
// 护甲: 1 + D × 0.6
```

**报告评估**:
- D=5 时，HP×12 (僵尸 240HP)，攻击×9 (伤害 45) **极端情况过强** ⚠️
- D=2-3 时，HP×5-7，攻击×4-5，尚可接受

**建议修改**:
```
HP: 2.2 → 1.5-2.0
攻击: 1.6 → 1.2-1.5
或引入硬上限 (如最高 20x 生命)
```

**评估**:
- **可行性**: 高 - 配置参数调整
- **优先级**: 高 - 直接影响战斗平衡
- **建议**: **部分采纳** - 降低系数 + 添加上限

**实施方案**:
```properties
# config/triaxis-difficulty.properties
difficulty.hp_multiplier=1.8  # 从 2.2 降低
difficulty.attack_multiplier=1.4  # 从 1.6 降低
difficulty.max_hp_multiplier=15.0  # 新增上限
difficulty.max_attack_multiplier=10.0  # 新增上限
```

**代码修改** (`IndustrialDifficultyGetter.java`):
```java
// 添加上限检查
double hpMult = Math.min(1 + difficulty * hpMultFactor, maxHpMult);
double attackMult = Math.min(1 + difficulty * attackMultFactor, maxAttackMult);
```

---

## 3. 威胁系统分析

### 3.1 阈值与生成概率

**当前实现** (`ThreatManager.java`):
```java
// MV (Tier 2): 污染 ≥ 50, 僵尸被动攻击机器
// HV (Tier 3+): 污染 ≥ 80, 1%/s 生成僵尸
//               污染 ≥ 120, 0.5%/s 生成 Creeper
//               污染 ≥ 200, 0.1%/s 生成闪电 Creeper
```

**报告评估**:
- 1%/秒 = 期望 100 秒 1 次，**相对稀疏** ⚠️
- 若玩家清理能力强，该频率可能感觉稀少

**建议修改**:
```
僵尸阈值: 80 → 60-80
与团队规模挂钩: 多人时提高概率
```

**评估**:
- **可行性**: 高 - 配置参数调整
- **优先级**: 中 - 影响游戏节奏和紧迫感
- **建议**: **采纳** - 降低阈值 + 提高概率

**实施方案**:
```properties
# config/triaxis-difficulty.properties
threat.zombie_spawn_threshold=70  # 从 80 降低
threat.zombie_spawn_chance=0.015  # 从 0.01 提高到 1.5%
threat.creeper_spawn_chance=0.008  # 从 0.005 提高到 0.8%
threat.charged_creeper_chance=0.002  # 从 0.001 提高到 0.2%
```

---

### 3.2 多人游戏缩放

**当前实现**: 无多人缩放机制

**建议修改**: 与团队规模挂钩

**评估**:
- **可行性**: 中 - 需要新增玩家计数逻辑
- **优先级**: 低 - 单人游戏优先，多人可后续优化
- **建议**: **延后实施** - 记录为 TODO

**实施方案** (未来):
```java
// ThreatManager.java
int nearbyPlayers = level.getEntitiesOfClass(Player.class, 
    new AABB(pos).inflate(64)).size();
double scaledChance = baseChance * Math.sqrt(nearbyPlayers);
```

---

## 4. 炮塔系统分析

### 4.1 激光炮塔

**当前实现** (`LaserTurretBlockEntity.java`):
```
Basic: 4 伤害, 50 tick, 18 格, DPS 1.6
Advanced: 8 伤害, 40 tick, 24 格, DPS 4.0
Elite: 12 伤害, 30 tick, 32 格, DPS 8.0
Ultimate: 16 伤害, 25 tick, 40 格, DPS 12.8
```

**报告评估**:
- DPS 1.6~9.7 (报告数据) vs 实际 1.6~12.8 ✅
- 数值与 Mekanism 激光炮塔相近，合理

**建议修改**: 可微调或保持

**评估**:
- **可行性**: 高
- **优先级**: 低 - 当前数值合理
- **建议**: **保持当前值**

---

### 4.2 火焰炮塔

**当前实现** (`FlamethrowerTurretBlockEntity.java`):
```
直击伤害: 5.0
地面燃烧: 2.0/tick (持续 5 秒)
射速: 4 发/秒
直击 DPS: 20
```

**报告评估**:
- 数值偏强，一座塔足以轻松清理低阶段虫群 ⚠️

**建议修改**:
```
直击伤害: 5 → 4
地面燃烧: 2 → 1.5/tick
或增加冷却: 5 tick → 10 tick
```

**评估**:
- **可行性**: 高 - 配置参数调整
- **优先级**: 中 - 影响防御平衡
- **建议**: **采纳** - 降低直击伤害

**实施方案**:
```properties
# config/triaxis-difficulty.properties (新增炮塔配置)
turret.flamethrower.direct_damage=4.0  # 从 5.0 降低
turret.flamethrower.ground_damage=1.5  # 从 2.0 降低
```

---

## 5. 优先级总结

### 高优先级 (立即实施)

1. **降低全局难度倍率**: 2.8 → 2.0-2.5
2. **降低怪物属性系数**: HP 2.2→1.8, 攻击 1.6→1.4
3. **添加属性上限**: HP ≤15x, 攻击 ≤10x

### 中优先级 (近期实施)

4. **降低环境吸收**: 自然衰减 0.05→0.03, 树叶 0.0001→0.00005
5. **提高污染归一化**: 100 → 150
6. **降低威胁阈值**: 僵尸 80→70, 提高生成概率
7. **平衡火焰塔**: 直击伤害 5→4

### 低优先级 (观察后决定)

8. UHV 污染速率微调
9. 污染转化逻辑简化
10. 多人游戏缩放机制

---

## 6. 配置文件修改建议

### 新增配置参数

```properties
# config/triaxis-difficulty.properties

# ===== 难度系统 =====
difficulty.global_multiplier=2.0
difficulty.hp_multiplier=1.8
difficulty.attack_multiplier=1.4
difficulty.max_hp_multiplier=15.0
difficulty.max_attack_multiplier=10.0

# ===== 污染系统 =====
pollution.decay.natural=0.03
pollution.absorption.leaves=0.00005
pollution.normalization_divisor=150

# ===== 威胁系统 =====
threat.zombie_spawn_threshold=70
threat.zombie_spawn_chance=0.015
threat.creeper_spawn_chance=0.008
threat.charged_creeper_chance=0.002

# ===== 炮塔系统 =====
turret.flamethrower.direct_damage=4.0
turret.flamethrower.ground_damage=1.5
```

---

## 7. 实施计划

### Phase 1: 配置系统扩展 (1-2 小时)

1. 在 `TriAxisConfig.java` 添加新配置参数
2. 更新 `load()` 和 `save()` 方法
3. 添加配置验证和默认值

### Phase 2: 难度系统调整 (1 小时)

1. 修改 `IndustrialDifficultyGetter.java` 添加属性上限
2. 更新 `TriAxisDifficultyManager.java` 使用新倍率
3. 调整污染归一化逻辑

### Phase 3: 威胁系统调整 (30 分钟)

1. 修改 `ThreatManager.java` 使用新阈值和概率
2. 更新威胁触发逻辑

### Phase 4: 炮塔系统调整 (30 分钟)

1. 修改 `FlamethrowerTurretBlockEntity.java` 使用新伤害值
2. 更新配置加载逻辑

### Phase 5: 测试与验证 (2-3 小时)

1. 单元测试: 验证配置加载和数值计算
2. 游戏测试: 各阶段难度曲线验证
3. 性能测试: 确保修改不影响性能

---

## 8. 风险评估

### 低风险

- 配置参数调整: 可随时回滚
- 数值平衡: 不影响核心逻辑

### 中风险

- 属性上限添加: 需要测试边界情况
- 污染归一化修改: 可能影响现有平衡

### 高风险

- 污染转化逻辑重构: **不建议实施**，风险大于收益

---

## 9. 测试检查清单

### 难度系统

- [ ] 早期 (0-10 天): 难度应在 0.5-1.0
- [ ] 中期 (MV, 20-30 台机器): 难度应在 1.5-2.5
- [ ] 后期 (HV, 50+ 台机器): 难度应在 3.0-4.0
- [ ] 极限 (IV+, 100+ 台机器): 难度应在 5.0-6.0 (不超过 8.0)

### 污染系统

- [ ] 森林区块污染积累速度合理
- [ ] 工业区污染能够触发威胁
- [ ] 永久污染转化正常工作

### 威胁系统

- [ ] MV 阶段 (污染 50-70) 出现僵尸攻击机器
- [ ] HV 阶段 (污染 70-120) 主动生成僵尸和 Creeper
- [ ] 极高污染 (200+) 出现闪电 Creeper
- [ ] 生成频率不会过于频繁或稀疏

### 炮塔系统

- [ ] 激光塔能够有效防御僵尸
- [ ] 火焰塔不会过于强大
- [ ] 能量消耗合理

---

## 10. 结论

Deep Research 报告提供了基于 Factorio 机制的系统性分析，大部分建议具有可行性。建议采纳的修改主要集中在：

1. **降低整体难度曲线** (全局倍率、属性系数)
2. **增强污染系统存在感** (降低吸收、提高归一化)
3. **提高威胁系统响应性** (降低阈值、提高概率)
4. **平衡炮塔系统** (火焰塔伤害)

不建议采纳的修改：

1. **污染转化逻辑重构** - 当前机制已稳定，重构风险大

建议分阶段实施，优先处理高优先级项目，通过测试验证后再进行中低优先级调整。

---

**文档版本**: 1.0  
**创建日期**: 2026-04-30  
**基于**: deep-research-report.md  
**状态**: 待审核
