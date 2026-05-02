---
name: Difficulty Rate Limiting Bug
description: Critical bug - difficulty increased 15x faster than intended due to per-call instead of per-second rate limiting
type: feedback
---

**问题**: 难度增长速度异常快（10分钟涨到135%），是预期速度的15倍

**根本原因**: 
`TriAxisDifficultyManager.calculateLocalDifficulty()` 的限速机制是**按调用次数**而不是**按真实时间**

**Why**:
1. `maxChangePerSec = 0.015` 本意是"每秒最大变化0.015"
2. 但代码实现是"每次调用最大变化0.015"
3. `calculateLocalDifficulty()` 被两个地方调用：
   - HudUpdateService: 每秒1次（显示HUD）
   - DifficultyProvider: 每次怪物生成/更新时调用（可能每秒N次）
4. 如果每秒有15个怪物更新，实际增长 = `0.015 × 15 = 0.225`/秒
5. 10分钟 = 600秒，增长 = `0.225 × 600 = 135%` ✓ 符合观察

**错误代码**:
```java
// 错误 ❌ - 每次调用限制
double delta = targetD - currentD;
delta = Mth.clamp(delta, -TriAxisConfig.maxChangePerSec, TriAxisConfig.maxChangePerSec);
double finalD = currentD + delta;
difficultyCache.put(center, finalD);
```

**修复代码**:
```java
// 正确 ✅ - 基于真实时间限制
long currentTime = level.getGameTime();
long lastTime = lastUpdateTime.getOrDefault(center, currentTime);
long ticksElapsed = currentTime - lastTime;

if (ticksElapsed > 0) {
    double secondsElapsed = ticksElapsed / 20.0;
    double maxChange = TriAxisConfig.maxChangePerSec * secondsElapsed;
    
    double delta = targetD - currentD;
    delta = Mth.clamp(delta, -maxChange, maxChange);
    double finalD = currentD + delta;
    
    difficultyCache.put(center, finalD);
    lastUpdateTime.put(center, currentTime);
    return new DifficultyState(finalD, T, V, P);
} else {
    // 同一tick内重复调用，返回缓存值
    return new DifficultyState(currentD, T, V, P);
}
```

**How to apply**:
- 任何限速机制都应该基于**真实时间**而不是**调用次数**
- 需要记录上次更新时间：`Map<Key, Long> lastUpdateTime`
- 计算时间差：`ticksElapsed = currentTime - lastTime`
- 按时间比例计算允许的变化量：`maxChange = maxChangePerSec × (ticksElapsed / 20.0)`
- 同一tick内的重复调用应该返回缓存值，避免重复增长

**影响**:
- 修复前：难度增长速度取决于怪物数量（不可控）
- 修复后：难度增长速度严格按配置的 `maxChangePerSec` 限制（可控）
