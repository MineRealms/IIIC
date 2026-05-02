# Spore-Driven Horde System 设计文档
> 基于菌群网络距离的动态 Horde 触发机制

---

## 🎯 核心理念

**不是疯狂刷怪，而是生态压迫**

```
污染扩散 → 加速 Hivemind 生长 → 菌群网络扩张 → 接近玩家 → 触发 Horde
```

---

## 📊 当前系统分析

### 已实现的功能

#### 1. HivemindProximityManager
```java
// 第52-63行：追踪 Hivemind 距离
public record HivemindProximityInfo(
    boolean isNear,      // 是否在范围内
    double distance,     // 距离（区块）
    int biomass,         // 生物质
    double pollution     // 污染
)

// 第64-90行：计算加速倍率
public double calculateAccelerationMultiplier() {
    double distanceFactor = 1.0 / Math.max(1.0, distance);  // 越近越强
    double biomassFactor = Math.log10(Math.max(10, biomass)) / 3.0;
    double pollutionFactor = Math.min(2.0, pollution / 100.0);
    return 1.0 + (distanceFactor × biomassFactor × pollutionFactor);
}
```

**功能**: 追踪最近的 Hivemind，计算难度加速

---

#### 2. SporeIntegration
```java
// 第439-480行：污染反馈
public static void applyPollutionFeedback(ServerLevel level, double pollution) {
    int biomassIncrease = (int) (pollution * POLLUTION_TO_BIOMASS_RATE);
    // 给所有 Hivemind 增加生物质
}

// 第547-560行：获取最近 Hivemind 距离
public static double getNearestHivemindDistance(Entity entity) {
    List<Object> hiveminds = getHiveminds();
    // 计算最近距离
}
```

**功能**: 污染加速 Hivemind 生长，可以查询距离

---

### ❌ 缺失的功能

**HordeIntegrationManager 没有使用 Spore 距离**

当前触发逻辑（第44-46行）：
```java
public static double pollutionHordeTriggerThreshold = 150.0;  // 固定阈值
public static double pollutionHordeCheckInterval = 600.0;     // 固定间隔
public static double pollutionHordeTriggerChance = 0.05;      // 固定概率
```

**问题**: 
- 只看污染，不看菌群距离
- 固定间隔，没有压迫感
- 菌群扩张没有意义

---

## 🔧 新系统设计

### 核心机制：距离驱动的威胁等级

```
Hivemind 距离 → 威胁等级 → Horde 频率 + 强度
```

---

### 1. 威胁等级计算

```java
/**
 * 威胁等级：0-5
 * 0 = 安全（无 Hivemind 或很远）
 * 1 = 警戒（Hivemind 在 100+ 区块外）
 * 2 = 紧张（50-100 区块）
 * 3 = 危险（20-50 区块）
 * 4 = 严重（10-20 区块）
 * 5 = 紧急（<10 区块）
 */
public static int calculateThreatLevel(ServerPlayer player) {
    // 获取最近 Hivemind 距离
    double distance = SporeIntegration.getNearestHivemindDistance(player);
    
    if (distance == Double.MAX_VALUE) return 0;  // 无 Hivemind
    
    // 距离转威胁等级
    if (distance < 10) return 5;   // 紧急
    if (distance < 20) return 4;   // 严重
    if (distance < 50) return 3;   // 危险
    if (distance < 100) return 2;  // 紧张
    if (distance < 200) return 1;  // 警戒
    return 0;  // 安全
}
```

---

### 2. 动态 Horde 间隔

**原则**: 菌群越近，Horde 越频繁

```java
/**
 * 计算 Horde 检查间隔（ticks）
 * 
 * 威胁等级 0: 不触发
 * 威胁等级 1: 3600 ticks (3分钟)
 * 威胁等级 2: 1800 ticks (1.5分钟)
 * 威胁等级 3: 900 ticks (45秒)
 * 威胁等级 4: 600 ticks (30秒)
 * 威胁等级 5: 300 ticks (15秒)
 */
public static long calculateHordeInterval(int threatLevel) {
    return switch (threatLevel) {
        case 0 -> Long.MAX_VALUE;  // 不触发
        case 1 -> 3600;  // 3分钟
        case 2 -> 1800;  // 1.5分钟
        case 3 -> 900;   // 45秒
        case 4 -> 600;   // 30秒
        case 5 -> 300;   // 15秒
        default -> Long.MAX_VALUE;
    };
}
```

