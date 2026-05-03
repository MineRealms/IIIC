package cn.minerealms.iic.pollution;

import cn.minerealms.iic.api.PollutionOverlayAPI;
import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.integration.gregtech.GTIntegration;
import cn.minerealms.iic.integration.gregtech.GTPollutionScanner;
import cn.minerealms.iic.integration.spore.SporeIntegration;
import cn.minerealms.iic.threat.ThreatManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Pollution Manager - Handles temporary and permanent pollution systems for Improved Mobs.
 *
 * <h2>Pollution System Design</h2>
 * <p>This manager implements a dual-layer pollution system:
 *
 * <h3>1. Temporary Pollution</h3>
 * <ul>
 *   <li>Chunk-based diffusion pollution</li>
 *   <li>Can be absorbed by environment (leaves, grass, water)</li>
 *   <li>Triggers mob attacks on machines</li>
 *   <li>Does not directly affect difficulty</li>
 * </ul>
 *
 * <h3>2. Permanent Pollution</h3>
 * <ul>
 *   <li>Global accumulated pollution value</li>
 *   <li>Converted from temporary pollution when threshold exceeded</li>
 *   <li>Directly increases ImprovedMobs difficulty</li>
 *   <li>Represents long-term environmental impact of industrial development</li>
 * </ul>
 *
 * <h3>Performance Optimization</h3>
 * <p>Uses extremely low-frequency staggered tick scanning to ensure
 * thousands of machines don't cause server lag. All heavy operations
 * are performed asynchronously off the main thread.
 *
 * @see ThreatManager
 * @see TriAxisConfig
 * @author ImprovedMobs Industrial Integration
 */
public class PollutionManager {

    /** Chunk-based temporary pollution map (temporary pollution - chunk diffusion) */
    private static final Map<ChunkPos, Double> temporaryPollution = new ConcurrentHashMap<>();

    /** Chunk-based environmental reduction cache (caches water and leaf absorption per chunk) */
    private static final Map<ChunkPos, Double> environmentalReductionCache = new ConcurrentHashMap<>();

    /** Global permanent pollution value (permanent pollution - global accumulation) */
    private static double permanentPollution = 0.0;

    /** Scan counter for staggered processing */
    private static int tickCounter = 0;

    // Configuration parameters are loaded from TriAxisConfig:
    // - tempToPermanentThreshold: Threshold for converting temporary to permanent pollution
    // - tempToPermanentRate: Conversion rate (per second)
    // - permanentToDifficultyRate: Threshold for converting permanent pollution to difficulty
    // - naturalDecayRate: Natural pollution decay rate per chunk per second
    // - leafAbsorptionRate, waterAbsorptionRate, grassAbsorptionRate: Environmental absorption rates

    /**
     * Adds to the global permanent pollution value.
     *
     * @param amount Amount of pollution to add
     */
    public static void addPermanentPollution(double amount) {
        permanentPollution += amount;
    }

    /**
     * Gets the current global permanent pollution value.
     *
     * @return Current permanent pollution level
     */
    public static double getPermanentPollution() {
        return permanentPollution;
    }

    /**
     * Gets the temporary pollution level for a specific chunk.
     *
     * @param pos Chunk position to query
     * @return Temporary pollution level, or 0 if no pollution
     */
    public static double getTemporaryPollution(ChunkPos pos) {
        return temporaryPollution.getOrDefault(pos, 0.0);
    }

    /**
     * Sets the temporary pollution level for a specific chunk.
     * Used by commands for direct pollution manipulation.
     *
     * @param level Server level (unused but kept for API consistency)
     * @param pos Chunk position to set
     * @param amount Pollution amount to set
     */
    public static void setChunkPollution(ServerLevel level, ChunkPos pos, double amount) {
        if (amount <= 0) {
            temporaryPollution.remove(pos);
        } else {
            temporaryPollution.put(pos, amount);
        }
    }

    /**
     * Gets the temporary pollution level for a specific chunk.
     * Alias for getTemporaryPollution for API consistency.
     *
     * @param level Server level (unused but kept for API consistency)
     * @param pos Chunk position to query
     * @return Temporary pollution level, or 0 if no pollution
     */
    public static double getChunkPollution(ServerLevel level, ChunkPos pos) {
        return getTemporaryPollution(pos);
    }

    /**
     * Sets the global permanent pollution value directly.
     * Used by commands for direct pollution manipulation.
     *
     * @param amount New permanent pollution value
     */
    public static void setPermanentPollution(double amount) {
        permanentPollution = Math.max(0, amount);
    }

