package cn.minerealms.iic.difficulty;

import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.pollution.PollutionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class TriAxisDifficultyManager {

    // 缓存上一次的难度，用于平滑过渡和限速 (ChunkPos -> Difficulty)
    // 使用 ChunkPos 而不是 BlockPos，避免玩家移动/怪物位置变化导致缓存失效
    private static final Map<ChunkPos, Double> difficultyCache = new HashMap<>();
    private static final Map<ChunkPos, Double> pollutionEmaCache = new HashMap<>();

    // 记录上次更新时间，用于基于真实时间的限速 (ChunkPos -> GameTime)
    private static final Map<ChunkPos, Long> lastUpdateTime = new HashMap<>();

    // 快速路径：检测是否在主线程且当前没有玩家请求
    // 通过检测当前tick是否刚切换来判断
    private static long lastPlayerUpdateTick = -1;
    private static boolean fastPathActive = false;

    // 同tick快速缓存，避免实体加载时重复扫描
    private static ChunkPos lastScanChunk = null;
    private static DifficultyState lastScanResult = null;
    private static long lastScanTick = -1;

    public static class DifficultyState {
        public final double totalDifficulty; // D
        public final double timeFactor;      // T
        public final double voltageFactor;   // V
        public final double pollutionFactor; // P

        public DifficultyState(double d, double t, double v, double p) {
            this.totalDifficulty = d;
            this.timeFactor = t;
            this.voltageFactor = v;
            this.pollutionFactor = p;
        }
    }

    public static DifficultyState calculateLocalDifficulty(ServerLevel level, BlockPos center) {
        try {
            // 使用 ChunkPos 作为缓存 key，避免玩家移动/怪物位置变化导致缓存失效
            ChunkPos chunkPos = new ChunkPos(center);

            long currentTick = level.getGameTime();

            // 同tick缓存：避免在同一tick内对同一chunk重复扫描（实体加载时会频繁调用）
            if (lastScanChunk != null && lastScanTick == currentTick && lastScanChunk.equals(chunkPos) && lastScanResult != null) {
                return lastScanResult;
            }

            // FAST PATH: 检测是否在实体加载上下文中
            // 当玩家最近没有被tick过时（当前tick没有玩家请求），使用快速路径
            boolean playerWasUpdatedThisTick = (lastPlayerUpdateTick == currentTick);
            boolean likelyEntityLoad = !playerWasUpdatedThisTick && !fastPathActive;

            if (likelyEntityLoad) {
                // 在实体加载上下文中（玩家没被tick过的tick），使用缓存或默认值
                Double cached = difficultyCache.get(chunkPos);
                if (cached != null) {
                    // 返回缓存值，避免扫描
                    Double pollutionEma = pollutionEmaCache.getOrDefault(chunkPos, 0.0);
                    double P = pollutionEma / TriAxisConfig.pollutionDenominator;
                    return new DifficultyState(cached, 0.5, 0.0, P);
                }
                // 无缓存，返回默认值（基于时间）
                long mcDays = level.getDayTime() / 24000L;
                double mcDaysPerRealDay = 24.0 * 3.0;
                double targetMCDays = TriAxisConfig.realWorldDaysToMax * mcDaysPerRealDay;
                double tRaw = Math.log1p(mcDays / TriAxisConfig.baseDays) / Math.log1p(targetMCDays / TriAxisConfig.baseDays);
                double T = Mth.clamp(tRaw, 0.0, 1.0);
                double Base = TriAxisConfig.baseMin + (TriAxisConfig.baseMax - TriAxisConfig.baseMin) * T;
                Double pollutionEma = pollutionEmaCache.getOrDefault(chunkPos, 0.0);
                double P = pollutionEma / TriAxisConfig.pollutionDenominator;
                return new DifficultyState(Base, T, 0.0, P);
            }

            // 标记玩家已更新，实体加载完成后会自然恢复
            if (!likelyEntityLoad) {
                lastPlayerUpdateTick = currentTick;
                fastPathActive = false;
            }

            IndustrialLogger.infoDifficulty(String.format(
                "calculateLocalDifficulty() called for chunk=%s, center=%s", chunkPos, center));

            // --- 1. 时间轴 (Time Axis - T) - 基础难度 ---
            long mcDays = level.getDayTime() / 24000L;

            // Calculate time factor based on real-world days
            // 1 MC day = 20 minutes real time
            // realWorldDaysToMax (default 30) = target real-world days to reach max difficulty
            // Convert to MC days: 30 real days × 24 hours × 3 MC days/hour = 2160 MC days
            double mcDaysPerRealDay = 24.0 * 3.0; // 24 hours × 3 MC days per hour
            double targetMCDays = TriAxisConfig.realWorldDaysToMax * mcDaysPerRealDay;

            double tRaw = Math.log1p(mcDays / TriAxisConfig.baseDays) / Math.log1p(targetMCDays / TriAxisConfig.baseDays);
            double T = Mth.clamp(tRaw, 0.0, 1.0);

            // Base difficulty: 0.5 ~ 2.0 (prevents AFK, but not dominant)
            double Base = TriAxisConfig.baseMin + (TriAxisConfig.baseMax - TriAxisConfig.baseMin) * T;

            IndustrialLogger.infoDifficulty(String.format(
                "Time Axis: mcDays=%d, targetMCDays=%.1f, T=%.4f, Base=%.4f", mcDays, targetMCDays, T, Base));

            // --- 2. 科技轴 (Voltage Axis - V) - 工业规模倍率 ---
            double V = 0.0;
            if (TriAxisConfig.hasGTCEu()) {
                IndustrialLogger.infoDifficulty("GTCEu detected, scanning machines...");
                int medianTier = MachineScanner.scanNearbyVoltageTierMedianSafely(level, center, TriAxisConfig.scanRadiusBlocks);
                // 使用配置的最大电压等级进行归一化
                int effectiveMaxTier = TriAxisConfig.getEffectiveMaxTier();
                V = Mth.clamp((double) medianTier / effectiveMaxTier, 0.0, 1.0);

                IndustrialLogger.infoDifficulty(String.format(
                    "Voltage Axis: medianTier=%d, effectiveMaxTier=%d, V=%.4f",
                    medianTier, effectiveMaxTier, V));
            } else {
                V = T; // 降级处理
                IndustrialLogger.infoDifficulty("GTCEu NOT detected, using fallback V=T");
            }

            // Scale multiplier: 1.0 ~ 4.5 (exponential growth with voltage tier)
            double tierValue = V * TriAxisConfig.getEffectiveMaxTier();
            double Scale = 1.0 + Math.pow(tierValue, TriAxisConfig.scaleExponent) * TriAxisConfig.scaleMultiplier;

            IndustrialLogger.infoDifficulty(String.format(
                "Scale calculation: tierValue=%.4f, Scale=%.4f", tierValue, Scale));

            // --- 3. Pollution Axis (P) - 环境压力 ---
            // Always use temporary + permanent pollution, regardless of Spore
            double localPollution = PollutionManager.getTemporaryPollution(chunkPos);
            double globalPollution = PollutionManager.getPermanentPollution();
            double totalPollution = localPollution + globalPollution;

            IndustrialLogger.infoPollution(String.format(
                "Pollution: local=%.2f, global=%.2f, total=%.2f",
                localPollution, globalPollution, totalPollution));

            // EMA smoothing
            double currentEma = pollutionEmaCache.getOrDefault(chunkPos, 0.0);
            double newEma = (TriAxisConfig.emaAlpha * totalPollution) + ((1.0 - TriAxisConfig.emaAlpha) * currentEma);
            pollutionEmaCache.put(chunkPos, newEma);

            // Normalize pollution for sigmoid function
            double P = newEma / TriAxisConfig.pollutionDenominator;

            // Pressure multiplier: 1.0 ~ 4.0 (sigmoid curve for smooth transition)
            // When pollution is 0, Pressure should be 1.0 (no effect on difficulty)
            double Pressure;
            if (newEma <= 0.0) {
                Pressure = 1.0;
            } else {
                double sigmoid = 1.0 / (1.0 + Math.exp(-P + TriAxisConfig.sigmoidShift));
                Pressure = TriAxisConfig.pressureMin + (TriAxisConfig.pressureMax - TriAxisConfig.pressureMin) * sigmoid;
            }

            IndustrialLogger.infoDifficulty(String.format(
                "Pollution Axis: EMA=%.4f, P=%.4f, Pressure=%.4f", newEma, P, Pressure));

            // --- 4. 乘法融合 (Multiplicative Model) ---
            // D = Base × Scale × Pressure × GlobalMultiplier
            // Range: 0.5 ~ 36.0 (much wider than additive model's 0 ~ 4.4)
            double targetD = Base * Scale * Pressure * TriAxisConfig.globalMultiplier;

            IndustrialLogger.infoDifficulty(String.format(
                "Target Difficulty: Base(%.2f) × Scale(%.2f) × Pressure(%.2f) × Global(%.2f) = %.4f",
                Base, Scale, Pressure, TriAxisConfig.globalMultiplier, targetD));

            // --- 5. 迟滞与限速 (Hysteresis & Rate Limiting) - 基于真实时间 ---
            double currentD = difficultyCache.getOrDefault(chunkPos, 0.0);
            long currentTime = level.getGameTime();
            Long lastTimeObj = lastUpdateTime.get(chunkPos);

            // 如果是第一次计算这个chunk的难度，直接设置为目标值
            if (lastTimeObj == null) {
                difficultyCache.put(chunkPos, targetD);
                lastUpdateTime.put(chunkPos, currentTime);

                IndustrialLogger.infoDifficulty(String.format(
                    "First calculation for chunk %s: setting difficulty to %.4f", chunkPos, targetD));

                return new DifficultyState(targetD, T, V, P);
            }

            // 保存到同tick缓存
            lastScanChunk = chunkPos;
            lastScanResult = new DifficultyState(targetD, T, V, P);
            lastScanTick = currentTime;

            long lastTime = lastTimeObj;

            // 计算自上次更新以来经过的游戏时间（ticks）
            long ticksElapsed = currentTime - lastTime;

            // 只有当经过至少1 tick时才更新（避免同一tick内多次调用导致重复增长）
            if (ticksElapsed > 0) {
                // 计算允许的最大变化量：maxChangePerSec × (经过的秒数)
                // 20 ticks = 1 second
                double secondsElapsed = ticksElapsed / 20.0;
                double maxChange = TriAxisConfig.maxChangePerSec * secondsElapsed;

                double delta = targetD - currentD;

                // 限制变化率（基于实际经过的时间）
                delta = Mth.clamp(delta, -maxChange, maxChange);
                double finalD = currentD + delta;

                difficultyCache.put(chunkPos, finalD);
                lastUpdateTime.put(chunkPos, currentTime);

                IndustrialLogger.infoDifficulty(String.format(
                    "Rate Limiting: currentD=%.4f, targetD=%.4f, delta=%.4f, finalD=%.4f, ticksElapsed=%d",
                    currentD, targetD, delta, finalD, ticksElapsed));

                // Debug Logging
                if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % 100 == 0) {
                    IndustrialLogger.debugDifficulty(String.format(
                            "TriAxis at %s | T: %.4f (Base: %.2f) | V: %.4f (Scale: %.2f) | P: %.4f (Pressure: %.2f) | D: %.4f | TargetD: %.4f | Delta: %.4f | TicksElapsed: %d | LocalPollution: %.2f | GlobalPollution: %.2f",
                            chunkPos, T, Base, V, Scale, P, Pressure, finalD, targetD, delta, ticksElapsed, localPollution, globalPollution));
                }

                return new DifficultyState(finalD, T, V, P);
            } else {
                // 同一tick内的重复调用，直接返回缓存值
                IndustrialLogger.infoDifficulty(String.format(
                    "Same tick, returning cached: currentD=%.4f", currentD));
                return new DifficultyState(currentD, T, V, P);
            }
        } catch (Exception e) {
            IndustrialLogger.error("[TriAxisDifficultyManager] Error calculating difficulty", e);
            return new DifficultyState(0.0, 0.0, 0.0, 0.0);
        }
    }

    // 清理缓存（可在服务器关闭或区块卸载时调用）
    public static void clearCache() {
        difficultyCache.clear();
        pollutionEmaCache.clear();
        lastUpdateTime.clear();
    }
}