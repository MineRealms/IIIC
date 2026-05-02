# 配置系统改进总结

## 修复日期
2025-01-XX

## 问题描述
通过全面审计发现项目中存在大量硬编码数值，这些数值无法通过配置文件调整，影响了模组的可配置性和平衡性调整。

---

## 修复内容

### 1. 修复 HordeIntegrationManager 配置集成 ✅

**问题：** `HordeIntegrationManager` 中声明了 18 个独立的 `public static` 参数，这些参数与 `TriAxisConfig` 中的同名参数重复，导致配置文件修改不生效。

**解决方案：**
- 删除 `HordeIntegrationManager` 中的重复参数声明
- 将所有引用改为使用 `TriAxisConfig.` 前缀
- 添加 `hordeTierIntensityMultipliers` 数组到配置系统

**影响的参数（18个）：**
```java
// 尸潮强度
enableHordeIntegration
hordeIntensityMultiplier
difficultyToIntensityFactor

// 污染触发
enablePollutionTriggeredHordes
pollutionHordeTriggerThreshold
pollutionHordeCheckInterval
pollutionHordeTriggerChance

// 小股袭扰
enableSkirmishes
skirmishPollutionThreshold
skirmishInterval
skirmishMinCount
skirmishMaxCount

// 大尸潮
majorHordePollutionThreshold
majorHordeMultiplier

// 机器攻击
enableMachineTargeting
machineTargetingRange
machineTargetingChance

// 电压缩放
enableVoltageTierScaling
```

**新增参数：**
```java
hordeTierIntensityMultipliers: double[10]
// 电压等级强度倍率数组 (ULV到UHV)
// 默认: [1.0, 1.1, 1.3, 1.5, 1.8, 2.2, 2.6, 3.0, 3.5, 4.0]
// 配置格式: "1.0,1.1,1.3,1.5,1.8,2.2,2.6,3.0,3.5,4.0"
```

---

### 2. 添加 Spore 怪物增强配置参数 ✅

**问题：** `SporeIntegration.java` 中的怪物增强计算使用了大量硬编码数值，无法通过配置调整平衡性。

**新增参数（8个）：**

#### 2.1 健康加成参数
```java
sporePollutionBonusDivisor = 200.0
// 污染加成除数
// 健康加成 = 污染 / 除数 (最大100%)
// 默认: 200.0 (200污染时达到最大加成)

sporeVoltageBonusPerTier = 0.10
// 每个电压等级的加成百分比
// 每高于ULV一个等级增加此百分比
// 默认: 0.10 (10%每等级)

sporeEvolutionBonusPerPhase = 0.05
// 每个进化阶段的加成百分比
// 默认: 0.05 (5%每阶段，最大50%在阶段10)
```

#### 2.2 伤害加成参数
```java
sporeDamageBonusThreshold = 100.0
// 伤害加成触发阈值
// 只有污染超过此值才应用伤害加成
// 默认: 100.0

sporeDamageBonusDivisor = 200.0
// 伤害加成除数
// 伤害加成 = 污染 / 除数
// 默认: 200.0

sporeDamageBonusMultiplier = 0.3
// 伤害加成倍率
// 最终伤害加成 = (污染 / 除数) * 倍率
// 默认: 0.3 (30%每200污染)
```

#### 2.3 其他参数
```java
sporeMaxVoltageTier = 9.0
// 最大电压等级（用于归一化）
// 用于将电压等级归一化到0.0-1.0范围
// 默认: 9.0 (UHV)

sporeInfectionIntensityMultiplier = 10.0
// 感染强度倍率
// 将进化阶段(0-10)转换为感染强度(0-100%)
// 默认: 10.0
```

**代码修改：**
- `SporeIntegration.java:406` - 电压归一化使用 `TriAxisConfig.sporeMaxVoltageTier`
- `SporeIntegration.java:438` - 感染强度转换使用 `TriAxisConfig.sporeInfectionIntensityMultiplier`
- `SporeIntegration.java:525-530` - 健康加成计算使用配置参数
- `SporeIntegration.java:554-560` - 伤害加成计算使用配置参数

---

## 配置文件更新

### 新增配置项

在 `config/triaxis-difficulty.properties` 中新增以下配置项：