    /**
     * Main tick method called from ServerTickEvent.
     * For performance, scanning is not done every tick but every 20 ticks (1 second).
     * Randomly samples loaded chunks for pollution accumulation and decay calculation.
     *
     * @param level The server level to process
     */
    public static void tick(ServerLevel level) {
        tickCounter++;

        // Pollution update interval (configurable)
        if (tickCounter % TriAxisConfig.pollutionUpdateInterval == 0) {
            IndustrialLogger.infoPollution(String.format(
                "[PollutionManager] Tick %d: Starting pollution update (interval=%d)",
                tickCounter, TriAxisConfig.pollutionUpdateInterval));
            processPollutionDecayAndScanning(level);
        }

        // Environment scan interval (configurable)
        if (tickCounter % TriAxisConfig.pollutionEnvironmentScanInterval == 0) {
            updateEnvironmentalCache(level);
        }
    }

    /**
     * Processes pollution decay and machine scanning asynchronously.
     * This method:
     * <ul>
     *   <li>Scans for active GT machines near players</li>
     *   <li>Calculates pollution generation from machines</li>
     *   <li>Converts temporary pollution to permanent when threshold exceeded</li>
     *   <li>Applies environmental reduction (leaves, water, grass)</li>
     *   <li>Triggers threat mechanisms based on pollution levels</li>
     * </ul>
     *
     * @param level The server level to process
     */
    private static void processPollutionDecayAndScanning(ServerLevel level) {
        Map<ChunkPos, Double> newPollutionThisSec = new HashMap<>();

        // Debug: 检查 level 和 server
        IndustrialLogger.infoPollution(String.format(
            "[PollutionManager] DEBUG: level=%s, server=%s",
            level != null ? level.dimension().location() : "null",
            level != null && level.getServer() != null ? "present" : "null"));

        // 获取所有在线玩家，在主线程安全获取
        List<net.minecraft.server.level.ServerPlayer> players = level.getServer().getPlayerList().getPlayers();

        // Debug: 玩家列表详情
        IndustrialLogger.infoPollution(String.format(
            "[PollutionManager] DEBUG: players.size()=%d, players.isEmpty()=%s",
            players.size(), players.isEmpty()));

        if (!players.isEmpty()) {
            for (int i = 0; i < Math.min(players.size(), 3); i++) {
                net.minecraft.server.level.ServerPlayer p = players.get(i);
                IndustrialLogger.infoPollution(String.format(
                    "[PollutionManager] DEBUG: player[%d]=%s, pos=%s, chunk=%s",
                    i, p.getName().getString(), p.blockPosition(), p.chunkPosition()));
            }
        }

        // 收集玩家附近的区块（避免扫描整个世界）
        Set<ChunkPos> chunksToScan = new HashSet<>();
        int scanRadius = TriAxisConfig.pollutionMachineScanRadius;

        IndustrialLogger.infoPollution(String.format(
            "[PollutionManager] DEBUG: scanRadius=%d", scanRadius));

        for (net.minecraft.server.level.ServerPlayer player : players) {
            ChunkPos center = player.chunkPosition();
            IndustrialLogger.infoPollution(String.format(
                "[PollutionManager] DEBUG: Collecting chunks around player %s at chunk %s",
                player.getName().getString(), center));

            int chunksBeforePlayer = chunksToScan.size();
            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    chunksToScan.add(new ChunkPos(center.x + x, center.z + z));
                }
            }
            IndustrialLogger.infoPollution(String.format(
                "[PollutionManager] DEBUG: Added %d chunks for player %s (total now: %d)",
                chunksToScan.size() - chunksBeforePlayer, player.getName().getString(), chunksToScan.size()));
        }

        // 如果没有玩家，扫描spawn附近的区块（避免完全不扫描）
        if (chunksToScan.isEmpty()) {
            IndustrialLogger.infoPollution("[PollutionManager] DEBUG: No players, using spawn area");
            net.minecraft.core.BlockPos spawnPos = level.getSharedSpawnPos();
            ChunkPos spawnChunk = new ChunkPos(spawnPos);
            IndustrialLogger.infoPollution(String.format(
                "[PollutionManager] DEBUG: Spawn pos=%s, chunk=%s", spawnPos, spawnChunk));

            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    chunksToScan.add(new ChunkPos(spawnChunk.x + x, spawnChunk.z + z));
                }
            }
            IndustrialLogger.infoPollution(String.format(
                "[PollutionManager] DEBUG: Added %d chunks around spawn", chunksToScan.size()));
        }

        IndustrialLogger.infoPollution(String.format(
            "[PollutionManager] Main thread: players=%d, chunksToScan=%d",
            players.size(), chunksToScan.size()));

        // Debug: 显示前几个要扫描的区块
        int chunkIndex = 0;
        for (ChunkPos cp : chunksToScan) {
            if (chunkIndex < 5) {
                IndustrialLogger.infoPollution(String.format(
                    "[PollutionManager] DEBUG: chunksToScan[%d]=%s, hasChunk=%s",
                    chunkIndex, cp, level.hasChunk(cp.x, cp.z)));
            }
            chunkIndex++;
            if (chunkIndex >= 5) break;
        }

        CompletableFuture.runAsync(() -> {
            int totalMachinesFound = 0;
            int activeMachinesFound = 0;

            IndustrialLogger.infoPollution(String.format(
                "[PollutionScanner] Starting scan: chunksToScan=%d",
                chunksToScan.size()));

            int scannedCount = 0;
            for (ChunkPos scanPos : chunksToScan) {
                scannedCount++;

                // Debug: 前5个区块的详细信息
                if (scannedCount <= 5) {
                    IndustrialLogger.infoPollution(String.format(
                        "[PollutionScanner] DEBUG: Checking chunk[%d/%d] %s, hasChunk=%s",
                        scannedCount, chunksToScan.size(), scanPos, level.hasChunk(scanPos.x, scanPos.z)));
                }

                if (level.hasChunk(scanPos.x, scanPos.z)) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(scanPos.x, scanPos.z);
                    int chunkMachines = 0;
                    int chunkActiveMachines = 0;

                    // Debug: 区块实体数量
                    if (scannedCount <= 5) {
                        IndustrialLogger.infoPollution(String.format(
                            "[PollutionScanner] DEBUG: Chunk %s has %d block entities",
                            scanPos, chunk.getBlockEntities().size()));
                    }

                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (GTIntegration.isGTMachine(be)) {
                            totalMachinesFound++;
                            chunkMachines++;

                            // Debug: 前3台机器的详细信息
                            if (totalMachinesFound <= 3) {
                                IndustrialLogger.infoPollution(String.format(
                                    "[PollutionScanner] DEBUG: Machine[%d] at %s, type=%s",
                                    totalMachinesFound, be.getBlockPos(), be.getClass().getSimpleName()));
                            }

                            // 检查机器是否有能量或活跃（使用已验证的方法）
                            boolean hasEnergy = GTIntegration.hasEnergyOrActive(be);

                            if (totalMachinesFound <= 3) {
                                IndustrialLogger.infoPollution(String.format(
                                    "[PollutionScanner] DEBUG: Machine[%d] hasEnergyOrActive=%s",
                                    totalMachinesFound, hasEnergy));
                            }

                            if (hasEnergy) {
                                activeMachinesFound++;
                                chunkActiveMachines++;

                                // 获取电压等级作为污染基础
                                int tier = GTIntegration.getVoltageTier(be);

                                if (totalMachinesFound <= 3) {
                                    IndustrialLogger.infoPollution(String.format(
                                        "[PollutionScanner] DEBUG: Machine[%d] tier=%d",
                                        totalMachinesFound, tier));
                                }

                                // 污染计算公式：basePollution × (tier+1)^exponent × multiblockBonus
                                // 设计目标：1台EV多方块24小时 = 240污染
                                // EV = tier 4: 0.21 × (4+1)^1.3 × 2.0 = 2.77 /秒 × 86400秒 = 240污染
                                double tierMultiplier = Math.pow(tier + 1, TriAxisConfig.pollutionTierExponent);
                                double pollutionValue = TriAxisConfig.basePollutionPerSecond * tierMultiplier;

                                // 多方块结构产生更多污染
                                boolean isMultiblock = GTIntegration.isMultiblock(be);
                                if (isMultiblock) {
                                    pollutionValue *= TriAxisConfig.multiblockPollutionMultiplier;
                                }

                                if (totalMachinesFound <= 3) {
                                    IndustrialLogger.infoPollution(String.format(
                                        "[PollutionScanner] DEBUG: Machine[%d] isMultiblock=%s, pollutionValue=%.6f",
                                        totalMachinesFound, isMultiblock, pollutionValue));
                                }

                                ChunkPos cPos = new ChunkPos(be.getBlockPos());
                                newPollutionThisSec.merge(cPos, pollutionValue, Double::sum);

                                IndustrialLogger.infoPollution(String.format(
                                    "[PollutionScanner] Machine at %s: tier=%d, multiblock=%s, pollution=%.4f/tick",
                                    be.getBlockPos(), tier, isMultiblock, pollutionValue));
                            }
                        }
                    }

                    if (chunkMachines > 0) {
                        IndustrialLogger.infoPollution(String.format(
                            "[PollutionScanner] Chunk %s: machines=%d, active=%d",
                            scanPos, chunkMachines, chunkActiveMachines));
                    }

                    // GT Pollution Integration - scan GT pollution sources
                    if (GTIntegration.isGTLoaded()) {
                        try {
                            GTPollutionScanner.GTHazardInfo gtHazard = GTPollutionScanner.scanChunkHazards(level, scanPos);
                            if (gtHazard != null && gtHazard.sourceCount() > 0) {
                                double gtMultiplier = GTPollutionScanner.calculateSourceMultiplier(gtHazard.sourceCount());
                                double gtPollutionContribution = gtHazard.strength() * gtMultiplier * TriAxisConfig.gtPollutionWeight;

                                // Add to temporary pollution for this chunk
                                newPollutionThisSec.merge(scanPos, gtPollutionContribution, Double::sum);

                                IndustrialLogger.infoPollution(String.format(
                                        "[PollutionScanner] GT Pollution: chunk=%s, strength=%.2f, sources=%d, contribution=%.4f",
                                        scanPos, gtHazard.strength(), gtHazard.sourceCount(), gtPollutionContribution));
                            }
                        } catch (Exception e) {
                            IndustrialLogger.error("[PollutionScanner] Error scanning GT pollution at chunk " + scanPos + ": " + e.getMessage(), e);
                        }
                    }
                } else {
                    // Debug: 区块未加载
                    if (scannedCount <= 5) {
                        IndustrialLogger.infoPollution(String.format(
                            "[PollutionScanner] DEBUG: Chunk %s not loaded, skipping", scanPos));
                    }
                }
            }

            IndustrialLogger.infoPollution(String.format(
                "[PollutionScanner] Scan complete: scannedChunks=%d, totalMachines=%d, activeMachines=%d, pollutionSources=%d",
                chunksToScan.size(), totalMachinesFound, activeMachinesFound, newPollutionThisSec.size()));
        }).thenAccept(v -> {
            // Process temporary pollution conversion to permanent pollution
            double totalTempPollution = 0;
            for (ChunkPos cPos : temporaryPollution.keySet()) {
                double current = temporaryPollution.get(cPos);
                totalTempPollution += current;

                // Threshold-based conversion to permanent pollution
                // Only pollution above threshold converts (prevents low pollution from converting)
                double converted = 0.0;
                if (current > TriAxisConfig.tempToPermanentThreshold) {
                    double excess = current - TriAxisConfig.tempToPermanentThreshold;
                    converted = excess * TriAxisConfig.tempToPermanentRate;
                    addPermanentPollution(converted);
                }

                if (IndustrialLogger.isDebugEnabled() && level.getGameTime() % 100 == 0 && converted > 0) {
                    IndustrialLogger.debugPollution(String.format(
                            "Chunk %s: Temp pollution %.2f (excess: %.2f) -> Converting %.4f to permanent",
                            cPos, current, current - TriAxisConfig.tempToPermanentThreshold, converted));
                }

                // Natural decay: proportional to current pollution (0.2% per second by default)
                double naturalDecay = current * TriAxisConfig.naturalDecayRate;

                // Environmental absorption with absolute cap (configurable)
                // envScore is already in pollution/sec from calculateChunkReduction()
                double envScore = getSurroundingEnvironmentalReduction(cPos);
                // Cap: min(envScore, current * maxPercent)
                // This prevents "plant forest = invincible" while still making environment useful
                double envAbsorb = Math.min(envScore, current * TriAxisConfig.pollutionEnvAbsorptionMaxPercent);

                double reduction = naturalDecay + envAbsorb;
                double added = newPollutionThisSec.getOrDefault(cPos, 0.0);
                double nextVal = Math.max(0.0, current - reduction + added);

                // Pollution diffusion to neighbors (Factorio style - creates pollution clouds, configurable)
                nextVal = applyPollutionDiffusion(temporaryPollution, cPos, nextVal, TriAxisConfig.pollutionDiffusionRate);

                // Remove low pollution chunks (performance optimization, configurable threshold)
                if (nextVal <= TriAxisConfig.pollutionRemovalThreshold) {
                    temporaryPollution.remove(cPos);
                    PollutionOverlayAPI.updatePollutionCache(cPos, 0.0); // Clear from overlay cache
                } else {
                    temporaryPollution.put(cPos, nextVal);
                    PollutionOverlayAPI.updatePollutionCache(cPos, nextVal); // Update overlay cache

                    // Check and trigger threat mechanisms (based on voltage tier and pollution)
                    double avgTier = ThreatManager.getChunkAverageVoltageTier(level, cPos);
                    ThreatManager.checkAndTriggerThreats(level, cPos, nextVal, avgTier);
                }
            }

            // New pollution chunks
            for (Map.Entry<ChunkPos, Double> entry : newPollutionThisSec.entrySet()) {
                if (!temporaryPollution.containsKey(entry.getKey())) {
                    temporaryPollution.put(entry.getKey(), entry.getValue());
                    PollutionOverlayAPI.updatePollutionCache(entry.getKey(), entry.getValue()); // Update overlay cache
                }
            }

            // When permanent pollution accumulates to a certain level, increase global difficulty
            if (permanentPollution >= TriAxisConfig.permanentToDifficultyRate) {
                io.github.flemmli97.improvedmobs.difficulty.DifficultyData diffData =
                        io.github.flemmli97.improvedmobs.difficulty.DifficultyData.get(level.getServer());
                diffData.addDifficulty((float)permanentPollution, level.getServer());

                if (IndustrialLogger.isDebugEnabled()) {
                    IndustrialLogger.debugPollution(String.format(
                            "Added %.2f permanent pollution to global difficulty (Total temp: %.2f)",
                            permanentPollution, totalTempPollution));
                }

                permanentPollution = 0.0;
            }

            // Spore pollution feedback - accelerate Hivemind growth based on pollution (configurable threshold)
            // Must be called on main thread (modifies entity data)
            if (SporeIntegration.isSporeLoaded() && totalTempPollution > TriAxisConfig.pollutionSporeFeedbackThreshold) {
                final double pollutionForFeedback = totalTempPollution;  // Make effectively final
                level.getServer().execute(() -> {
                    SporeIntegration.applyPollutionFeedback(level, pollutionForFeedback);
                });
            }
        });
    }

    /**
     * Gets the total environmental reduction from surrounding 4 chunks.
     * Environmental reduction comes from leaves, water, and grass blocks.
     *
     * @param center Center chunk position
     * @return Total environmental reduction value
     */
    private static double getSurroundingEnvironmentalReduction(ChunkPos center) {
        double totalReduction = 0.0;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                ChunkPos p = new ChunkPos(center.x + x, center.z + z);
                totalReduction += environmentalReductionCache.getOrDefault(p, 0.0);
            }
        }
        return totalReduction;
    }

    /**
     * Applies pollution diffusion to neighboring chunks.
     * Creates pollution cloud spread effect (Factorio core experience).
     */
    private static double applyPollutionDiffusion(Map<ChunkPos, Double> pollutionMap, ChunkPos center, double selfPollution, double diffusionRate) {
        double totalDiffused = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos neighbor = new ChunkPos(center.x + dx, center.z + dz);
                double neighborPollution = pollutionMap.getOrDefault(neighbor, 0.0);
                double diff = (selfPollution - neighborPollution) * diffusionRate;
                totalDiffused += diff;
            }
        }
        return selfPollution - totalDiffused;
    }

    /**
     * Updates the environmental reduction cache in a background thread.
     * Scans chunks for leaves and water blocks to calculate pollution absorption.
     * Since vanilla water is Blocks.WATER and leaves are in BlockTags.LEAVES,
     * we read block states directly in a separate thread.
     *
     * @param level The server level to scan
     */
    private static void updateEnvironmentalCache(ServerLevel level) {

        new Thread(() -> {
            for (ChunkPos cPos : temporaryPollution.keySet()) {
                // Only scan chunks near pollution to avoid scanning too many useless areas
                for (int cx = -4; cx <= 4; cx++) {
                    for (int cz = -4; cz <= 4; cz++) {
                        ChunkPos scanPos = new ChunkPos(cPos.x + cx, cPos.z + cz);
                        if (!environmentalReductionCache.containsKey(scanPos) && level.hasChunk(scanPos.x, scanPos.z)) {
                            LevelChunk chunk = level.getChunk(scanPos.x, scanPos.z);
                            double reduction = calculateChunkReduction(chunk);
                            environmentalReductionCache.put(scanPos, reduction);
                        }
                    }
                }
            }
        }, "ImprovedMobs-Pollution-Scanner").start();
    }

    /**
     * Ultra-fast chunk scanning using ChunkSection's two-layer loop instead of three-layer coordinate loop.
     * This greatly optimizes performance!
     *
     * <p>Pollution absorption formula (configurable via TriAxisConfig):
     * <ul>
     *   <li>Grass block: grassBlockAbsorption (default 0.0015 /sec, typical chunk ~200 blocks = 0.3 /sec)</li>
     *   <li>Leaves: leavesAbsorption (default 0.0025 /sec, typical chunk ~150 blocks = 0.375 /sec)</li>
     *   <li>Water: waterAbsorption (default 0.0012 /sec, typical chunk ~50 blocks = 0.06 /sec)</li>
     *   <li>Grass/flowers: grassAbsorption (default 0.001 /sec)</li>
     *   <li>Logs: logAbsorption (default 0.001 /sec)</li>
     * </ul>
     * <p>Design target: Typical natural chunk absorbs ~0.735 pollution/sec
     * <p>5 HV machines produce 648 pollution in 1 minute, needs ~15 minutes to absorb
     *
     * @param chunk The chunk to scan
     * @return Total environmental reduction value for the chunk (pollution per second)
     */
    private static double calculateChunkReduction(LevelChunk chunk) {
        int grassBlockCount = 0;
        int leafCount = 0;
        int waterCount = 0;
        int grassPlantCount = 0;
        int logCount = 0;

        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir()) continue;

            // For safe async thread access, we only do basic blockId comparison
            // (classic three-layer loop optimized version shown here)
            // Direct PaletteContainer access is not thread-safe
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.is(Blocks.GRASS_BLOCK)) {
                            grassBlockCount++;
                        } else if (state.is(BlockTags.LEAVES)) {
                            leafCount++;
                        } else if (state.is(Blocks.WATER)) {
                            waterCount++;
                        } else if (state.is(BlockTags.FLOWERS) ||
                                   state.is(Blocks.GRASS) ||
                                   state.is(Blocks.TALL_GRASS) ||
                                   state.is(Blocks.FERN) ||
                                   state.is(Blocks.LARGE_FERN)) {
                            grassPlantCount++;
                        } else if (state.is(BlockTags.LOGS)) {
                            logCount++;
                        }
                    }
                }
            }
        }

        return (grassBlockCount * TriAxisConfig.grassBlockAbsorption) +
               (leafCount * TriAxisConfig.leavesAbsorption) +
               (waterCount * TriAxisConfig.waterAbsorption) +
               (grassPlantCount * TriAxisConfig.grassAbsorption) +
               (logCount * TriAxisConfig.logAbsorption);
    }

    /**
     * Cleans pollution in a radius around a position (for Air Scrubber integration).
     * <p>
     * The cleaning amount decreases with distance from the center.
     * This method is called when GT Air Scrubber machines complete recipes.
     *
     * @param level         The server level
     * @param center        Center position of the cleaning effect
     * @param radiusChunks  Radius in chunks
     * @param amount        Base cleaning amount
     */
    public static void cleanPollutionInRadius(ServerLevel level, BlockPos center, int radiusChunks, double amount) {
        ChunkPos centerChunk = new ChunkPos(center);
        double efficiency = TriAxisConfig.airScrubberEfficiency;

        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                ChunkPos targetChunk = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                double distance = Math.sqrt(dx * dx + dz * dz);
                double cleanAmount = (amount * efficiency) / Math.max(1.0, distance);

                temporaryPollution.computeIfPresent(targetChunk, (k, v) -> {
                    double newValue = v - cleanAmount;
                    if (IndustrialLogger.isDebugEnabled()) {
                        IndustrialLogger.debugPollution(String.format(
                                "Air Scrubber cleaning chunk %s: %.2f -> %.2f (cleaned %.4f)",
                                targetChunk, v, Math.max(0, newValue), cleanAmount));
                    }
                    return newValue > 0 ? newValue : null; // Remove if <= 0
                });
            }
        }
    }
}