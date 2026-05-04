package cn.minerealms.iic.spawn;

import cn.minerealms.iic.IntegratedIndustrialCraft;
import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import io.github.flemmli97.improvedmobs.difficulty.DifficultyData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages mob spawning based on difficulty and configuration.
 * <p>
 * Features:
 * - Monitor spawn rates and counts
 * - Apply spawn multipliers based on difficulty
 * - Override mob cap limits
 * - Debug spawn events
 * <p>
 * Configuration:
 * - enableSpawnEnhancement: Enable/disable spawn enhancement
 * - spawnMultiplier: Global spawn rate multiplier
 * - difficultySpawnBonus: Additional spawns per difficulty point
 * - maxSpawnCapOverride: Override vanilla mob cap
 */
@Mod.EventBusSubscriber(modid = IntegratedIndustrialCraft.MODID)
public class MobSpawnManager {

    // Spawn statistics
    private static final AtomicInteger totalSpawns = new AtomicInteger(0);
    private static final AtomicInteger allowedSpawns = new AtomicInteger(0);
    private static final AtomicInteger deniedSpawns = new AtomicInteger(0);
    private static final AtomicLong lastResetTime = new AtomicLong(0);

    // Per-entity-type spawn counts (for debugging)
    private static final Map<EntityType<?>, Integer> spawnCountsByType = new HashMap<>();

    // Debug mode
    private static boolean debugMode = false;