---

### 3. 动态 Horde 强度

**原则**: 菌群越近，Horde 越强

```java
/**
 * 计算 Horde 强度倍率
 * 
 * 威胁等级 1: 1.0x (基础)
 * 威胁等级 2: 1.3x
 * 威胁等级 3: 1.7x
 * 威胁等级 4: 2.2x
 * 威胁等级 5: 3.0x (极限)
 */
public static double calculateHordeIntensity(int threatLevel, double pollution) {
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
    int voltageTier = getPlayerVoltageTier(player);
    double tierMult = getTierIntensityMultiplier(voltageTier);
    
    return baseMult * (1.0 + pollutionBonus) * tierMult;
}
```

---

### 4. 触发条件

**不再是固定概率，而是确定性触发**

```java
/**
 * 检查是否应该触发 Horde
 */
public static boolean shouldTriggerHorde(ServerPlayer player, long currentTick) {
    // 1. 计算威胁等级
    int threatLevel = calculateThreatLevel(player);
    if (threatLevel == 0) return false;  // 安全，不触发
    
    // 2. 检查冷却
    String playerKey = player.getUUID().toString();
    Long lastTrigger = playerHordeCooldowns.get(playerKey);
    if (lastTrigger == null) {
        playerHordeCooldowns.put(playerKey, currentTick);
        return false;
    }
    
    // 3. 计算间隔
    long interval = calculateHordeInterval(threatLevel);
    long elapsed = currentTick - lastTrigger;
    
    // 4. 时间到了就触发（确定性）
    if (elapsed >= interval) {
        playerHordeCooldowns.put(playerKey, currentTick);
        return true;
    }
    
    return false;
}
```

---

## 📊 数值表

### 威胁等级与 Horde 频率

| 威胁等级 | Hivemind 距离 | Horde 间隔 | 每小时次数 | 强度倍率 |
|----------|---------------|------------|------------|----------|
| 0 | 无或很远 | 不触发 | 0 | - |
| 1 | 100-200区块 | 3分钟 | 20次 | 1.0x |
| 2 | 50-100区块 | 1.5分钟 | 40次 | 1.3x |
| 3 | 20-50区块 | 45秒 | 80次 | 1.7x |
| 4 | 10-20区块 | 30秒 | 120次 | 2.2x |
| 5 | <10区块 | 15秒 | 240次 | 3.0x |

---

### 实际威胁对比

假设：污染 200，HV 科技（Tier 3）

| 威胁等级 | Horde 强度 | 波次大小 | 每小时总怪物 | 说明 |
|----------|------------|----------|--------------|------|
| 1 | 1.5x | 15只 | 300只 | 偶尔骚扰 |
| 2 | 2.0x | 20只 | 800只 | 持续压力 |
| 3 | 2.6x | 26只 | 2080只 | 需要防御 |
| 4 | 3.3x | 33只 | 3960只 | 高强度 |
| 5 | 4.5x | 45只 | 10800只 | 战争状态 |

**注意**: 这些是理论最大值，实际会被性能限制

---

## 🎮 玩家体验

### 阶段 1: 早期扩张（无 Hivemind）
- 威胁等级: 0
- Horde: 不触发
- 体验: 安全建设

---

### 阶段 2: 菌群出现（200+ 区块外）
- 威胁等级: 1
- Horde: 3分钟一次
- 体验: 偶尔警报，可以应对

---

### 阶段 3: 菌群接近（50-100 区块）
- 威胁等级: 2
- Horde: 1.5分钟一次
- 体验: 持续压力，需要炮塔

---

### 阶段 4: 菌群逼近（20-50 区块）
- 威胁等级: 3
- Horde: 45秒一次
- 体验: 高频攻击，需要自动化防御

---

### 阶段 5: 菌群包围（10-20 区块）
- 威胁等级: 4
- Horde: 30秒一次
- 体验: 战争状态，必须清理 Hivemind

---

### 阶段 6: 菌群入侵（<10 区块）
- 威胁等级: 5
- Horde: 15秒一次
- 体验: 紧急状态，工厂即将沦陷

---

## 🔧 实施方案

### 修改文件: HordeIntegrationManager.java

#### 1. 添加威胁等级系统

