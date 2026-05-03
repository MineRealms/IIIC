package cn.minerealms.iic.difficulty;

import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.integration.gregtech.GTIntegration;
import cn.minerealms.iic.industrial.TriAxisConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class MachineScanner {

    public record ScanResult(List<Integer> tiers, List<Double> weights) {}

    public static ScanResult scanNearbyMachines(Player player, int radius) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        List<Integer> tiers = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius / 2, -radius),
                                                   center.offset(radius, radius / 2, radius))) {
            BlockEntity be = level.getBlockEntity(pos);

            // Filter out multiblock parts (energy hatches, etc.) to avoid double-counting
            if (GTIntegration.isGTMachine(be) &&
                !GTIntegration.isMultiblockPart(be) &&
                GTIntegration.hasEnergyOrActive(be)) {
                int tier = GTIntegration.getVoltageTier(be);
                if (tier >= 0) {
                    double distSq = pos.distSqr(center);
                    double distanceWeight = 1.0 / (1.0 + Math.sqrt(distSq) * 0.1);
                    double typeWeight = GTIntegration.isMultiblock(be) ? 5.0 : 1.0;

                    tiers.add(tier);
                    weights.add(distanceWeight * typeWeight);

                    // Debug logging
                    IndustrialLogger.debugMachine(String.format(
                            "Scanned machine at %s | Tier: %d | Active: %s",
                            pos, tier, GTIntegration.isMachineActive(be)));
                }
            }
        }

        // Output scan results
        if (IndustrialLogger.isDebugEnabled() && !tiers.isEmpty()) {
            IndustrialLogger.debugMachine(String.format(
                    "Scan complete: Found %d machines with energy", tiers.size()));
        }

        return new ScanResult(tiers, weights);
    }
    
    public static double scanNearbyVoltageTier(ServerLevel level, BlockPos center) {
        int maxTier = 0;
        int radius = 8;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius / 2, -radius),
                                                   center.offset(radius, radius / 2, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            // Filter out multiblock parts to avoid double-counting
            if (GTIntegration.isGTMachine(be) &&
                !GTIntegration.isMultiblockPart(be) &&
                GTIntegration.hasEnergyOrActive(be)) {
                int tier = GTIntegration.getVoltageTier(be);
                if (tier > maxTier) {
                    maxTier = tier;
                }
            }
        }
        return maxTier;
    }

    public static double scanNearbyVoltageTierSafely(ServerLevel level, BlockPos center) {
        int maxTier = 0;
        int radius = 8;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius / 2, -radius),
                                                   center.offset(radius, radius / 2, radius))) {
            // Check if chunk is loaded before trying to get block entity to avoid triggering chunk loads
            if (level.hasChunkAt(pos)) {
                try {
                    BlockEntity be = level.getBlockEntity(pos);
                    // Filter out multiblock parts to avoid double-counting
                    if (GTIntegration.isGTMachine(be) &&
                        !GTIntegration.isMultiblockPart(be) &&
                        GTIntegration.hasEnergyOrActive(be)) {
                        int tier = GTIntegration.getVoltageTier(be);
                        if (tier > maxTier) {
                            maxTier = tier;
                        }
                    }
                } catch (Exception e) {
                    // Ignore exceptions during async access
                }
            }
        }
        return maxTier;
    }

    public static int scanNearbyVoltageTierMedianSafely(ServerLevel level, BlockPos center, int radius) {
        try {
            List<Integer> tiers = new ArrayList<>();
            int scannedBlocks = 0;
            int loadedChunks = 0;
            int gtMachines = 0;
            int multiblockParts = 0;

            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius / 2, -radius),
                                                       center.offset(radius, radius / 2, radius))) {
                scannedBlocks++;
                if (level.hasChunkAt(pos)) {
                    loadedChunks++;
                    try {
                        BlockEntity be = level.getBlockEntity(pos);
                        boolean isGT = GTIntegration.isGTMachine(be);
                        if (isGT) {
                            gtMachines++;

                            // Filter out multiblock parts (energy hatches, etc.)
                            boolean isPart = GTIntegration.isMultiblockPart(be);
                            if (isPart) {
                                multiblockParts++;
                                continue; // Skip multiblock parts
                            }

                            boolean hasEnergy = GTIntegration.hasEnergyOrActive(be);
                            if (hasEnergy) {
                                int tier = GTIntegration.getVoltageTier(be);
                                if (tier >= 0) {
                                    tiers.add(tier);
                                }
                            }
                        }
                    } catch (Exception e) {
                        IndustrialLogger.error("[MachineScanner] Error scanning block at " + pos, e);
                    }
                }
            }

            IndustrialLogger.infoMachineScan(String.format(
                "Scan complete: scannedBlocks=%d, loadedChunks=%d, gtMachines=%d, multiblockParts=%d (filtered), tiersFound=%d",
                scannedBlocks, loadedChunks, gtMachines, multiblockParts, tiers.size()));

            if (tiers.isEmpty()) {
                IndustrialLogger.infoMachineScan("No machines with energy found, returning tier 0");
                return 0;
            }

            // 取中位数 (Median)
            tiers.sort(Integer::compareTo);
            int median = tiers.get(tiers.size() / 2);

            IndustrialLogger.infoMachineScan(String.format(
                "Median tier: %d (from %d machines)", median, tiers.size()));

            return median;
        } catch (Exception e) {
            IndustrialLogger.error("[MachineScanner] Fatal error in scanNearbyVoltageTierMedianSafely", e);
            return 0;
        }
    }
}