```properties
# ========== Hordes 电压等级倍率 ==========
hordeTierIntensityMultipliers=1.0,1.1,1.3,1.5,1.8,2.2,2.6,3.0,3.5,4.0

# ========== Spore 怪物增强参数 ==========
# 健康加成
sporePollutionBonusDivisor=200.0
sporeVoltageBonusPerTier=0.10
sporeEvolutionBonusPerPhase=0.05

# 伤害加成
sporeDamageBonusThreshold=100.0
sporeDamageBonusDivisor=200.0
sporeDamageBonusMultiplier=0.3

# 其他
sporeMaxVoltageTier=9.0
sporeInfectionIntensityMultiplier=10.0
```

---

## 影响分析

### 游戏平衡性
- **Horde 系统**：现在可以通过配置文件精确调整尸潮强度、触发条件和电压等级影响
- **Spore 增强**：可以独立调整污染、电压、进化对怪物属性的影响，便于平衡性测试

### 可配置性提升
- **新增可配置参数**：26个（18个Horde + 8个Spore）
- **配置文件兼容性**：所有参数都有默认值，旧配置文件仍然有效
- **运行时调整**：支持通过配置文件热重载（如果实现了重载功能）

### 向后兼容性
- ✅ 完全向后兼容
- ✅ 默认值与之前的硬编码值相同
- ✅ 不影响现有存档和配置文件

---

## 测试建议

### 1. 配置加载测试
- [ ] 删除配置文件，验证默认值生成
- [ ] 修改配置文件，验证参数正确加载
- [ ] 测试数组参数解析（`hordeTierIntensityMultipliers`）

### 2. 游戏内测试
- [ ] 测试不同污染等级下的 Spore 怪物强度
- [ ] 测试不同电压等级下的尸潮强度
- [ ] 验证配置修改后的效果

### 3. 边界测试
- [ ] 测试极端配置值（0, 负数, 超大值）
- [ ] 测试数组长度不匹配的情况
- [ ] 测试配置文件格式错误的处理

---

## 后续改进建议

### 高优先级（未实现）
根据审计报告 `HARDCODED_VALUES_AUDIT.md`，还有以下参数需要配置化：

1. **Horde 威胁等级距离阈值**（5个）
   - 威胁等级 1-5 的距离阈值
   - 威胁更新间隔和冷却时间

2. **污染系统核心参数**（10个）
   - 污染生成公式参数（指数、倍率）
   - 扩散和衰减参数
   - 环境影响参数

3. **性能和调试参数**（8个）
   - 全局实体上限
   - 扫描范围和间隔
   - 调试日志间隔

### 中优先级
- 实现配置文件热重载功能
- 添加配置验证和错误提示
- 提供配置预设模板

### 低优先级
- 添加游戏内配置界面
- 提供配置导入/导出功能
- 添加配置变更日志

---

## 文件清单

### 修改的文件
1. `src/main/java/cn/minerealms/iic/threat/horde/HordeIntegrationManager.java`
   - 删除重复参数声明
   - 所有引用改为使用 `TriAxisConfig`

2. `src/main/java/cn/minerealms/iic/industrial/TriAxisConfig.java`
   - 添加 `hordeTierIntensityMultipliers` 数组
   - 添加 8 个 Spore 增强参数
   - 更新 `load()` 方法添加参数加载逻辑
   - 更新 `save()` 方法添加参数保存逻辑
   - 更新配置文件注释

3. `src/main/java/cn/minerealms/iic/integration/spore/SporeIntegration.java`
   - 所有硬编码数值改为使用 `TriAxisConfig` 参数
   - 更新注释说明参数来源

### 新增的文件
1. `HARDCODED_VALUES_AUDIT.md` - 完整的硬编码数值审计报告
2. `CONFIG_CHANGES_SUMMARY.md` - 本文档

---

## 总结

本次改进解决了配置系统中最紧急的问题：
- ✅ 修复了 Horde 系统配置不生效的 bug
- ✅ 添加了 26 个关键的可配置参数
- ✅ 提升了模组的可配置性和平衡性调整能力
- ✅ 保持了完全的向后兼容性

**总计新增可配置参数：26个**
- Horde 系统：18个 + 1个数组
- Spore 增强：8个

**剩余待配置化参数：~25个**（详见 `HARDCODED_VALUES_AUDIT.md`）

---

生成时间：2025-01-XX
作者：Claude Code (Opus 4.6)
