package cn.minerealms.iic.difficulty;

import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.integration.spore.HivemindProximityManager;
import cn.minerealms.iic.integration.spore.SporeIntegration;
import cn.minerealms.iic.pollution.PollutionManager;
import io.github.flemmli97.improvedmobs.api.difficulty.DifficultyGetter;
import io.github.flemmli97.improvedmobs.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ImprovedMobs API difficulty provider for location-based difficulty calculation.
 * <p>
 * <b>Responsibility:</b> Implements {@link DifficultyGetter} to provide location-based
 * difficulty values to the ImprovedMobs system using the Tri-Axis Difficulty Model.
 * <p>
 * <b>Architecture:</b>
 * <ul>
 *   <li><b>DifficultyProvider</b> - Provides location-based difficulty to ImprovedMobs API (this class)</li>
 *   <li><b>TriAxisDifficultyManager</b> - Calculates tri-axis difficulty (Time × Voltage × Pollution)</li>
 *   <li><b>DifficultyManager</b> - Manages per-player state (legacy, for compatibility)</li>
 * </ul>
 * <p>
 * <b>Tri-Axis Model:</b>
 * <pre>
 * D = Base(T) × Scale(V) × Pressure(P) × GlobalMultiplier
 *
 * Where:
 * - T (Time): 0.0-1.0, normalized game days
 * - V (Voltage): 0.0-1.0, normalized median voltage tier
 * - P (Pollution): 0.0-1.0, normalized pollution level
 * - Base: 0.5-2.0 (prevents AFK, not dominant)
 * - Scale: 1.0-4.5 (exponential growth with voltage)
 * - Pressure: 1.0-4.0 (sigmoid curve for pollution)
 * </pre>
 *
 * @see TriAxisDifficultyManager
 * @see PollutionManager
 * @see TriAxisConfig
 * @author ImprovedMobs Team
 * @since 1.0.0
 */
public class DifficultyProvider implements DifficultyGetter {

    @Override
    public float getDifficulty(ServerLevel level, Vec3 pos) {
        try {
            // Use Tri-Axis Difficulty Model
            BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);

            IndustrialLogger.infoDifficulty(String.format(
                "getDifficulty() called at pos=%s", blockPos));

            TriAxisDifficultyManager.DifficultyState state =
                TriAxisDifficultyManager.calculateLocalDifficulty(level, blockPos);

            // Get raw difficulty from tri-axis calculation
            double rawDifficulty = state.totalDifficulty;

            // Scale to ImprovedMobs expected range (0-250)
            // Our system outputs 0-36 (Base: 0.5-2.0 × Scale: 1.0-4.5 × Pressure: 1.0-4.0 × Global: 2.0)
            // We need to scale this to 0-targetMaxDifficulty
            double scaledDifficulty = scaleToImprovedMobsRange(rawDifficulty);

            float difficulty = (float) scaledDifficulty;

            // Rate-limited INFO logging
            IndustrialLogger.infoDifficulty(String.format(
                "Result: Raw=%.4f | Scaled=%.4f | Time=%.4f | Voltage=%.4f | Pollution=%.4f",
                rawDifficulty, difficulty, state.timeFactor, state.voltageFactor, state.pollutionFactor));

            // Debug logging
            if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % 100 == 0) {
                IndustrialLogger.debugDifficulty(String.format(
                    "[DifficultyProvider] Pos: %s | Raw: %.4f | Scaled: %.4f | T: %.4f | V: %.4f | P: %.4f",
                    blockPos, rawDifficulty, difficulty, state.timeFactor, state.voltageFactor, state.pollutionFactor));
            }

            return difficulty;
        } catch (Exception e) {
            IndustrialLogger.error("[DifficultyProvider] Error calculating difficulty", e);
            return 0.0f;
        }
    }

    /**
     * Scale raw tri-axis difficulty to ImprovedMobs expected range.
     * <p>
     * Our system outputs approximately 0-36 range:
     * - Base: 0.5-2.0 (time factor)
     * - Scale: 1.0-4.5 (voltage factor)
     * - Pressure: 1.0-4.0 (pollution factor)
     * - Global: 2.0 (multiplier)
     * - Max: 2.0 × 4.5 × 4.0 × 2.0 = 72.0 (theoretical max)
     * - Practical max: ~36.0 (typical gameplay)
     * <p>
     * ImprovedMobs expects 0-250 range where:
     * - 50: Mobs start breaking blocks
     * - 100: Moderate difficulty
     * - 150: High difficulty
     * - 250: Maximum difficulty
     *
     * @param rawDifficulty Raw difficulty from tri-axis calculation
     * @return Scaled difficulty for ImprovedMobs
     */
    private double scaleToImprovedMobsRange(double rawDifficulty) {
        // Calculate theoretical maximum from our system
        // Max = baseMax × (1 + maxTier^scaleExponent × scaleMultiplier) × pressureMax × globalMultiplier
        double theoreticalMax = TriAxisConfig.baseMax *
            (1.0 + Math.pow(TriAxisConfig.getEffectiveMaxTier(), TriAxisConfig.scaleExponent) * TriAxisConfig.scaleMultiplier) *
            TriAxisConfig.pressureMax *
            TriAxisConfig.globalMultiplier;

        // Scale to target max difficulty (default 250)
        double scaled = (rawDifficulty / theoreticalMax) * TriAxisConfig.targetMaxDifficulty;

        // Clamp to valid range
        return Math.max(0.0, Math.min(scaled, TriAxisConfig.targetMaxDifficulty));
    }

    @Override
    public Config.IntegrationType getType() {
        // Use config to determine integration type
        return TriAxisConfig.takeoverImprovedMobsDifficulty ?
            Config.IntegrationType.ON :  // Take over difficulty calculation
            Config.IntegrationType.ADD;  // Add to base difficulty
    }
}