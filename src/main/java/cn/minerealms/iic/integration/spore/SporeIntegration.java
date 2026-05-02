package cn.minerealms.iic.integration.spore;

import cn.minerealms.iic.difficulty.DifficultyManager;
import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.pollution.PollutionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spore 2.0 Integration - Enhanced pollution feedback and evolution system
 *
 * <h2>Core Concepts (Factorio-inspired)</h2>
 * <ul>
 *   <li>Hiveminds (Proto) = Nests/Spawners</li>
 *   <li>Biomass = Nest size/strength</li>
 *   <li>Evolution = Hiveminds count + Biomass + Pollution + Voltage</li>
 *   <li>Pollution feedback = Accelerates Hivemind spread and Proto growth</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>Reflection initialization: Thread-safe singleton</li>
 *   <li>Data collection: Async-safe (reads only)</li>
 *   <li>Mob buffing: Main thread only</li>
 *   <li>Pollution feedback: Scheduled on main thread</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * <ul>
 *   <li>Cached reflection methods (initialized once)</li>
 *   <li>Async data collection where possible</li>
 *   <li>Throttled pollution feedback (every 5 seconds)</li>
 * </ul>
 *
 * @author ImprovedMobs Team
 */
public class SporeIntegration {

    // ==================== Reflection Cache ====================

    private static volatile boolean checkDone = false;
    private static volatile boolean isSporeLoaded = false;

    // Spore classes
    private static Class<?> protoClass;
    private static Class<?> sporeSavedDataClass;
    private static Class<?> organoidClass;

    // Proto methods
    private static Method getHivemindsMethod;
    private static Method getBiomassMethod;
    private static Method getHostsMethod;
    private static Method setBiomassMethod;  // For pollution feedback

    // Proto fields
    private static Field nodeField;
    private static Method getEntityDataMethod;

    // ==================== Runtime Data Cache ====================

    /** Cached evolution data per level (thread-safe) */
    private static final ConcurrentHashMap<String, EvolutionData> evolutionCache = new ConcurrentHashMap<>();

    /** Last pollution feedback tick */
    private static final ConcurrentHashMap<String, Long> lastFeedbackTick = new ConcurrentHashMap<>();

    /** Pollution feedback interval (5 seconds = 100 ticks) */
    private static final long FEEDBACK_INTERVAL = 100L;

    // ==================== Configuration ====================

    /** Pollution to biomass conversion rate (configurable) - REDUCED from 0.05 to prevent Biomass explosion */
    public static double POLLUTION_TO_BIOMASS_RATE = TriAxisConfig.sporePollutionToBiomassRate;

    /** Maximum Biomass per Proto (prevents infinite growth) */
    public static int MAX_BIOMASS_PER_PROTO = TriAxisConfig.sporeMaxBiomassPerProto;

    /** Pollution feedback trigger threshold */
    public static double POLLUTION_FEEDBACK_THRESHOLD = TriAxisConfig.sporePollutionFeedbackThreshold;

    /** Pollution to evolution factor (configurable) - INCREASED from 0.15 weight */
    public static double POLLUTION_EVOLUTION_FACTOR = 1.0;  // Removed 0.3 multiplier

    /** Voltage to evolution factor (configurable) - INCREASED from 0.10 weight */
    public static double VOLTAGE_EVOLUTION_FACTOR = 1.0;  // Removed 0.2 multiplier

    /** Maximum evolution phase (0-10) */
    public static int MAX_EVOLUTION_PHASE = 10;

    /** Maximum health multiplier for Spore mobs */
    public static double MAX_HEALTH_MULTIPLIER = TriAxisConfig.sporeMaxHealthMultiplier;

    /** Maximum damage multiplier for Spore mobs */
    public static double MAX_DAMAGE_MULTIPLIER = TriAxisConfig.sporeMaxDamageMultiplier;

    // ==================== Initialization ====================

    /**
     * Thread-safe initialization of Spore reflection API.
     * Only executes once, subsequent calls return cached result.
     *
     * @return true if Spore is loaded and initialized successfully
     */
    public static boolean isSporeLoaded() {
        if (!checkDone) {
            synchronized (SporeIntegration.class) {
                if (!checkDone) {  // Double-check locking
                    initializeReflection();
                    checkDone = true;
                }
            }
        }
        return isSporeLoaded;
    }

