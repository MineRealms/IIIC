# Phase 4 完成报告

## ✅ 已完成的修改

### 1. 威胁等级系统（HordeIntegrationManager.java）

**新增方法**:

#### calculateThreatLevel()
```java
// 基于 Hivemind 距离计算威胁等级（0-5）
double distance = SporeIntegration.getNearestHivemindDistance(player);

if (distance < 10) return 5;   // 紧急
if (distance < 20) return 4;   // 严重
if (distance < 50) return 3;   // 危险
if (distance < 100) return 2;  // 紧张
if (distance < 200) return 1;  // 警戒
return 0;  // 安全
```

**特点**:
- 基于 Spore Hivemind 距离
- 6个威胁等级（0-5）
- 距离越近威胁越高

---

#### getThreatLevel()
```java
// 带缓存的威胁等级获取（每5秒更新）
if (lastUpdate == null || currentTick - lastUpdate >= THREAT_UPDATE_INTERVAL) {
    int newLevel = calculateThreatLevel(player);
    playerThreatLevel.put(playerId, newLevel);
    lastThreatUpdate.put(playerId, currentTick);
}
```

**特点**:
- 5秒更新一次（性能优化）
- 使用 ConcurrentHashMap 缓存
- 线程安全

---

### 2. 动态 Horde 间隔（HordeIntegrationManager.java）

#### calculateHordeInterval()
```java
return switch (threatLevel) {
    case 0 -> Long.MAX_VALUE;  // 不触发
    case 1 -> 3600;  // 3分钟
    case 2 -> 1800;  // 1.5分钟
    case 3 -> 900;   // 45秒
    case 4 -> 600;   // 30秒
    case 5 -> 300;   // 15秒
    default -> Long.MAX_VALUE;
};
```

**效果**:
| 威胁等级 | Hivemind 距离 | Horde 间隔 | 每小时次数 |
|----------|---------------|------------|------------|
| 0 | 无或很远 | 不触发 | 0 |
| 1 | 100-200区块 | 3分钟 | 20次 |
| 2 | 50-100区块 | 1.5分钟 | 40次 |
| 3 | 20-50区块 | 45秒 | 80次 |
| 4 | 10-20区块 | 30秒 | 120次 |
| 5 | <10区块 | 15秒 | 240次 |

---

### 3. 动态 Horde 强度（HordeIntegrationManager.java）

#### calculateHordeIntensity()
```java
// 基础倍率（威胁等级）
double baseMult = switch (threatLevel) {
    case 1 -> 1.0;
    case 2 -> 1.3;
    case 3 -> 1.7;
    case 4 -> 2.2;
    case 5 -> 3.0;
    default -> 1.0;
};

// 污染加成（最多 +50%）
double pollutionBonus = Math.min(0.5, pollution / 400.0);

// 电压加成
double tierMult = getTierIntensityMultiplier(voltageTier);

return baseMult * (1.0 + pollutionBonus) * tierMult;
```

**效果示例**（污染200, HV Tier 3）:
| 威胁等级 | 基础倍率 | 污染加成 | 电压倍率 | 最终强度 |
|----------|----------|----------|----------|----------|
| 1 | 1.0x | +25% | 1.5x | 1.88x |
| 2 | 1.3x | +25% | 1.5x | 2.44x |
| 3 | 1.7x | +25% | 1.5x | 3.19x |
| 4 | 2.2x | +25% | 1.5x | 4.13x |
| 5 | 3.0x | +25% | 1.5x | 5.63x |

---

### 4. 威胁等级触发系统（HordeIntegrationManager.java）

#### checkThreatLevelHordes()
```java
// 获取威胁等级
int threatLevel = getThreatLevel(player, currentTick);
if (threatLevel == 0) continue;  // 安全，不触发

// 计算动态间隔
long interval = calculateHordeInterval(threatLevel);
long elapsed = currentTick - lastTrigger;

// 时间到了就触发（确定性触发）
if (elapsed >= interval) {
    double intensity = calculateHordeIntensity(threatLevel, pollution, voltageTier);
    boolean success = HordeManager.tryStartHorde(player, (int)(intensity * 10), false);
}
```

**特点**:
- 确定性触发（不是随机概率）
- 基于威胁等级动态调整
- 集成污染和电压系统

