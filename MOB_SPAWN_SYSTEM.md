# Mob Spawn Enhancement System

## 概述

怪物刷新增强系统允许你基于 difficulty 控制怪物刷新率，并提供调试工具来诊断刷新问题。

## 功能特性

1. **刷新率控制** - 基于 difficulty 动态调整刷新概率
2. **统计监控** - 实时追踪刷新尝试、成功和失败
3. **调试模式** - 详细日志记录每次刷新事件
4. **强制刷新** - 测试用的强制刷新命令
5. **可配置参数** - 所有参数都可通过配置文件调整

## 配置参数

在 `config/triaxis-difficulty.properties` 中：

```properties
# ========== Mob Spawn Enhancement ==========

# 启用刷新增强系统（默认关闭，需要手动开启）
enableSpawnEnhancement=false

# 全局刷新倍率（0.0-10.0）
# 1.0 = 正常刷新率
# 2.0 = 双倍刷新率
# 0.5 = 半倍刷新率
spawnMultiplier=1.0

# 每点 difficulty 的额外刷新概率（0.0-1.0）
# 公式：spawnChance = spawnMultiplier + (difficulty × difficultySpawnBonus)
# 例如：difficulty=100, bonus=0.001 → +0.1 刷新概率
difficultySpawnBonus=0.001

# 最大刷新概率上限（0.0-10.0）
# 防止刷新率过高
maxSpawnChance=3.0

# 启用强制刷新（高难度区域主动刷怪）
enableForcedSpawning=false

# 强制刷新的 difficulty 阈值（50-250）
forcedSpawnThreshold=150.0

# 强制刷新概率（每秒）
forcedSpawnChance=0.01

# 每次强制刷新的怪物数量
forcedSpawnCount=3
```

## 命令使用

### 1. 查看刷新状态

```
/im spawn status
```

显示：
- 当前配置（倍率、加成、上限）
- 刷新统计（总尝试、成功、失败）
- 刷新率（每秒）
- 最常刷新的怪物类型

**示例输出**：
```
=== Mob Spawn Status ===
Enhancement: ENABLED
Spawn Multiplier: 1.50
Difficulty Bonus: 0.0020 per difficulty point
Max Spawn Chance: 3.00
Debug Mode: ON

Statistics (last 120s):
  Total Attempts: 450 (3.75/s)
  Allowed: 380 (84.4%)
  Denied: 70 (15.6%)

Top Spawned Types:
  Zombie: 120
  Skeleton: 95
  Creeper: 85
  Spider: 50
  Enderman: 30
```

### 2. 开启调试模式

```
/im spawn debug true
```

开启后，每次刷新尝试都会在日志中输出：
```
[SpawnManager] Allowed spawn: Zombie at (123, 64, -456) (difficulty=85.23, chance=1.17)
[SpawnManager] Denied spawn: Skeleton at (125, 64, -450) (difficulty=85.23, chance=1.17)
```

关闭调试：
```
/im spawn debug false
```

### 3. 重置统计

```
/im spawn reset
```

清空所有刷新统计数据，重新开始计数。

### 4. 调整刷新倍率

```
/im spawn multiplier 2.0
```

设置全局刷新倍率为 2.0（双倍刷新率）。

**常用值**：
- `0.5` - 减少刷新（如果怪物太多）
- `1.0` - 正常刷新
- `1.5` - 增加 50% 刷新
- `2.0` - 双倍刷新
- `3.0` - 三倍刷新

### 5. 调整 difficulty 加成

```
/im spawn bonus 0.002
```

设置每点 difficulty 增加 0.002 刷新概率。

**计算示例**：
- difficulty = 100, bonus = 0.001 → 额外 +0.1 刷新概率
- difficulty = 150, bonus = 0.002 → 额外 +0.3 刷新概率

### 6. 测试刷新

在玩家位置刷新 10 只僵尸：
```
/im spawn test 10 minecraft:zombie
```

在指定坐标刷新 5 只苦力怕：
```
/im spawn test 5 minecraft:creeper ~ ~ ~
/im spawn test 5 minecraft:creeper 100 64 200
```

**支持的怪物类型**：
- `minecraft:zombie`
- `minecraft:skeleton`
- `minecraft:creeper`
- `minecraft:spider`
- `minecraft:enderman`
- `minecraft:witch`
- 等等...

### 7. 启用/禁用刷新增强

```
/im spawn enable true   # 启用
/im spawn enable false  # 禁用
```

## 使用场景

### 场景 1：诊断刷新问题

如果你觉得怪物刷新太少：

1. **开启调试模式**：
   ```
   /im spawn debug true
   ```

2. **查看状态**：
   ```
   /im spawn status
   ```

3. **检查日志**：
   - 查看 `logs/latest.log`
   - 搜索 `[SpawnManager]`
   - 看看是 "Allowed" 多还是 "Denied" 多

4. **分析原因**：
   - 如果 Denied 很多 → 刷新被抑制了
   - 如果 Total Attempts 很少 → 原版刷新机制问题
   - 如果 Allowed 很多但看不到怪物 → 怪物可能被其他系统移除

### 场景 2：增加刷新率

如果确认刷新太少，想增加刷新：

1. **启用刷新增强**：
   ```
   /im spawn enable true
   ```

2. **提高倍率**：
   ```
   /im spawn multiplier 2.0
   ```

3. **增加 difficulty 加成**：
   ```
   /im spawn bonus 0.002
   ```

4. **观察效果**：
   ```
   /im spawn status
   ```
   等待 1-2 分钟，查看 "Total Attempts" 和 "Allowed" 是否增加