    /**
     * Handle mob spawn finalization event.
     * This is called after a mob has been spawned and is about to be added to the world.
     */
    @SubscribeEvent
    public static void onMobSpawnFinalize(MobSpawnEvent.FinalizeSpawn event) {
        if (!TriAxisConfig.enableSpawnEnhancement) {
            return;
        }

        if (!(event.getEntity() instanceof Monster mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Track spawn attempt
        totalSpawns.incrementAndGet();

        // Get current difficulty
        double difficulty = DifficultyData.getDifficulty(level, mob);

        // Calculate spawn chance based on difficulty
        double spawnChance = calculateSpawnChance(difficulty, event.getSpawnType());

        // Apply spawn multiplier
        if (level.getRandom().nextDouble() > spawnChance) {
            if (debugMode) {
                IndustrialLogger.info(String.format(
                    "[SpawnManager] Denied spawn: %s at %s (difficulty=%.2f, chance=%.2f)",
                    mob.getType().getDescription().getString(),
                    mob.blockPosition(),
                    difficulty,
                    spawnChance
                ));
            }
            deniedSpawns.incrementAndGet();
            event.setSpawnCancelled(true);
            event.setCanceled(true);
            return;
        }

        // Allow spawn
        allowedSpawns.incrementAndGet();
        spawnCountsByType.merge(mob.getType(), 1, Integer::sum);

        if (debugMode) {
            IndustrialLogger.info(String.format(
                "[SpawnManager] Allowed spawn: %s at %s (difficulty=%.2f, chance=%.2f)",
                mob.getType().getDescription().getString(),
                mob.blockPosition(),
                difficulty,
                spawnChance
            ));
        }
    }

    /**
     * Handle mob join level event.
     * This is called when any entity joins the level.
     */
    @SubscribeEvent
    public static void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (!TriAxisConfig.enableSpawnEnhancement) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Monster mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Additional spawn logic can be added here
        // For example, force-spawning additional mobs based on difficulty
    }

    /**
     * Calculate spawn chance based on difficulty and spawn type.
     *
     * @param difficulty Current difficulty value
     * @param spawnType Type of spawn (natural, chunk generation, etc.)
     * @return Spawn chance (0.0-1.0)
     */
    private static double calculateSpawnChance(double difficulty, MobSpawnType spawnType) {
        // Base spawn chance from config
        double baseChance = TriAxisConfig.spawnMultiplier;

        // Difficulty bonus
        double difficultyBonus = difficulty * TriAxisConfig.difficultySpawnBonus;

        // Spawn type modifiers
        double typeModifier = switch (spawnType) {
            case NATURAL -> 1.0;
            case CHUNK_GENERATION -> 0.8;
            case SPAWNER -> 1.5; // Allow more spawner spawns
            case STRUCTURE -> 1.2;
            case BREEDING, CONVERSION, JOCKEY, PATROL, REINFORCEMENT, TRIGGERED -> 1.0;
            default -> 1.0;
        };

        // Calculate final chance
        double finalChance = (baseChance + difficultyBonus) * typeModifier;

        // Clamp to valid range
        return Math.max(0.0, Math.min(finalChance, TriAxisConfig.maxSpawnChance));
    }

    /**
     * Force spawn mobs at a location based on difficulty.
     *
     * @param level Server level
     * @param pos Spawn position
     * @param count Number of mobs to spawn
     * @param entityType Type of mob to spawn
     * @return Number of successfully spawned mobs
     */
    public static int forceSpawnMobs(ServerLevel level, BlockPos pos, int count, EntityType<? extends Mob> entityType) {
        int spawned = 0;

        for (int i = 0; i < count; i++) {
            Mob mob = entityType.create(level);
            if (mob == null) {
                continue;
            }

            // Set position with random offset
            double offsetX = (level.getRandom().nextDouble() - 0.5) * 8.0;
            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 8.0;
            BlockPos spawnPos = pos.offset((int) offsetX, 0, (int) offsetZ);

            // Find valid spawn position
            BlockPos validPos = findValidSpawnPos(level, spawnPos);
            if (validPos == null) {
                continue;
            }

            mob.moveTo(validPos.getX() + 0.5, validPos.getY(), validPos.getZ() + 0.5, 0, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(validPos), MobSpawnType.COMMAND, null, null);

            if (level.addFreshEntity(mob)) {
                spawned++;
                if (debugMode) {
                    IndustrialLogger.info(String.format(
                        "[SpawnManager] Force spawned: %s at %s",
                        mob.getType().getDescription().getString(),
                        validPos
                    ));
                }
            }
        }

        return spawned;
    }

    /**
     * Find a valid spawn position near the given position.
     *
     * @param level Server level
     * @param pos Starting position
     * @return Valid spawn position, or null if none found
     */
    private static BlockPos findValidSpawnPos(ServerLevel level, BlockPos pos) {
        // Try positions in a 3x3 area
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos testPos = pos.offset(dx, dy, dz);
                    if (level.getBlockState(testPos).isAir() &&
                        level.getBlockState(testPos.above()).isAir() &&
                        level.getBlockState(testPos.below()).isSolidRender(level, testPos.below())) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }

    // ========== Statistics and Debug ==========

    /**
     * Get total spawn attempts since last reset.
     */
    public static int getTotalSpawns() {
        return totalSpawns.get();
    }

    /**
     * Get allowed spawns since last reset.
     */
    public static int getAllowedSpawns() {
        return allowedSpawns.get();
    }

    /**
     * Get denied spawns since last reset.
     */
    public static int getDeniedSpawns() {
        return deniedSpawns.get();
    }

    /**
     * Get spawn counts by entity type.
     */
    public static Map<EntityType<?>, Integer> getSpawnCountsByType() {
        return new HashMap<>(spawnCountsByType);
    }

    /**
     * Reset spawn statistics.
     */
    public static void resetStatistics() {
        totalSpawns.set(0);
        allowedSpawns.set(0);
        deniedSpawns.set(0);
        spawnCountsByType.clear();
        lastResetTime.set(System.currentTimeMillis());
    }

    /**
     * Get time since last statistics reset (in seconds).
     */
    public static long getTimeSinceReset() {
        return (System.currentTimeMillis() - lastResetTime.get()) / 1000;
    }

    /**
     * Enable or disable debug mode.
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        IndustrialLogger.info("[SpawnManager] Debug mode: " + (enabled ? "ON" : "OFF"));
    }

    /**
     * Check if debug mode is enabled.
     */
    public static boolean isDebugMode() {
        return debugMode;
    }
}