    /**
     * Initialize reflection (called once, synchronized)
     */
    private static void initializeReflection() {
        try {
            // Load Spore classes
            sporeSavedDataClass = Class.forName("com.Harbinger.Spore.ExtremelySusThings.SporeSavedData");
            protoClass = Class.forName("com.Harbinger.Spore.Sentities.Organoids.Proto");
            organoidClass = Class.forName("com.Harbinger.Spore.Sentities.BaseEntities.Organoid");

            // Load methods
            getHivemindsMethod = sporeSavedDataClass.getMethod("getHiveminds");
            getBiomassMethod = protoClass.getMethod("getBiomass");
            getHostsMethod = protoClass.getMethod("getHosts");

            // Try to load setBiomass for pollution feedback
            try {
                setBiomassMethod = protoClass.getMethod("setBiomass", int.class);
            } catch (NoSuchMethodException e) {
                IndustrialLogger.warn("[Spore] setBiomass method not found, pollution feedback disabled");
            }

            // Load NODE field for position tracking
            try {
                nodeField = protoClass.getDeclaredField("NODE");
                nodeField.setAccessible(true);
                getEntityDataMethod = Entity.class.getMethod("getEntityData");
            } catch (NoSuchFieldException | NoSuchMethodException e) {
                IndustrialLogger.warn("[Spore] NODE field not accessible: " + e.getMessage());
            }

            isSporeLoaded = true;
            IndustrialLogger.info("[Spore] Integration initialized successfully");
            IndustrialLogger.info("[Spore] Pollution feedback: " + (setBiomassMethod != null ? "ENABLED" : "DISABLED"));

        } catch (ClassNotFoundException e) {
            isSporeLoaded = false;
            IndustrialLogger.info("[Spore] Mod not found, integration disabled");
        } catch (NoSuchMethodException e) {
            isSporeLoaded = false;
            IndustrialLogger.error("[Spore] API method not found: " + e.getMessage());
        } catch (Exception e) {
            isSporeLoaded = false;
            IndustrialLogger.error("[Spore] Unexpected initialization error: " + e.getMessage());
        }
    }

    // ==================== Mob Detection ====================

