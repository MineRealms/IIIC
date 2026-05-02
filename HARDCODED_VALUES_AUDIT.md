# 硬编码数值审计报告

## 审计范围
本次审计检查了以下核心系统文件中的所有硬编码数值：
- `SporeIntegration.java` - Spore 联动系统
- `ThreatManager.java` - 威胁管理系统
- `HordeIntegrationManager.java` - Horde 尸潮系统
- `PollutionManager.java` - 污染管理系统

## 审计结果总结

### ✅ 已配置化的参数
以下参数已经通过 `TriAxisConfig.java` 实现配置化：
- 所有 Spore 联动参数（15个）
- 所有威胁系统参数（波次大小、生成概率等）
- 所有污染系统参数（生成率、衰减率、吸收率等）
- 所有难度计算参数（权重、倍率、阈值等）

---

## 🔴 需要配置化的硬编码数值

### 1. SporeIntegration.java

#### 1.1 缓存和节流参数
```java
// Line 82: 污染反馈间隔
private static final int POLLUTION_FEEDBACK_INTERVAL = 100;  // 5秒 = 100 ticks

// Line 378: 进化缓存更新间隔
if (cached == null || level.getGameTime() - cached.timestamp > 200) {  // 10秒

// Line 302, 419: 调试日志输出间隔
if (TriAxisConfig.enableSporeDebug && level.getGameTime() % 200 == 0) {
```
**建议配置化：**
- `sporePollutionFeedbackInterval` = 100 (ticks)
- `sporeEvolutionCacheInterval` = 200 (ticks)
- `sporeDebugLogInterval` = 200 (ticks)

#### 1.2 进化计算参数
```java
// Line 406: 电压等级归一化
double voltageFactor = Math.min(1.0, avgVoltageTier / 9.0 * VOLTAGE_EVOLUTION_FACTOR);

// Line 438: 进化阶段转感染强度
return (float) phase * 10f;  // Phase 0-10 -> 0-100%
```
**建议配置化：**
- `sporeMaxVoltageTier` = 9.0 (用于归一化)
- `sporeInfectionIntensityMultiplier` = 10.0 (阶段转百分比)

#### 1.3 怪物增强参数
```java
// Line 525: 污染加成计算
double pollutionBonus = Math.min(1.0, pollutionLevel / 200.0);  // Max 100% at 200 pollution

// Line 526: 电压加成计算
double voltageBonus = Math.max(0, localVoltageTier - 1) * 0.10;  // 10% per tier above ULV

// Line 530: 进化加成计算
double evolutionBonus = evolutionPhase * 0.05;  // 5% per phase, max 50% at phase 10

// Line 554-560: 伤害加成计算
if (pollutionLevel > 100) {
    double damageBonus = (pollutionLevel / 200.0) * 0.3;  // 30% per 200 pollution
    double damageMultiplier = Math.min(MAX_DAMAGE_MULTIPLIER, 1.0 + damageBonus);
}
```
**建议配置化：**
- `sporePollutionBonusDivisor` = 200.0
- `sporeVoltageBonus PerTier` = 0.10
- `sporeEvolutionBonusPerPhase` = 0.05
- `sporeDamageBonusThreshold` = 100.0
- `sporeDamageBonusDivisor` = 200.0
- `sporeDamageBonusMultiplier` = 0.3

#### 1.4 其他硬编码值
```java
// Line 455: 污染反馈阈值（已有配置但未使用）
if (pollution < POLLUTION_FEEDBACK_THRESHOLD) return;  // 150

// Line 619: Hivemind 区块估算
return hiveminds * 20;  // Estimate: 20 chunks per Hivemind
```
**建议配置化：**
- `sporeChunksPerHivemind` = 20

---

### 2. ThreatManager.java

#### 2.1 性能限制参数
```java
// Line 43: 全局威胁实体上限
private static final int MAX_GLOBAL_THREAT_ENTITIES = 200;

// Line 57: 调试日志间隔
if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % 100 == 0) {
```
**建议配置化：**
- `threatMaxGlobalEntities` = 200
- `threatDebugLogInterval` = 100 (ticks)

#### 2.2 动态生成率计算
```java
// Line 71: 僵尸生成率计算
double zombieSpawnRate = baseZombieRate * (1.0 + pollution / 200.0);

// Line 81: 爬行者生成率计算
double creeperSpawnRate = baseCreeperRate * (1.0 + pollution / 200.0);
```
**建议配置化：**
- `threatSpawnRatePollutionDivisor` = 200.0

#### 2.3 生成位置参数
```java
// Line 170-171, 210-211, 227: 实体生成位置偏移
zombie.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
        level.random.nextFloat() * 360.0F, 0.0F);

// Line 252-253: 区块内随机位置
int x = chunkPos.getMinBlockX() + level.random.nextInt(16);
int z = chunkPos.getMinBlockZ() + level.random.nextInt(16);
```
**这些是 Minecraft 标准值，不建议配置化**

