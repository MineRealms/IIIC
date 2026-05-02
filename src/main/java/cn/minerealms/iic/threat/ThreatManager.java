package cn.minerealms.iic.threat;

import cn.minerealms.iic.ai.CreeperTargetMachineGoal;
import cn.minerealms.iic.ai.ZombieDestroyMachineGoal;
import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.integration.gregtech.GTIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threat Manager - Triggers different threat mechanisms based on voltage tier and pollution levels.
 *
 * <p>This system creates dynamic threats that scale with industrial progression:
 * <ul>
 *   <li><b>MV (Tier 2)</b>: Nearby zombies attack machines (passive threat)</li>
 *   <li><b>HV (Tier 3+)</b>: Active zombie spawning to attack machines</li>
 *   <li><b>HV + High Pollution</b>: Active creeper spawning to attack machines</li>
 *   <li><b>Extreme Pollution</b>: Charged creeper spawning</li>
 * </ul>
 *
 * <p>Threat levels are configurable via {@link TriAxisConfig} and can be adjusted
 * based on gameplay balance requirements.
 *
 * @see TriAxisConfig
 * @see PollutionManager
 * @author ImprovedMobs Industrial Integration
 */
public class ThreatManager {

    /** Global threat entity counter (performance limit) */
    private static final AtomicInteger globalThreatEntityCount = new AtomicInteger(0);

    /**
     * Checks and triggers threat mechanisms based on current conditions.
     * This method should be called once per second.
     *
     * @param level The server level where threats will spawn
     * @param chunkPos The chunk position to check for threats
     * @param pollution Current pollution level in the chunk
     * @param avgVoltageTier Average voltage tier of machines in the chunk
     */
    public static void checkAndTriggerThreats(ServerLevel level, ChunkPos chunkPos, double pollution, double avgVoltageTier) {
        // Global entity limit check (performance protection, configurable)
        if (globalThreatEntityCount.get() >= TriAxisConfig.threatMaxGlobalEntities) {
            if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % TriAxisConfig.debugLogInterval == 0) {
                IndustrialLogger.debugPollution(String.format(
                    "[IIC-Threat] Global entity limit reached (%d/%d), skipping spawn",
                    globalThreatEntityCount.get(), TriAxisConfig.threatMaxGlobalEntities));
            }
            return;
        }

        // MV stage: Nearby zombies will attack machines (implemented via AI, no handling needed here)