    /**
     * Check if an entity is a Spore mob.
     * Thread-safe, can be called from any thread.
     *
     * @param entity the entity to check
     * @return true if it's a Spore mob
     */
    public static boolean isSporeMob(LivingEntity entity) {
        if (!isSporeLoaded()) return false;

        try {
            String typeName = entity.getType().getDescriptionId().toLowerCase();
            boolean isSpore = typeName.contains("spore") ||
                            entity.getType().getCategory().getName().toLowerCase().contains("spore");

            if (isSpore && TriAxisConfig.enableSporeDebug) {
                IndustrialLogger.debugSpore("Detected Spore mob: " + entity.getType().getDescriptionId());
            }

            return isSpore;
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================== Data Collection (Thread-Safe) ====================

    /**
     * Get all active Hiveminds (Proto entities).
     * Thread-safe, can be called from async threads.
     *
     * @return List of Proto objects, or null if unavailable
     */
    public static List<Object> getHiveminds() {
        if (!isSporeLoaded()) return null;

        try {
            @SuppressWarnings("unchecked")
            List<Object> hiveminds = (List<Object>) getHivemindsMethod.invoke(null);

            if (TriAxisConfig.enableSporeDebug && hiveminds != null) {
                IndustrialLogger.debugSpore(String.format("[Spore] Retrieved %d Hiveminds", hiveminds.size()));
            }

            return hiveminds;
        } catch (Exception e) {
            IndustrialLogger.error("[Spore] Error getting Hiveminds: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get active Hivemind count.
     * Thread-safe.
     *
     * @param level the server level
     * @return number of active Hiveminds
     */
    public static int getActiveHiveminds(ServerLevel level) {
        List<Object> hiveminds = getHiveminds();
        return hiveminds != null ? hiveminds.size() : 0;
    }

    /**
     * Get active Hivemind count (alias for getActiveHiveminds).
     * Thread-safe.
     *
     * @param level the server level
     * @return number of active Hiveminds
     */
    public static int getHivemindCount(ServerLevel level) {
        return getActiveHiveminds(level);
    }

    /**
     * Get total biomass across all Hiveminds.
     * Thread-safe.
     *
     * @param level the server level
     * @return total biomass value
     */
    public static int getTotalBiomass(ServerLevel level) {
        List<Object> hiveminds = getHiveminds();
        if (hiveminds == null || hiveminds.isEmpty()) return 0;

        int totalBiomass = 0;
        for (Object proto : hiveminds) {
            try {
                int biomass = (int) getBiomassMethod.invoke(proto);
                totalBiomass += biomass;
            } catch (Exception e) {
                // Skip failed Proto
            }
        }

        if (TriAxisConfig.enableSporeDebug) {
            IndustrialLogger.debugSpore(String.format("[Spore] Total Biomass: %d", totalBiomass));
        }

        return totalBiomass;
    }

    /**
     * Get total host count across all Hiveminds.
     * Thread-safe.
     *
     * @param level the server level
     * @return total host count
     */
    public static int getTotalHosts(ServerLevel level) {
        List<Object> hiveminds = getHiveminds();
        if (hiveminds == null || hiveminds.isEmpty()) return 0;

        int totalHosts = 0;
        for (Object proto : hiveminds) {
            try {
                int hosts = (int) getHostsMethod.invoke(proto);
                totalHosts += hosts;
            } catch (Exception e) {
                // Skip failed Proto
            }
        }

        if (TriAxisConfig.enableSporeDebug && level.getGameTime() % 200 == 0) {
            IndustrialLogger.debugSpore(String.format("[Spore] Total Hosts: %d", totalHosts));
        }

        return totalHosts;
    }

    /**
     * Get node position of a Proto entity.
     * Thread-safe.
     *
     * @param proto the Proto entity
     * @return BlockPos of the node, or BlockPos.ZERO if unavailable
     */
    public static BlockPos getNodePosition(Object proto) {
        if (!isSporeLoaded() || proto == null || nodeField == null || getEntityDataMethod == null) {
            return BlockPos.ZERO;
        }

        try {
            Object nodeAccessor = nodeField.get(null);
            Object entityData = getEntityDataMethod.invoke(proto);
            Method getMethod = entityData.getClass().getMethod("get", net.minecraft.network.syncher.EntityDataAccessor.class);
            BlockPos nodePos = (BlockPos) getMethod.invoke(entityData, nodeAccessor);

            return nodePos != null ? nodePos : BlockPos.ZERO;
        } catch (Exception e) {
            if (TriAxisConfig.enableSporeDebug) {
                IndustrialLogger.debugSpore("[Spore] Error getting node position: " + e.getMessage());
            }
            return BlockPos.ZERO;
        }
    }

    /**
     * Get biomass of a specific Proto entity.
     * Thread-safe.
     *
     * @param proto the Proto entity
     * @return biomass value, or 0 if unavailable
     */
    public static int getBiomass(Object proto) {
        if (!isSporeLoaded() || proto == null) return 0;

        try {
            return (int) getBiomassMethod.invoke(proto);
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== Evolution System ====================

    /**
     * Calculate evolution phase (0-10) based on multiple factors.
     * Similar to Factorio's evolution factor.
     *
     * Formula:
     * - Hivemind factor: count / 10 (max 1.0)
     * - Biomass factor: total / 10000 (max 1.0)
     * - Pollution factor: permanent / 1000 * weight (max 1.0)
     * - Voltage factor: avgTier / 9 * weight (max 1.0)
     *
     * Evolution = (hivemind + biomass + pollution + voltage) / 4 * 10
     *
     * @param level the server level
     * @return evolution phase (0-10)
     */
    public static int calculateEvolutionPhase(ServerLevel level) {
        if (!isSporeLoaded()) return 0;

        // Get cached or calculate new
        String levelKey = level.dimension().location().toString();
        EvolutionData cached = evolutionCache.get(levelKey);

        // Update cache every 10 seconds
        if (cached == null || level.getGameTime() - cached.timestamp > 200) {
            cached = calculateEvolutionData(level);
            evolutionCache.put(levelKey, cached);
        }

        return cached.phase;
    }

    /**
     * Calculate detailed evolution data.
     *
     * @param level the server level
     * @return evolution data
     */
    private static EvolutionData calculateEvolutionData(ServerLevel level) {
        int hiveminds = getActiveHiveminds(level);
        int biomass = getTotalBiomass(level);
        int hosts = getTotalHosts(level);
        double permanentPollution = PollutionManager.getPermanentPollution();

        // Calculate average voltage tier (from player data)
        double avgVoltageTier = DifficultyManager.getAveragePlayerVoltageTier(level);

        // Normalize factors (0.0 - 1.0) - REBALANCED: Higher saturation thresholds
        double hivemindFactor = Math.min(1.0, hiveminds / (double) TriAxisConfig.sporeHivemindSaturation);
        double biomassFactor = Math.min(1.0, biomass / (double) TriAxisConfig.sporeBiomassSaturation);
        double hostFactor = Math.min(1.0, hosts / (double) TriAxisConfig.sporeHostSaturation);
        double pollutionFactor = Math.min(1.0, permanentPollution / TriAxisConfig.sporePollutionSaturation * POLLUTION_EVOLUTION_FACTOR);
        double voltageFactor = Math.min(1.0, avgVoltageTier / TriAxisConfig.sporeMaxVoltageTier * VOLTAGE_EVOLUTION_FACTOR);

        // Weighted average - REBALANCED: Pollution and Voltage more important
        // Old: H:35% B:25% Ho:15% P:15% V:10%
        // New: H:20% B:20% Ho:15% P:25% V:20%
        double evolutionValue = (hivemindFactor * TriAxisConfig.sporeEvolutionWeightHivemind +
                                biomassFactor * TriAxisConfig.sporeEvolutionWeightBiomass +
                                hostFactor * TriAxisConfig.sporeEvolutionWeightHost +
                                pollutionFactor * TriAxisConfig.sporeEvolutionWeightPollution +
                                voltageFactor * TriAxisConfig.sporeEvolutionWeightVoltage);

        int phase = (int) Math.min(MAX_EVOLUTION_PHASE, evolutionValue * MAX_EVOLUTION_PHASE);

        if (TriAxisConfig.enableSporeDebug && level.getGameTime() % 200 == 0) {
            IndustrialLogger.debugSpore(String.format(
                "[Spore] Evolution Phase: %d/10 (H:%.2f B:%.2f Ho:%.2f P:%.2f V:%.2f)",
                phase, hivemindFactor, biomassFactor, hostFactor, pollutionFactor, voltageFactor
            ));
        }

        return new EvolutionData(phase, evolutionValue, level.getGameTime());
    }

    /**
     * Get infection intensity (0-100) for compatibility.
     * Maps evolution phase to percentage.
     *
     * @param level the server level
     * @return infection intensity (0-100)
     */
    public static float calculateInfectionIntensity(ServerLevel level) {
        int phase = calculateEvolutionPhase(level);
        return (float) phase * (float) TriAxisConfig.sporeInfectionIntensityMultiplier;  // Phase 0-10 -> 0-100% (configurable)
    }

    // ==================== Pollution Feedback System ====================

    /**
     * Apply pollution feedback to Spore system.
     * High pollution accelerates Hivemind growth (increases biomass).
     *
     * MUST be called from main thread (modifies entity data).
     * Throttled to once per 5 seconds per level.
     *
     * @param level the server level
     * @param pollution current pollution level
     */
    public static void applyPollutionFeedback(ServerLevel level, double pollution) {
        if (!isSporeLoaded() || setBiomassMethod == null) return;
        if (pollution < POLLUTION_FEEDBACK_THRESHOLD) return;  // Raised threshold from 100 to 150

        String levelKey = level.dimension().location().toString();
        long currentTick = level.getGameTime();
        Long lastTick = lastFeedbackTick.get(levelKey);

        // Throttle: only apply every 5 seconds
        if (lastTick != null && currentTick - lastTick < FEEDBACK_INTERVAL) {
            return;
        }

        lastFeedbackTick.put(levelKey, currentTick);

        // Get Hiveminds and apply biomass boost
        List<Object> hiveminds = getHiveminds();
        if (hiveminds == null || hiveminds.isEmpty()) return;

        // Calculate biomass increase based on pollution
        int biomassIncrease = (int) (pollution * POLLUTION_TO_BIOMASS_RATE);
        if (biomassIncrease <= 0) return;

        int updated = 0;
        int capped = 0;
        for (Object proto : hiveminds) {
            try {
                int currentBiomass = (int) getBiomassMethod.invoke(proto);

                // Check if already at cap
                if (currentBiomass >= MAX_BIOMASS_PER_PROTO) {
                    capped++;
                    continue;
                }

                // Apply diminishing returns: growth slows as biomass approaches cap
                double diminishingFactor = 1.0 - (currentBiomass / (double) MAX_BIOMASS_PER_PROTO);
                int effectiveIncrease = (int) (biomassIncrease * diminishingFactor);

                // Apply cap
                int newBiomass = Math.min(MAX_BIOMASS_PER_PROTO, currentBiomass + effectiveIncrease);
                setBiomassMethod.invoke(proto, newBiomass);
                updated++;
            } catch (Exception e) {
                // Skip failed Proto
            }
        }

        if (TriAxisConfig.enableSporeDebug && updated > 0) {
            IndustrialLogger.debugSpore(String.format(
                "[Spore] Pollution feedback: +%d biomass to %d Hiveminds (%d at cap) (pollution: %.1f)",
                biomassIncrease, updated, capped, pollution
            ));
        }
    }

    // ==================== Mob Buffing (Main Thread Only) ====================

    /**
     * Buff Spore mobs based on pollution, voltage, and evolution.
     * MUST be called from main thread.
     *
     * @param mob the mob to buff
     * @param pollutionLevel local pollution level
     * @param localVoltageTier local voltage tier
     * @param level the server level
     */
    public static void buffSporeMob(Mob mob, double pollutionLevel, double localVoltageTier, ServerLevel level) {
        if (!isSporeLoaded() || !isSporeMob(mob)) return;

        try {
            // Calculate buff multipliers
            double pollutionBonus = Math.min(1.0, pollutionLevel / TriAxisConfig.sporePollutionBonusDivisor);  // Max 100% at configured pollution
            double voltageBonus = Math.max(0, localVoltageTier - 1) * TriAxisConfig.sporeVoltageBonusPerTier;  // Configurable % per tier above ULV

            // Evolution bonus
            int evolutionPhase = calculateEvolutionPhase(level);
            double evolutionBonus = evolutionPhase * TriAxisConfig.sporeEvolutionBonusPerPhase;  // Configurable % per phase

            // Total multiplier with HARD CAP to prevent stacking with ImprovedMobs
            double totalMultiplier = 1.0 + pollutionBonus + voltageBonus + evolutionBonus;
            totalMultiplier = Math.min(MAX_HEALTH_MULTIPLIER, totalMultiplier);  // Cap at 3.0x

            // Apply health buff
            AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                double oldHealth = health.getBaseValue();
                double newHealth = oldHealth * totalMultiplier;
                health.setBaseValue(newHealth);
                mob.setHealth(mob.getMaxHealth());

                if (TriAxisConfig.enableSporeDebug) {
                    IndustrialLogger.debugSpore(String.format(
                        "[Spore] Buffed %s: HP %.1f -> %.1f (×%.2f, capped at %.1fx) [P:%.0f%% V:%.0f%% E:%.0f%%]",
                        mob.getType().getDescriptionId(), oldHealth, newHealth, totalMultiplier,
                        MAX_HEALTH_MULTIPLIER, pollutionBonus * 100, voltageBonus * 100, evolutionBonus * 100
                    ));
                }
            }

            // Apply damage buff at high pollution with REDUCED scaling and HARD CAP
            if (pollutionLevel > TriAxisConfig.sporeDamageBonusThreshold) {
                AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damage != null) {
                    double oldDamage = damage.getBaseValue();
                    // Configurable damage bonus calculation
                    double damageBonus = (pollutionLevel / TriAxisConfig.sporeDamageBonusDivisor) * TriAxisConfig.sporeDamageBonusMultiplier;
                    double damageMultiplier = Math.min(MAX_DAMAGE_MULTIPLIER, 1.0 + damageBonus);  // Cap at configured max
                    damage.setBaseValue(oldDamage * damageMultiplier);
                }
            }

        } catch (Throwable t) {
            IndustrialLogger.error("[Spore] Error buffing mob: " + t.getMessage());
        }
    }

    /**
     * Get nearest Hivemind distance for threat calculations.
     * Thread-safe.
     *
     * @param level the server level
     * @param playerPos player position
     * @return distance to nearest Hivemind, or Double.MAX_VALUE if none
     */
    public static double getNearestHivemindDistance(ServerLevel level, BlockPos playerPos) {
        List<Object> hiveminds = getHiveminds();
        if (hiveminds == null || hiveminds.isEmpty()) return Double.MAX_VALUE;

        double minDistance = Double.MAX_VALUE;
        for (Object proto : hiveminds) {
            if (proto instanceof Entity entity) {
                double distance = Math.sqrt(entity.blockPosition().distSqr(playerPos));
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }

        return minDistance;
    }

    // ==================== Legacy Compatibility ====================

    /**
     * @deprecated Use calculateEvolutionPhase() instead
     */
    @Deprecated
    public static int getEvolutionPhase(ServerLevel level) {
        return calculateEvolutionPhase(level);
    }

    /**
     * @deprecated Use calculateInfectionIntensity() instead
     */
    @Deprecated
    public static float getInfectionLevel(ServerLevel level) {
        return calculateInfectionIntensity(level);
    }

    /**
     * @deprecated Estimated based on Hiveminds
     */
    @Deprecated
    public static int getInfectedChunks(ServerLevel level) {
        int hiveminds = getActiveHiveminds(level);
        return hiveminds * 20;  // Estimate: 20 chunks per Hivemind
    }

    // ==================== Data Classes ====================

    /**
     * Cached evolution data
     */
    private static class EvolutionData {
        final int phase;
        final double value;
        final long timestamp;

        EvolutionData(int phase, double value, long timestamp) {
            this.phase = phase;
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