#### 2.4 AI 目标优先级
```java
// Line 175: 僵尸攻击机器 AI 优先级
zombie.goalSelector.addGoal(1, new ZombieDestroyMachineGoal(zombie));

// Line 215: 爬行者攻击机器 AI 优先级
creeper.goalSelector.addGoal(1, new CreeperTargetMachineGoal(creeper));
```
**建议配置化：**
- `threatZombieAttackMachineGoalPriority` = 1
- `threatCreeperTargetMachineGoalPriority` = 1

---

### 3. HordeIntegrationManager.java

#### 3.1 威胁等级系统（距离阈值）
```java
// Line 123-128: 距离转威胁等级
if (distance < 10) return 5;   // 紧急（<10区块）
if (distance < 20) return 4;   // 严重（10-20区块）
if (distance < 50) return 3;   // 危险（20-50区块）
if (distance < 100) return 2;  // 紧张（50-100区块）
if (distance < 200) return 1;  // 警戒（100-200区块）
return 0;  // 安全（>200区块）
```
**建议配置化：**
- `hordeThreatLevel5Distance` = 10.0 (紧急)
- `hordeThreatLevel4Distance` = 20.0 (严重)
- `hordeThreatLevel3Distance` = 50.0 (危险)
- `hordeThreatLevel2Distance` = 100.0 (紧张)
- `hordeThreatLevel1Distance` = 200.0 (警戒)

#### 3.2 缓存和冷却时间
```java
// Line 85: 威胁等级更新间隔
private static final long THREAT_UPDATE_INTERVAL = 100;  // 5秒更新一次

// Line 89: 尸潮冷却时间
private static final long HORDE_COOLDOWN_TICKS = 6000;  // 5分钟冷却
```
**建议配置化：**
- `hordeThreatUpdateInterval` = 100 (ticks)
- `hordePlayerCooldown` = 6000 (ticks)

#### 3.3 电压等级倍率数组
```java
// Line 67-78: 电压等级强度倍率
public static double[] tierIntensityMultipliers = {
    1.0,  // ULV (Tier 0)
    1.1,  // LV  (Tier 1)
    1.3,  // MV  (Tier 2)
    1.5,  // HV  (Tier 3)
    1.8,  // EV  (Tier 4)
    2.2,  // IV  (Tier 5)
    2.6,  // LuV (Tier 6)
    3.0,  // ZPM (Tier 7)
    3.5,  // UV  (Tier 8)
    4.0   // UHV (Tier 9)
};
```
**已经是 public static，可以通过配置文件加载**
**建议添加配置加载逻辑**

#### 3.4 已有配置参数（需要验证是否已加载）
```java
// Line 39-63: 这些参数已声明为 public static
public static boolean enableHordeIntegration = true;
public static double hordeIntensityMultiplier = 1.0;
public static double difficultyToIntensityFactor = 0.5;
public static boolean enablePollutionTriggeredHordes = true;
public static double pollutionHordeTriggerThreshold = 150.0;
public static double pollutionHordeCheckInterval = 600.0;
public static double pollutionHordeTriggerChance = 0.05;
public static boolean enableSkirmishes = true;
public static double skirmishPollutionThreshold = 80.0;
public static double skirmishInterval = 1200.0;
public static int skirmishMinCount = 3;
public static int skirmishMaxCount = 8;
public static double majorHordePollutionThreshold = 200.0;
public static double majorHordeMultiplier = 2.0;
public static boolean enableMachineTargeting = true;
public static double machineTargetingRange = 32.0;
public static double machineTargetingChance = 0.3;
public static boolean enableVoltageTierScaling = true;
```
**⚠️ 这些参数已声明但未在 TriAxisConfig 中加载！需要添加加载逻辑！**

---

### 4. PollutionManager.java

#### 4.1 扫描和更新间隔
```java
// Line 161: 污染更新间隔
if (tickCounter % 20 == 0) {  // 1秒

// Line 166: 环境扫描间隔
if (tickCounter % 6000 == 0) {  // 5分钟

// Line 188: 机器扫描半径
int scanRadius = 4;  // 4区块

// Line 215, 271: 调试日志间隔
if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % 100 == 0) {
```
**建议配置化：**
- `pollutionUpdateInterval` = 20 (ticks)
- `pollutionEnvironmentScanInterval` = 6000 (ticks)
- `pollutionMachineScanRadius` = 4 (chunks)
- `pollutionDebugLogInterval` = 100 (ticks)

#### 4.2 污染计算参数
```java
// Line 204: 污染生成公式
double pollutionValue = 0.01 * (1 + Math.pow(tier, 1.3) * 0.25);

// Line 208: 多方块倍率（已配置但公式中有硬编码）
pollutionValue *= 3.0;  // 应使用 TriAxisConfig.multiblockPollutionMultiplier
```
**建议配置化：**
- `pollutionTierExponent` = 1.3
- `pollutionTierMultiplier` = 0.25

