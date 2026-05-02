# 工业战争系统重构 - 记忆整理

## 📋 项目背景

**项目**: ImprovedMobs Industrial Integration (IIC)  
**目标**: 将线性难度系统改造为工业驱动的动态对抗系统  
**核心体验**: 建得越大 → 污染越多 → 被打越狠 → 必须自动化防御

---

## 🔍 代码分析结果

### 1. 难度系统（TriAxisDifficultyManager.java）

**当前实现**: 加法模型
```java
D = globalMultiplier * (weightTime*T + weightVoltage*V + weightPollution*P)
  = 2.0 * (0.35*T + 0.35*V + 0.30*P)
  = 最大 4.4
```

**问题**: 三轴互相稀释，后期封顶

**建议**: 乘法模型
```java
D = Base(T) × Scale(V) × Pressure(P)
  = (0.5~2.0) × (1.0~4.5) × (1.0~4.0)
  = 0.5 ~ 36.0
```

---

### 2. 污染系统（PollutionManager.java）

**问题1**: 固定衰减（第273行）
```java
double reduction = TriAxisConfig.naturalDecayRate;  // 固定0.03
```

**建议**: 比例衰减
```java
double reduction = current * TriAxisConfig.naturalDecayRate;  // 0.2%
```

---

**问题2**: 环境吸收逻辑反了（第275-277行）
```java
double envCap = current * 0.1;  // 污染越高上限越高
```

**建议**: 绝对上限
```java
double envAbsorb = Math.min(envScore * 0.002, current * 0.15);
```

---

**问题3**: 污染转化无阈值（第263行）
```java
double converted = current * TriAxisConfig.tempToPermanentRate;  // 无条件转化
```

**建议**: 阈值转化
```java
if (current > 200.0) {
    double converted = (current - 200.0) * 0.005;
}
```

---

### 3. 威胁系统（ThreatManager.java）

**问题**: 固定概率 + 单只刷新（第52-60行）
```java
if (pollution >= 60.0 && random.nextDouble() < 0.015) {
    spawnHostileZombie(level, chunkPos);  // 单只
}
```

**建议**: 动态概率 + 波次生成
```java
double spawnRate = 0.01 * (1.0 + pollution / 200.0);
int waveSize = 3 + (int)(pollution / 50.0) + (int)(tier / 2.0);
waveSize = Math.min(waveSize, 15);
```

---

### 4. Horde系统（HordeIntegrationManager.java）

**问题**: 固定间隔和概率
```java
public static double pollutionHordeCheckInterval = 600.0;  // 固定30秒
public static double pollutionHordeTriggerChance = 0.05;   // 固定5%
```

**建议**: 基于 Spore 距离的动态触发
```java
int threatLevel = calculateThreatLevel(player);  // 0-5
long interval = calculateHordeInterval(threatLevel);  // 15秒~3分钟
double intensity = calculateHordeIntensity(threatLevel);  // 1.0x~3.0x
```

---

### 5. Spore集成（已实现）

**HivemindProximityManager.java**:
- ✅ 追踪 Hivemind 位置和距离
- ✅ 计算难度加速倍率

**SporeIntegration.java**:
- ✅ 污染反馈加速 Hivemind 生长
- ✅ 获取最近 Hivemind 距离

**缺失**: Horde 系统没有使用 Spore 距离

---

## 📊 数值对比

| 场景 | 当前难度 | 新难度 | 变化 |
|------|----------|--------|------|
| 早期无污染 | 1.0 | 0.6 | -40% |
| 中期中污染 | 2.6 | 3.2 | +23% |
| 后期高污染 | 4.4 | 10.8 | +145% |
| 极后期极污染 | 4.4 | 28.8 | +555% |

---

## 🎯 实施计划

### Phase 1: 难度系统重构（最高优先级）
- 文件: `TriAxisDifficultyManager.java`
- 改为乘法模型
- 工作量: 2-3小时

### Phase 2: 污染系统优化（高优先级）
- 文件: `PollutionManager.java`
- 比例衰减 + 环境吸收优化 + 转化机制
- 工作量: 1-2小时

### Phase 3: 威胁系统增强（中高优先级）
- 文件: `ThreatManager.java`
- 动态刷怪率 + 波次生成
- 工作量: 2-3小时

### Phase 4: Horde系统动态化（中优先级）
- 文件: `HordeIntegrationManager.java`
- 基于 Spore 距离的触发
- 工作量: 1-2小时

### Phase 5: 性能优化（低优先级）
- 实体数量限制 + AI优化
- 工作量: 2-3小时

**总工作量**: 8-13小时

---

## 📄 生成的文档

1. **REFACTOR_ANALYSIS.md** - 详细代码分析
2. **REFACTOR_PLAN.md** - 具体实施方案
3. **REFACTOR_SUMMARY.md** - 对比总结
4. **SPORE_HORDE_DESIGN.md** - Spore-Horde 系统设计

---

## 🚀 下一步

开始实施 Phase 1: 难度系统重构

---

**日期**: 2026-05-01  
**状态**: 准备开始实施