        // HV stage: Active threat spawning with dynamic rates
        if (avgVoltageTier >= 3.0) { // HV+
            // Dynamic spawn rate: increases with pollution (configurable divisor)
            double baseZombieRate = TriAxisConfig.zombieSpawnChance;
            double zombieSpawnRate = baseZombieRate * (1.0 + pollution / TriAxisConfig.threatSpawnRatePollutionDivisor);

            // Spawn zombie waves
            if (pollution >= TriAxisConfig.hvZombieSpawnThreshold && level.random.nextDouble() < zombieSpawnRate) {
                int waveSize = calculateWaveSize(pollution, avgVoltageTier, false);
                spawnHostileWave(level, chunkPos, waveSize, false);
            }

            // Dynamic creeper spawn rate (configurable divisor)
            double baseCreeperRate = TriAxisConfig.creeperSpawnChance;
            double creeperSpawnRate = baseCreeperRate * (1.0 + pollution / TriAxisConfig.threatSpawnRatePollutionDivisor);

            // Spawn creeper waves
            if (pollution >= TriAxisConfig.hvCreeperSpawnThreshold && level.random.nextDouble() < creeperSpawnRate) {
                boolean includeCharged = pollution >= TriAxisConfig.chargedCreeperThreshold;
                int waveSize = calculateWaveSize(pollution, avgVoltageTier, true);
                spawnHostileWave(level, chunkPos, waveSize, includeCharged);
            }
        }
    }

    /**
     * Calculate wave size based on pollution and voltage tier.
     * Formula: base + (pollution / divisor) + (tier / divisor)
     *
     * @param pollution Current pollution level
     * @param avgVoltageTier Average voltage tier
     * @param isCreeper Whether this is a creeper wave (smaller waves)
     * @return Wave size (capped at max)
     */
    private static int calculateWaveSize(double pollution, double avgVoltageTier, boolean isCreeper) {
        int base = isCreeper ? TriAxisConfig.waveCreeperBaseSize : TriAxisConfig.waveBaseSize;
        int pollutionBonus = (int)(pollution / TriAxisConfig.wavePollutionDivisor);
        int tierBonus = (int)(avgVoltageTier / TriAxisConfig.waveTierDivisor);

        int total = base + pollutionBonus + tierBonus;

        // Cap at max size (performance limit)
        int maxSize = isCreeper ? TriAxisConfig.waveCreeperMaxSize : TriAxisConfig.waveMaxSize;
        return Math.min(total, maxSize);
    }

    /**
     * Spawn a wave of hostile mobs.
     * Thread-safe: can be called from async context.
     *
     * @param level The server level
     * @param chunkPos The chunk position
     * @param count Number of mobs to spawn
     * @param includeCharged Whether to include charged creepers
     */
    private static void spawnHostileWave(ServerLevel level, ChunkPos chunkPos, int count, boolean includeCharged) {
        int zombies = 0;
        int creepers = 0;
        int chargedCreepers = 0;

        for (int i = 0; i < count; i++) {
            // Mix of zombies and creepers
            if (includeCharged && level.random.nextDouble() < TriAxisConfig.waveCreeperChance) {
                // Creeper wave
                boolean charged = level.random.nextDouble() < TriAxisConfig.chargedCreeperChance;
                spawnHostileCreeper(level, chunkPos, charged);
                if (charged) {
                    chargedCreepers++;
                } else {
                    creepers++;
                }
            } else {
                // Zombie wave
                spawnHostileZombie(level, chunkPos);
                zombies++;
            }
        }

        if (IndustrialLogger.isDebugEnabled()) {
            IndustrialLogger.debugPollution(String.format(
                    "Spawned hostile wave at chunk %s: %d zombies, %d creepers, %d charged creepers (total: %d)",
                    chunkPos, zombies, creepers, chargedCreepers, count));
        }
    }

    /**
     * Spawns a hostile zombie that will attack machines.
     * The zombie is given high-priority AI to target and destroy machines.
     *
     * @param level The server level to spawn in
     * @param chunkPos The chunk position for spawn location
     */
    private static void spawnHostileZombie(ServerLevel level, ChunkPos chunkPos) {
        // Check global limit
        if (globalThreatEntityCount.get() >= TriAxisConfig.threatMaxGlobalEntities) {
            return;
        }

        BlockPos spawnPos = findSpawnPosition(level, chunkPos);
        if (spawnPos == null) return;

        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie != null) {
            zombie.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);

            // Add machine-attacking AI (high priority)
            zombie.goalSelector.addGoal(1, new ZombieDestroyMachineGoal(zombie));

            // Mark as threat entity
            zombie.getPersistentData().putBoolean("IIC_ThreatEntity", true);

            level.addFreshEntity(zombie);
            globalThreatEntityCount.incrementAndGet();

            if (IndustrialLogger.isDebugEnabled()) {
                IndustrialLogger.debugPollution(String.format(
                        "Spawned hostile zombie at %s (chunk %s) [Global: %d/%d]",
                        spawnPos, chunkPos, globalThreatEntityCount.get(), TriAxisConfig.threatMaxGlobalEntities));
            }
        }
    }

    /**
     * Spawns a hostile creeper that will attack machines.
     * Can optionally spawn as a charged creeper for extreme pollution levels.
     *
     * @param level The server level to spawn in
     * @param chunkPos The chunk position for spawn location
     * @param charged Whether to spawn as a charged creeper
     */
    private static void spawnHostileCreeper(ServerLevel level, ChunkPos chunkPos, boolean charged) {
        // Check global limit
        if (globalThreatEntityCount.get() >= TriAxisConfig.threatMaxGlobalEntities) {
            return;
        }

        BlockPos spawnPos = findSpawnPosition(level, chunkPos);
        if (spawnPos == null) return;

        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper != null) {
            creeper.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            creeper.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);

            // Add machine-attacking AI (high priority)
            creeper.goalSelector.addGoal(1, new CreeperTargetMachineGoal(creeper));

            // Mark as threat entity
            creeper.getPersistentData().putBoolean("IIC_ThreatEntity", true);

            level.addFreshEntity(creeper);
            globalThreatEntityCount.incrementAndGet();

            // Charged creeper: Summon lightning immediately after spawn
            if (charged) {
                net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                if (lightning != null) {
                    lightning.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    lightning.setVisualOnly(true); // Visual effect only, no damage
                    level.addFreshEntity(lightning);
                }
            }

            if (IndustrialLogger.isDebugEnabled()) {
                IndustrialLogger.debugPollution(String.format(
                        "Spawned hostile %screeper at %s (chunk %s) [Global: %d/%d]",
                        charged ? "CHARGED " : "", spawnPos, chunkPos,
                        globalThreatEntityCount.get(), TriAxisConfig.threatMaxGlobalEntities));
            }
        }
    }

    /**
     * Finds a suitable spawn position within the chunk.
     * Ensures the position is valid for mob spawning (solid ground, air above).
     *
     * @param level The server level
     * @param chunkPos The chunk to search in
     * @return A valid spawn position, or null if none found
     */
    private static BlockPos findSpawnPosition(ServerLevel level, ChunkPos chunkPos) {
        // Randomly select position within chunk
        int x = chunkPos.getMinBlockX() + level.random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + level.random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        BlockPos pos = new BlockPos(x, y, z);

        // Check if spawn position is valid
        if (!level.hasChunkAt(pos)) return null;
        if (!level.getBlockState(pos).isAir()) return null;
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) return null;

        return pos;
    }

    /**
     * Calculates the average voltage tier of machines in a chunk.
     * Only considers machines that have energy or are active.
     *
     * @param level The server level
     * @param chunkPos The chunk position to scan
     * @return Average voltage tier, or 0 if no machines found
     */
    public static double getChunkAverageVoltageTier(ServerLevel level, ChunkPos chunkPos) {
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) return 0;

        int totalTier = 0;
        int count = 0;

        var chunk = level.getChunk(chunkPos.x, chunkPos.z);
        for (var be : chunk.getBlockEntities().values()) {
            if (GTIntegration.isGTMachine(be) && GTIntegration.hasEnergyOrActive(be)) {
                int tier = GTIntegration.getVoltageTier(be);
                if (tier >= 0) {
                    totalTier += tier;
                    count++;
                }
            }
        }

        return count > 0 ? (double) totalTier / count : 0;
    }

    /**
     * Called when a threat entity is removed (death, despawn, etc.)
     * Decrements the global counter.
     *
     * @param entity The entity being removed
     */
    public static void onThreatEntityRemoved(net.minecraft.world.entity.Entity entity) {
        if (entity.getPersistentData().getBoolean("IIC_ThreatEntity")) {
            int current = globalThreatEntityCount.decrementAndGet();
            if (current < 0) {
                globalThreatEntityCount.set(0); // Safety check
            }
        }
    }

    /**
     * Gets the current global threat entity count.
     *
     * @return Current count
     */
    public static int getGlobalThreatEntityCount() {
        return globalThreatEntityCount.get();
    }

    /**
     * Resets the global threat entity counter.
     * Should be called on server restart or world unload.
     */
    public static void resetGlobalCounter() {
        globalThreatEntityCount.set(0);
    }
}