```java
// 在类开头添加
private static final Map<String, Integer> playerThreatLevel = new ConcurrentHashMap<>();
private static final Map<String, Long> lastThreatUpdate = new ConcurrentHashMap<>();

/**
 * 更新玩家威胁等级（每5秒）
 */
public static void updateThreatLevels(ServerLevel level) {
    long currentTick = level.getGameTime();
    
    for (ServerPlayer player : level.players()) {
        String playerKey = player.getUUID().toString();
        Long lastUpdate = lastThreatUpdate.get(playerKey);
        
        // 每5秒更新一次
        if (lastUpdate == null || currentTick - lastUpdate >= 100) {
            int threatLevel = calculateThreatLevel(player);
            playerThreatLevel.put(playerKey, threatLevel);
            lastThreatUpdate.put(playerKey, currentTick);
            
            // 调试信息
            if (TriAxisConfig.enableSporeDebug && threatLevel > 0) {
                double distance = SporeIntegration.getNearestHivemindDistance(player);
                IndustrialLogger.debugSpore(String.format(
                    "[Horde-Spore] Player %s: Threat Level %d, Hivemind Distance: %.1f chunks",
                    player.getName().getString(), threatLevel, distance
                ));
            }
        }
    }
}

/**
 * 计算威胁等级
 */
private static int calculateThreatLevel(ServerPlayer player) {
    if (!SporeIntegration.isSporeLoaded()) return 0;
    
    double distance = SporeIntegration.getNearestHivemindDistance(player);
    
    if (distance == Double.MAX_VALUE) return 0;
    if (distance < 10) return 5;
    if (distance < 20) return 4;
    if (distance < 50) return 3;
    if (distance < 100) return 2;
    if (distance < 200) return 1;
    return 0;
}

/**
 * 获取玩家威胁等级
 */
public static int getPlayerThreatLevel(ServerPlayer player) {
    return playerThreatLevel.getOrDefault(player.getUUID().toString(), 0);
}
```

---

#### 2. 修改 Horde 触发逻辑

```java
/**
 * 检查并触发基于 Spore 距离的 Horde
 */
public static void checkSporeProximityHorde(ServerLevel level) {
    if (!HordeManager.isHordesLoaded() || !SporeIntegration.isSporeLoaded()) return;
    if (!enableHordeIntegration) return;
    
    long currentTick = level.getGameTime();
    
    // 更新威胁等级
    updateThreatLevels(level);
    
    for (ServerPlayer player : level.players()) {
        // 获取威胁等级
        int threatLevel = getPlayerThreatLevel(player);
        if (threatLevel == 0) continue;  // 安全，跳过
        
        // 检查是否应该触发
        if (shouldTriggerSporeHorde(player, currentTick, threatLevel)) {
            // 计算强度
            ChunkPos chunkPos = player.chunkPosition();
            double pollution = PollutionManager.getTemporaryPollution(chunkPos);
            double intensity = calculateHordeIntensity(threatLevel, pollution, player);
            
            // 触发 Horde
            triggerSporeHorde(player, intensity, threatLevel);
        }
    }
}

/**
 * 检查是否应该触发 Spore Horde
 */
private static boolean shouldTriggerSporeHorde(ServerPlayer player, long currentTick, int threatLevel) {
    String playerKey = player.getUUID().toString();
    Long lastTrigger = playerHordeCooldowns.get(playerKey);
    
    if (lastTrigger == null) {
        playerHordeCooldowns.put(playerKey, currentTick);
        return false;
    }
    
    // 计算间隔
    long interval = calculateHordeInterval(threatLevel);
    long elapsed = currentTick - lastTrigger;
    
    // 确定性触发
    if (elapsed >= interval) {
        playerHordeCooldowns.put(playerKey, currentTick);
        return true;
    }
    
    return false;
}

/**
 * 计算 Horde 间隔
 */
private static long calculateHordeInterval(int threatLevel) {
    return switch (threatLevel) {
        case 1 -> 3600;  // 3分钟
        case 2 -> 1800;  // 1.5分钟
        case 3 -> 900;   // 45秒
        case 4 -> 600;   // 30秒
        case 5 -> 300;   // 15秒
        default -> Long.MAX_VALUE;
    };
}

/**
 * 计算 Horde 强度
 */
private static double calculateHordeIntensity(int threatLevel, double pollution, ServerPlayer player) {
    // 基础倍率
    double baseMult = switch (threatLevel) {
        case 1 -> 1.0;
        case 2 -> 1.3;
        case 3 -> 1.7;
        case 4 -> 2.2;
        case 5 -> 3.0;
        default -> 1.0;
    };
    
    // 污染加成
    double pollutionBonus = Math.min(0.5, pollution / 400.0);
    
    // 电压加成
    int voltageTier = getPlayerVoltageTier(player);
    double tierMult = getTierIntensityMultiplier(voltageTier);
    
    return baseMult * (1.0 + pollutionBonus) * tierMult * hordeIntensityMultiplier;
}

/**
 * 触发 Spore 驱动的 Horde
 */
private static void triggerSporeHorde(ServerPlayer player, double intensity, int threatLevel) {
    // 计算波次大小
    int baseSize = 10;
    int waveSize = (int) (baseSize * intensity);
    waveSize = Math.min(waveSize, 50);  // 上限50只
    
    // 触发 Horde
    boolean success = HordeManager.tryStartHorde(player, waveSize, true);
    
    if (success && TriAxisConfig.enableSporeDebug) {
        IndustrialLogger.debugSpore(String.format(
            "[Horde-Spore] Triggered for %s: Threat Level %d, Intensity %.2fx, Wave Size %d",
            player.getName().getString(), threatLevel, intensity, waveSize
        ));
    }
}
```

