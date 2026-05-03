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

            // Return the total difficulty from tri-axis calculation
            float difficulty = (float) state.totalDifficulty;

            // Rate-limited INFO logging
            IndustrialLogger.infoDifficulty(String.format(
                "Result: Difficulty=%.4f | Time=%.4f | Voltage=%.4f | Pollution=%.4f",
                difficulty, state.timeFactor, state.voltageFactor, state.pollutionFactor));

            // Debug logging
            if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % 100 == 0) {
                IndustrialLogger.debugDifficulty(String.format(
                    "[DifficultyProvider] Pos: %s | Difficulty: %.4f | T: %.4f | V: %.4f | P: %.4f",
                    blockPos, difficulty, state.timeFactor, state.voltageFactor, state.pollutionFactor));
            }

            return difficulty;
        } catch (Exception e) {
            IndustrialLogger.error("[DifficultyProvider] Error calculating difficulty", e);
            return 0.0f;
        }
    }

    @Override
    public Config.IntegrationType getType() {
        return Config.IntegrationType.ADD;  // Add to base difficulty
    }
}