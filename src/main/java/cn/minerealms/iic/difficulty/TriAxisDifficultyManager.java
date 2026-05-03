package cn.minerealms.iic.difficulty;

import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.pollution.PollutionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import java.util.HashMap;
import java.util.Map;

public class TriAxisDifficultyManager {

    // 缓存上一次的难度，用于平滑过渡和限速 (ChunkPos -> Difficulty)
    // 使用 ChunkPos 而不是 BlockPos，避免玩家移动/怪物位置变化导致缓存失效
    private static final Map<ChunkPos, Double> difficultyCache = new HashMap<>();
    private static final Map<ChunkPos, Double> pollutionEmaCache = new HashMap<>();

    // 记录上次更新时间，用于基于真实时间的限速 (ChunkPos -> GameTime)
    private static final Map<ChunkPos, Long> lastUpdateTime = new HashMap<>();

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

            IndustrialLogger.infoDifficulty(String.format(
                "calculateLocalDifficulty() called for chunk=%s, center=%s", chunkPos, center));

            // --- 1. 时间轴 (Time Axis - T) - 基础难度 ---
            long mcDays = level.getDayTime() / 24000L;

            double tRaw = Math.log1p(mcDays / TriAxisConfig.baseDays) / Math.log1p(TriAxisConfig.targetDays / TriAxisConfig.baseDays);
            double T = Mth.clamp(tRaw, 0.0, 1.0);

            // Base difficulty: 0.5 ~ 2.0 (prevents AFK, but not dominant)
            double Base = TriAxisConfig.baseMin + (TriAxisConfig.baseMax - TriAxisConfig.baseMin) * T;

            IndustrialLogger.infoDifficulty(String.format(
                "Time Axis: mcDays=%d, T=%.4f, Base=%.4f", mcDays, T, Base));

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