---

#### 3. 在主 tick 方法中调用

```java
/**
 * 主 tick 方法（在 ServerTickEvent 中调用）
 */
public static void onServerTick(ServerLevel level) {
    if (!enableHordeIntegration) return;
    
    long currentTick = level.getGameTime();
    
    // 每秒检查一次 Spore 驱动的 Horde
    if (currentTick % 20 == 0) {
        checkSporeProximityHorde(level);
    }
    
    // 清理无效僵尸
    if (currentTick % 200 == 0) {
        cleanup(level);
    }
}
```

---

## 📊 性能优化

### 1. 威胁等级缓存
- 每5秒更新一次（不是每 tick）
- 避免频繁调用 `getNearestHivemindDistance`

### 2. 确定性触发
- 不使用随机概率
- 基于时间间隔，可预测

### 3. 波次大小限制
- 最大50只/波
- 避免实体爆炸

### 4. 距离计算优化
- `SporeIntegration.getNearestHivemindDistance` 已经优化
- 只计算到玩家的距离

---

## 🎯 配置参数

```java
// HordeIntegrationManager.java

// Spore 驱动的 Horde
public static boolean enableSporeProximityHorde = true;
public static double[] threatLevelDistances = {200, 100, 50, 20, 10};  // 威胁等级距离阈值
public static long[] threatLevelIntervals = {3600, 1800, 900, 600, 300};  // 威胁等级间隔
public static double[] threatLevelIntensities = {1.0, 1.3, 1.7, 2.2, 3.0};  // 威胁等级强度
public static int sporeHordeMaxSize = 50;  // 最大波次大小
```

---

## 🎮 玩家策略

### 1. 监控菌群距离
- 使用命令查看最近 Hivemind 距离
- 威胁等级 3+ 时需要警惕

### 2. 主动清理 Hivemind
- 菌群接近时，组织远征清理
- 推迟菌群扩张

### 3. 建立防御纵深
- 外围炮塔（应对威胁等级 1-2）
- 核心防御（应对威胁等级 3-4）
- 紧急避难所（威胁等级 5）

### 4. 污染管理
- 降低污染 = 减缓菌群生长
- 减缓菌群扩张速度

---

## 📝 总结

### 核心改进

1. **不再是固定间隔刷怪**
   - 而是基于菌群距离的动态触发

2. **不再是随机概率**
   - 而是确定性的时间间隔

3. **不再是疯狂刷怪**
   - 而是渐进式的压力增加

4. **菌群扩张有意义**
   - 菌群越近 = 威胁越大

### 玩家体验

- ✅ 早期安全（无菌群）
- ✅ 中期警觉（菌群远）
- ✅ 后期紧张（菌群近）
- ✅ 极限挑战（菌群包围）

### 性能友好

- ✅ 威胁等级缓存（5秒更新）
- ✅ 确定性触发（无随机）
- ✅ 波次大小限制（最大50）
- ✅ 距离计算优化

---

**创建日期**: 2026-05-01  
**状态**: 设计完成，待实施  
**预计工作量**: 2-3小时
