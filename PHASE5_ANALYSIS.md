# Phase 5: 性能优化分析

## 🔍 性能瓶颈分析

### 1. 污染系统（PollutionManager.java）

**当前问题**:
```java
// 每秒遍历所有污染区块
for (ChunkPos cPos : temporaryPollution.keySet()) {
    double current = temporaryPollution.get(cPos);
    // 计算衰减、转化、扩散...
}
```

**瓶颈**:
- 大量区块时（1000+）每秒遍历开销大
- 每个区块都计算环境吸收（getSurroundingEnvironmentalReduction）
- 扩散计算（applyPollutionDiffusion）涉及邻居区块

**优化方案**:
1. **批量处理**: 每秒只处理一部分区块（轮询）
2. **环境缓存**: 缓存环境吸收值（5秒更新）
3. **跳过低污染**: 污染 < 1.0 的区块直接移除

---

### 2. 威胁系统（ThreatManager.java）

**当前问题**:
```java
// 每秒检查每个污染区块
public static void checkAndTriggerThreats(ServerLevel level, ChunkPos chunkPos, 
                                          double pollution, double avgVoltageTier) {
    // 动态概率计算
    double spawnRate = baseRate * (1.0 + pollution / 200.0);
    
    // 波次大小计算
    int waveSize = calculateWaveSize(pollution, avgVoltageTier, false);
    
    // 生成多个实体
    for (int i = 0; i < waveSize; i++) {
        spawnHostileZombie(level, chunkPos);
    }
}
```

**瓶颈**:
- 每秒为每个区块计算刷怪率
- 波次生成可能同时创建大量实体
- 没有全局实体数量限制

**优化方案**:
1. **全局实体限制**: 限制同时存在的威胁实体总数
2. **区块优先级**: 优先处理玩家附近的区块
3. **批量生成**: 分帧生成实体，避免卡顿

---

### 3. Horde 系统（HordeIntegrationManager.java）

**当前问题**:
```java
// 每 tick 遍历所有玩家
public static void tick(ServerLevel level) {
    for (ServerPlayer player : level.players()) {
        // 获取威胁等级（已缓存5秒）
        int threatLevel = getThreatLevel(player, currentTick);
        
        // 计算强度
        double intensity = calculateHordeIntensity(threatLevel, pollution, voltageTier);
    }
}
```

**瓶颈**:
- 每 tick 遍历所有玩家（20次/秒）
- 多人服务器时开销大

**优化方案**:
1. **降低检查频率**: 从每 tick 改为每5 tick（4次/秒）
2. **玩家批处理**: 每次只处理部分玩家

---

### 4. 难度计算（TriAxisDifficultyManager.java）

**当前问题**:
```java
// 每次计算都扫描附近机器
int medianTier = MachineScanner.scanNearbyVoltageTierMedianSafely(
    level, center, TriAxisConfig.scanRadiusBlocks);
```

**瓶颈**:
- 扫描半径内所有方块实体
- 频繁调用时开销大

**优化方案**:
1. **已有缓存**: MachineScanner 已实现缓存（5秒）
2. **无需额外优化**: 当前实现已经很好

---

## 📊 优化优先级

### 高优先级
1. **全局实体限制** - 防止服务器崩溃
2. **污染区块批处理** - 减少每秒计算量
3. **Horde 检查频率** - 降低 tick 开销

### 中优先级
4. **环境吸收缓存** - 减少重复计算
5. **低污染区块清理** - 减少遍历数量

### 低优先级
6. **波次分帧生成** - 优化体验（非必需）

---

## 🎯 优化目标

### 性能指标
- **TPS**: 保持 20 TPS（无卡顿）
- **内存**: 减少 ConcurrentHashMap 大小
- **CPU**: 减少每 tick 计算量

### 兼容性
- ✅ 不改变游戏体验
- ✅ 保持数值平衡
- ✅ 向后兼容配置

---

## 🔧 实施计划

### 优化1: 全局实体限制
**文件**: ThreatManager.java  
**工作量**: 30分钟

### 优化2: 污染区块批处理
**文件**: PollutionManager.java  
**工作量**: 45分钟

### 优化3: Horde 检查频率
**文件**: HordeIntegrationManager.java  
**工作量**: 15分钟

### 优化4: 环境吸收缓存
**文件**: PollutionManager.java  
**工作量**: 30分钟

### 优化5: 低污染区块清理
**文件**: PollutionManager.java  
**工作量**: 15分钟

**总工作量**: 2-3小时

---

## 💡 建议

### 立即实施
- 优化1: 全局实体限制（防止崩溃）
- 优化3: Horde 检查频率（简单有效）
- 优化5: 低污染区块清理（简单有效）

### 可选实施
- 优化2: 污染区块批处理（复杂，但效果好）
- 优化4: 环境吸收缓存（中等复杂度）

---

**创建时间**: 2026-05-01  
**状态**: 分析完成，等待实施