### 场景 3：测试特定怪物

想测试某种怪物的强度：

```
/im spawn test 20 minecraft:zombie
```

刷新 20 只僵尸，观察它们的行为和强度。

### 场景 4：高难度区域强制刷怪

如果你希望在高 difficulty 区域主动刷怪：

1. **编辑配置文件**：
   ```properties
   enableForcedSpawning=true
   forcedSpawnThreshold=150.0
   forcedSpawnChance=0.02
   forcedSpawnCount=5
   ```

2. **重载配置**：
   ```
   /im config reload
   ```

3. **效果**：
   - 当 difficulty ≥ 150 时
   - 每秒有 2% 概率
   - 强制刷新 5 只怪物

## 刷新公式

### 基础刷新概率

```
spawnChance = spawnMultiplier + (difficulty × difficultySpawnBonus)
spawnChance = min(spawnChance, maxSpawnChance)
```

### 刷新类型修正

不同刷新类型有不同的修正系数：

| 刷新类型 | 修正系数 | 说明 |
|---------|---------|------|
| NATURAL | 1.0 | 自然刷新 |
| CHUNK_GENERATION | 0.8 | 区块生成时刷新 |
| SPAWNER | 1.5 | 刷怪笼刷新（允许更多） |
| STRUCTURE | 1.2 | 结构生成时刷新 |
| 其他 | 1.0 | 其他类型 |

### 最终刷新概率

```
finalChance = spawnChance × typeModifier
```

### 示例计算

**场景**：difficulty = 100, spawnMultiplier = 1.5, difficultySpawnBonus = 0.001

```
spawnChance = 1.5 + (100 × 0.001) = 1.5 + 0.1 = 1.6
finalChance (NATURAL) = 1.6 × 1.0 = 1.6
finalChance (SPAWNER) = 1.6 × 1.5 = 2.4
```

结果：
- 自然刷新概率为 160%（比原版高 60%）
- 刷怪笼刷新概率为 240%（比原版高 140%）

## 常见问题

### Q1: 为什么开启后还是没有怪物？

**A**: 可能的原因：
1. **原版刷新机制限制** - 检查 `/gamerule doMobSpawning`
2. **区块未加载** - 怪物只在加载的区块刷新
3. **光照太高** - 大多数怪物需要低光照
4. **Mob Cap 已满** - 原版有怪物数量上限
5. **其他模组干扰** - 某些模组可能阻止刷新

**诊断步骤**：
```
/gamerule doMobSpawning          # 检查是否为 true
/im spawn debug true             # 开启调试
/im spawn status                 # 查看统计
/im spawn test 10 minecraft:zombie  # 测试强制刷新
```

### Q2: 怪物刷新太多了怎么办？

**A**: 降低刷新率：
```
/im spawn multiplier 0.5   # 减半
/im spawn bonus 0.0        # 移除 difficulty 加成
```

或者直接禁用：
```
/im spawn enable false
```

### Q3: 如何只在高难度区域增加刷新？

**A**: 使用 difficulty 加成而不是全局倍率：
```
/im spawn multiplier 1.0    # 保持基础倍率不变
/im spawn bonus 0.005       # 高 difficulty 加成
```

这样：
- difficulty = 50 → 刷新概率 = 1.0 + 0.25 = 1.25
- difficulty = 100 → 刷新概率 = 1.0 + 0.5 = 1.5
- difficulty = 150 → 刷新概率 = 1.0 + 0.75 = 1.75

### Q4: 统计数据不准确？

**A**: 重置统计：
```
/im spawn reset
```

然后等待几分钟重新收集数据。

### Q5: 如何永久保存设置？

**A**: 命令修改的设置会自动保存到配置文件。如果想手动编辑：
1. 编辑 `config/triaxis-difficulty.properties`
2. 重启服务器或使用 `/im config reload`

## 性能影响

- **事件监听** - 每次刷新尝试都会触发事件，性能影响极小
- **调试模式** - 开启后会输出大量日志，建议只在调试时使用
- **统计收集** - 使用原子操作，性能影响可忽略
- **强制刷新** - 如果启用，每秒检查一次，性能影响很小

## 与其他系统的关系

- **ImprovedMobs** - 刷新的怪物会被 ImprovedMobs 增强
- **Pollution System** - 高污染区域的 difficulty 更高，刷新更多
- **Threat System** - 威胁系统会主动刷怪，与此系统独立
- **The Hordes** - 如果安装了 The Hordes，两个系统会叠加

## 建议配置

### 轻度增强（推荐新手）

```properties
enableSpawnEnhancement=true
spawnMultiplier=1.2
difficultySpawnBonus=0.001
maxSpawnChance=2.0
enableForcedSpawning=false
```

### 中度增强（推荐普通玩家）

```properties
enableSpawnEnhancement=true
spawnMultiplier=1.5
difficultySpawnBonus=0.002
maxSpawnChance=3.0
enableForcedSpawning=false
```

### 重度增强（推荐硬核玩家）

```properties
enableSpawnEnhancement=true
spawnMultiplier=2.0
difficultySpawnBonus=0.003
maxSpawnChance=5.0
enableForcedSpawning=true
forcedSpawnThreshold=100.0
forcedSpawnChance=0.02
forcedSpawnCount=5
```

### 仅高难度增强

```properties
enableSpawnEnhancement=true
spawnMultiplier=1.0
difficultySpawnBonus=0.005
maxSpawnChance=3.0
enableForcedSpawning=false
```

这样只有在高 difficulty 区域才会增加刷新。