#### 4.3 污染转换和衰减
```java
// Line 282: 环境吸收上限计算
double envAbsorb = Math.min(envScore * 0.002, current * 0.15);

// Line 291: 污染扩散系数
nextVal = applyPollutionDiffusion(temporaryPollution, cPos, nextVal, 0.15);

// Line 294: 污染清除阈值
if (nextVal <= 1.0) {
    temporaryPollution.remove(cPos);
}

// Line 329: Spore 反馈阈值
if (SporeIntegration.isSporeLoaded() && totalTempPollution > 100.0) {
```
**建议配置化：**
- `pollutionEnvAbsorptionFactor` = 0.002
- `pollutionEnvAbsorptionMaxPercent` = 0.15
- `pollutionDiffusionRate` = 0.15
- `pollutionRemovalThreshold` = 1.0
- `pollutionSporeFeedbackThreshold` = 100.0

#### 4.4 环境扫描参数
```java
// Line 347-350: 环境影响范围
for (int x = -4; x <= 4; x++) {
    for (int z = -4; z <= 4; z++) {

// Line 362-365: 污染扩散范围
for (int dx = -1; dx <= 1; dx++) {
    for (int dz = -1; dz <= 1; dz++) {

// Line 387-388: 环境扫描范围
for (int cx = -4; cx <= 4; cx++) {
    for (int cz = -4; cz <= 4; cz++) {

// Line 426-428: 区块内扫描
for (int x = 0; x < 16; x++) {
    for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
```
**建议配置化：**
- `pollutionEnvironmentRadius` = 4 (chunks)
- `pollutionDiffusionRadius` = 1 (chunks)

#### 4.5 污染清理效率
```java
// Line 470: 距离衰减公式
double cleanAmount = (amount * efficiency) / Math.max(1.0, distance);
```
**建议配置化：**
- `pollutionCleanDistanceMin` = 1.0

---

## 📊 统计总结

### 按优先级分类

#### 🔴 高优先级（影响游戏平衡）
1. **Spore 怪物增强参数** (8个)
   - 污染/电压/进化加成计算
   - 伤害加成阈值和倍率
   
2. **Horde 威胁等级距离阈值** (5个)
   - 直接影响尸潮触发机制
   
3. **Horde 配置参数加载** (18个)
   - 已声明但未加载到配置系统
   
4. **污染计算核心参数** (5个)
   - 污染生成公式、扩散、衰减

#### 🟡 中优先级（影响性能和体验）
1. **性能限制参数** (3个)
   - 全局实体上限
   - 扫描范围和间隔
   
2. **缓存和节流参数** (6个)
   - 更新间隔、冷却时间

#### 🟢 低优先级（微调参数）
1. **调试日志间隔** (4个)
2. **AI 目标优先级** (2个)
3. **其他辅助参数** (5个)

---

## 🎯 推荐实施方案

### 阶段 1：修复 Horde 配置加载（最紧急）
**问题：** `HordeIntegrationManager` 中的 18 个参数已声明但未在 `TriAxisConfig` 中加载
**影响：** 用户无法通过配置文件调整 Horde 系统
**工作量：** 中等（需要在 TriAxisConfig 中添加 load/save 逻辑）

### 阶段 2：配置化 Spore 增强参数（高优先级）
**新增参数：** 8 个
- `sporePollutionBonusDivisor`
- `sporeVoltageBonusPerTier`
- `sporeEvolutionBonusPerPhase`
- `sporeDamageBonusThreshold`
- `sporeDamageBonusDivisor`
- `sporeDamageBonusMultiplier`
- `sporeMaxVoltageTier`
- `sporeInfectionIntensityMultiplier`

### 阶段 3：配置化 Horde 威胁等级系统
**新增参数：** 7 个
- 5 个距离阈值
- 2 个时间间隔

### 阶段 4：配置化污染系统核心参数
**新增参数：** 10 个
- 污染生成公式参数
- 扩散和衰减参数
- 环境影响参数

### 阶段 5：配置化性能和调试参数
**新增参数：** 8 个
- 性能限制
- 调试间隔
- AI 优先级

---

## 📝 总计

- **已配置化参数：** ~60 个（通过 TriAxisConfig）
- **需要配置化参数：** ~51 个
  - 高优先级：26 个（包括 18 个 Horde 未加载参数）
  - 中优先级：9 个
  - 低优先级：11 个
  - Minecraft 标准值（不建议配置）：5 个

---

## ✅ 下一步行动

1. **立即修复：** 将 HordeIntegrationManager 的 18 个参数添加到 TriAxisConfig 加载逻辑
2. **高优先级：** 配置化 Spore 增强参数（8个）
3. **高优先级：** 配置化 Horde 威胁等级系统（7个）
4. **中优先级：** 配置化污染系统核心参数（10个）
5. **低优先级：** 配置化性能和调试参数（8个）

**预计总工作量：** 
- 阶段 1：1-2 小时
- 阶段 2-5：3-4 小时
- 总计：4-6 小时

---

生成时间：2025-01-XX
审计人：Claude Code (Opus 4.6)