---

### 5. 调试信息增强（HordeIntegrationManager.java）

**新增输出**:
```
[Threat Level System]
  Threat Level: 3 / 5
  Nearest Hivemind: 35.2 blocks
  Horde Interval: 900 ticks (45.0s)
  Horde Intensity: 3.19x
```

**改进**:
- 显示威胁等级
- 显示最近 Hivemind 距离
- 显示动态间隔和强度
- 便于调试和平衡

---

## 📊 系统对比

### 旧系统（污染触发）
```
固定间隔: 30秒检查一次
固定概率: 5%
触发条件: 污染 > 150
```

**问题**:
- 与菌群扩张无关
- 固定间隔，无压迫感
- 随机触发，不可预测

---

### 新系统（威胁等级）
```
动态间隔: 15秒~3分钟（基于距离）
确定性触发: 时间到就触发
触发条件: Hivemind 距离 < 200区块
```

**优势**:
- 菌群越近越危险
- 可预测的压力增长
- 性能友好（确定性触发）

---

## 🎮 玩家体验

### 阶段1: 早期扩张（无 Hivemind）
- 威胁等级: 0
- Horde: 不触发
- 体验: 安全建设

---

### 阶段2: 菌群出现（150区块外）
- 威胁等级: 1
- Horde: 3分钟一次
- 体验: 偶尔警报，可以应对

---

### 阶段3: 菌群接近（75区块）
- 威胁等级: 2
- Horde: 1.5分钟一次
- 体验: 持续压力，需要炮塔

---

### 阶段4: 菌群逼近（35区块）
- 威胁等级: 3
- Horde: 45秒一次
- 体验: 高频攻击，需要自动化防御

---

### 阶段5: 菌群包围（15区块）
- 威胁等级: 4
- Horde: 30秒一次
- 体验: 战争状态，必须清理 Hivemind

---

### 阶段6: 菌群入侵（<10区块）
- 威胁等级: 5
- Horde: 15秒一次
- 体验: 紧急状态，工厂即将沦陷

---

## ⚠️ 性能考虑

### 缓存机制
- ✅ 威胁等级每5秒更新一次
- ✅ 使用 ConcurrentHashMap 存储
- ✅ 避免频繁计算距离

### 触发频率
- 最高频率: 15秒一次（威胁等级5）
- 最低频率: 3分钟一次（威胁等级1）
- 安全区域: 不触发（威胁等级0）

### 线程安全
- ✅ 所有计算在主线程
- ✅ 使用线程安全的集合
- ✅ 无竞态条件

---

## 🔗 系统集成

### 与 Spore 集成
- ✅ 使用 SporeIntegration.getNearestHivemindDistance()
- ✅ 自动检测 Spore 是否加载
- ✅ 无 Spore 时降级为旧系统

### 与污染系统集成
- ✅ 污染影响 Horde 强度（+50%）
- ✅ 保留旧的污染触发作为备用
- ✅ 双系统并行运行

### 与电压系统集成
- ✅ 电压等级影响强度倍率
- ✅ 使用现有的 tierIntensityMultipliers
- ✅ 完全兼容

---

## 🎯 设计目标达成

### ✅ 不是24小时疯狂刷怪
- 威胁等级0时不触发
- 最高频率15秒（可控）
- 基于菌群距离，有明确逻辑

### ✅ 菌群扩张有意义
- 距离越近威胁越高
- 玩家必须主动清理 Hivemind
- 形成"工业 vs 生态"的对抗

### ✅ 性能友好
- 缓存机制减少计算
- 确定性触发避免随机开销
- 上限控制防止过载

---

## 🚀 下一步：Phase 5（可选）

性能优化：
1. 实体数量全局限制
2. AI 优化
3. 异步处理优化

**预计工作量**: 2-3小时

---

## 📊 总体进度

```
Phase 1: ████████████████████ 100% ✅
Phase 2: ████████████████████ 100% ✅
Phase 3: ████████████████████ 100% ✅
Phase 4: ████████████████████ 100% ✅
Phase 5: ░░░░░░░░░░░░░░░░░░░░   0% ⏭️

总进度: ████████████████░░░░  80%
```

---

**完成时间**: 2026-05-01  
**状态**: ✅ 完成
