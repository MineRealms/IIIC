package cn.minerealms.iic.industrial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration manager for the Tri-Axis Difficulty System.
 * <p>
 * This class manages all configuration parameters for the industrial integration system,
 * including difficulty weights, pollution thresholds, threat triggers, and mob attribute multipliers.
 * <p>
 * The configuration supports multiple difficulty presets:
 * <ul>
 *   <li>NORMAL - Balanced for casual players, recommended for beginners</li>
 *   <li>HARD - Higher challenge for experienced players</li>
 *   <li>HARDCORE - High difficulty for hardcore players</li>
 *   <li>INSANE - Extreme challenge for masochists</li>
 *   <li>CUSTOM - Use custom values from config file</li>
 * </ul>
 * <p>
 * Configuration file location: config/triaxis-difficulty.properties
 * <p>
 * All parameters can be customized via the config file. The file is automatically
 * generated with detailed comments on first run.
 *
 * @see TriAxisDifficultyManager
 * @see IndustrialDifficultyManager
 * @see PollutionManager
 * @see ThreatManager
 * @author ImprovedMobs Team
 */
public class TriAxisConfig {
    // ========== Tri-Axis Weights ==========

    /**
     * Weight for time factor in tri-axis difficulty calculation (0.0-1.0).
     * Higher values make game time more influential on difficulty.
     */
    public static double weightTime = 0.35;

    /**
     * Weight for voltage/technology factor in tri-axis difficulty calculation (0.0-1.0).
     * Higher values make machine voltage tiers more influential on difficulty.
     */
    public static double weightVoltage = 0.35;

    /**
     * Weight for pollution factor in tri-axis difficulty calculation (0.0-1.0).
     * Higher values make pollution more influential on difficulty.
     * Note: The three weights should sum to approximately 1.0.
     */
    public static double weightPollution = 0.30;

    // ========== Global Difficulty Multipliers ==========

    /**
     * Global difficulty multiplier applied to final difficulty value (1.0-5.0 recommended).
     * Higher values make mobs stronger overall.
     */
    public static double globalMultiplier = 2.8;

    /**
     * Exponential moving average alpha for smoothing (0.01-0.2).
     * Lower values = smoother transitions, higher values = faster response.
     */
    public static double emaAlpha = 0.05;

    /**
     * Pollution saturation threshold (500-1500 recommended).
     * Higher values require more pollution to reach high difficulty.
     * Preset values: NORMAL=1000, HARD=900, HARDCORE=800, INSANE=600
     */
    public static double pollutionDenominator = 800.0;

    // ========== Multiplicative Difficulty Model Parameters ==========

    /**
     * Minimum base difficulty from time factor (0.3-0.7 recommended).
     * Prevents difficulty from being too low in early game.
     */
    public static double baseMin = 0.5;

    /**
     * Maximum base difficulty from time factor (1.5-3.0 recommended).
     * Caps the time-based difficulty growth.
     */
    public static double baseMax = 2.0;

    /**
     * Exponent for voltage tier scaling (1.0-1.5 recommended).
     * Higher values create steeper difficulty curve with voltage tiers.
     */
    public static double scaleExponent = 1.2;

    /**
     * Multiplier for voltage tier scaling (0.1-0.3 recommended).
     * Controls the strength of voltage tier impact on difficulty.
     */
    public static double scaleMultiplier = 0.15;

    /**
     * Minimum pressure multiplier from pollution (1.0 recommended).
     * Base multiplier when pollution is zero.
     */
    public static double pressureMin = 1.0;

    /**
     * Maximum pressure multiplier from pollution (3.0-5.0 recommended).
     * Maximum multiplier at extreme pollution levels.
     */
    public static double pressureMax = 4.0;

    /**
     * Sigmoid shift for pollution pressure curve (1.5-3.0 recommended).
     * Controls the inflection point of the pollution curve.
     * Higher values delay the pressure increase.
     */
    public static double sigmoidShift = 2.0;

    // ========== Industrial Bonus Configuration ==========

    /**
     * Technology weight for industrial bonus calculation (10.0-50.0).
     * Controls how much voltage tier affects mob strength.
     */
    public static double techWeight = 30.0;

    /**
     * Pollution weight for industrial bonus calculation (5.0-30.0).
     * Controls how much pollution affects mob strength.
     */
    public static double pollutionWeight = 18.0;

    /**
     * Maximum industrial voltage tier for normalization (0=ULV, 9=UHV, 14=MAX).
     * Should be set to the highest voltage tier in your modpack.
     * Recommended: 9 (UHV) for most modpacks.
     */
    public static int maxIndustrialTier = 9;

    // ========== Difficulty Contribution Weights ==========

    /**
     * Weight for player industrial bonus in difficulty calculation.
     */
    public static double playerBonusWeight = 1.0;

    /**
     * Weight for local temporary pollution in difficulty calculation.
     */
    public static double localPollutionWeight = 0.5;

    /**
     * Weight for global permanent pollution in difficulty calculation.
     */
    public static double globalPollutionWeight = 0.3;

    /**
     * Weight for time factor in difficulty calculation.
     */
    public static double timeFactorWeight = 0.2;

    // ========== Threat System Configuration ==========

    /**
     * Minimum voltage tier for zombies to attack machines (passive threat).
     * Default: 2 (MV tier)
     * Set to -1 to disable this threat type.
     */
    public static int zombieAttackMinTier = 2;

    /**
     * Minimum voltage tier for active zombie spawning.
     * Default: 3 (HV tier)
     * Set to -1 to disable this threat type.
     */
    public static int zombieSpawnMinTier = 3;

    /**
     * Minimum voltage tier for active creeper spawning.
     * Default: 3 (HV tier)
     * Set to -1 to disable this threat type.
     */
    public static int creeperSpawnMinTier = 3;

    /**
     * MV stage: Pollution threshold for zombies to attack machines.
     * When chunk pollution reaches this value, nearby zombies will target machines.
     */
    public static double mvZombieAttackThreshold = 40.0;

    /**
     * HV stage: Pollution threshold for active zombie spawning.
     * When chunk pollution reaches this value, zombies will spawn near machines.
     */
    public static double hvZombieSpawnThreshold = 60.0;

    /**
     * HV stage: Pollution threshold for active creeper spawning.
     * When chunk pollution reaches this value, creepers will spawn near machines.
     */
    public static double hvCreeperSpawnThreshold = 100.0;

    /**
     * Pollution threshold for charged creeper spawning.
     * When chunk pollution reaches this value, charged creepers may spawn.
     */
    public static double chargedCreeperThreshold = 180.0;

    /**
     * Zombie spawn chance per second (0.0-1.0).
     * Default: 0.015 (1.5% chance per second)
     */
    public static double zombieSpawnChance = 0.015;

    /**
     * Creeper spawn chance per second (0.0-1.0).
     * Default: 0.008 (0.8% chance per second)
     */
    public static double creeperSpawnChance = 0.008;

    /**
     * Charged creeper spawn chance per second (0.0-1.0).
     * Default: 0.0015 (0.15% chance per second)
     */
    public static double chargedCreeperChance = 0.0015;

    // ========== Wave System Configuration ==========

    /**
     * Base wave size for zombie waves.
     * Default: 3
     */
    public static int waveBaseSize = 3;

    /**
     * Base wave size for creeper waves (smaller than zombie waves).
     * Default: 2
     */
    public static int waveCreeperBaseSize = 2;

    /**
     * Pollution divisor for wave size calculation.
     * Wave size increases by 1 per this amount of pollution.
     * Default: 50 (1 extra mob per 50 pollution)
     */
    public static int wavePollutionDivisor = 50;

    /**
     * Voltage tier divisor for wave size calculation.
     * Wave size increases by 1 per this many tiers.
     * Default: 2 (1 extra mob per 2 tiers)
     */
    public static int waveTierDivisor = 2;

    /**
     * Maximum wave size for zombie waves (performance limit).
     * Default: 15
     */
    public static int waveMaxSize = 15;

    /**
     * Maximum wave size for creeper waves (performance limit).
     * Default: 10
     */
    public static int waveCreeperMaxSize = 10;

    /**
     * Chance for a mob in a wave to be a creeper instead of zombie.
     * Default: 0.3 (30% creepers, 70% zombies)
     */
    public static double waveCreeperChance = 0.3;

    // ========== Pollution System Configuration ==========

    /**
     * Base pollution generation rate per second for machines.
     * This is the pollution generated by a ULV (tier 0) machine.
     * Default: 0.01 pollution/second
     */
    public static double basePollutionRate = 0.01;

    /**
     * Pollution growth factor per voltage tier.
     * Formula: pollution = basePollutionRate * (1 + tier * tierGrowthFactor)
     * Default: 0.5 (50% increase per tier)
     * Example: MV (tier 2) = 0.01 * (1 + 2 * 0.5) = 0.02 pollution/second
     */
    public static double tierGrowthFactor = 0.5;

    /**
     * Natural pollution decay rate (proportional, per second).
     * Default: 0.002 (0.2% per second)
     * This is now proportional to current pollution, not a fixed value.
     */
    public static double naturalDecayRate = 0.002;

    /**
     * Threshold for converting temporary pollution to permanent pollution.
     * Only pollution above this threshold converts to permanent.
     * Default: 200.0
     */
    public static double tempToPermanentThreshold = 200.0;

    /**
     * Conversion rate from temporary to permanent pollution per second (0.0-1.0).
     * Default: 0.005 (0.5% per second, increased from 0.001)
     * Only applies to pollution above threshold.
     */
    public static double tempToPermanentRate = 0.005;

    /**
     * Conversion rate from permanent pollution to difficulty.
     * Higher values make permanent pollution more impactful on difficulty.
     */
    public static double permanentToDifficultyRate = 0.1;

    // ========== Turret System Configuration (New) ==========

    /**
     * Flamethrower turret direct hit damage.
     * Default: 4.0 (降低自 5.0)
     */
    public static double flamethrowerDirectDamage = 4.0;

    /**
     * Flamethrower turret ground fire damage per tick.
     * Default: 1.5 (降低自 2.0)
     */
    public static double flamethrowerGroundDamage = 1.5;

    // ========== Scanning & Thresholds ==========

    /**
     * Radius in blocks for machine scanning.
     * Larger values increase scan range but may impact performance.
     * Recommended: 64 blocks
     */
    public static int scanRadiusBlocks = 64;

    /**
     * Maximum GT voltage tier supported by the mod (for compatibility).
     * Usually 14 for full GregTech CEu support.
     */
    public static int maxGTTier = 14;

    /**
     * Maximum difficulty change per second to prevent sudden spikes.
     * Higher values allow faster difficulty changes.
     */
    public static double maxChangePerSec = 0.015;

    // ========== Time Scaling Parameters ==========

    /**
     * Target number of Minecraft days to reach peak time difficulty.
     * Longer values make time progression slower.
     * Preset values: NORMAL=800, HARD=1000, HARDCORE=1200, INSANE=1500
     */
    public static double targetDays = 1200.0;

    /**
     * Base days for logarithmic scaling (10-50).
     * Lower values make early game difficulty ramp up faster.
     * Recommended: 30
     */
    public static double baseDays = 30.0;

    // ========== Attribute Multipliers ==========

    /**
     * Health multiplier factor (0.5-5.0).
     * Multiplied with ImprovedMobs base config to determine final mob health.
     */
    public static double hpMultFactor = 2.2;

    /**
     * Attack damage multiplier factor (0.5-3.0).
     * Multiplied with ImprovedMobs base config to determine final mob damage.
     */
    public static double attackMultFactor = 1.6;

    /**
     * Speed multiplier factor (0.3-2.0).
     * Multiplied with ImprovedMobs base config to determine final mob speed.
     */
    public static double speedMultFactor = 0.8;

    /**
     * Armor multiplier factor (0.5-2.0).
     * Multiplied with ImprovedMobs base config to determine final mob armor.
     */
    public static double armorMultFactor = 1.2;

    // ========== Attribute Caps (New) ==========

    /**
     * Maximum HP multiplier cap (5.0-20.0).
     * Prevents mob health from growing infinitely.
     * Default: 15.0 (僵尸最高 300 HP)
     */
    public static double maxHpMultiplier = 15.0;

    /**
     * Maximum attack damage multiplier cap (3.0-15.0).
     * Prevents mob damage from growing infinitely.
     * Default: 10.0 (僵尸最高 50 伤害)
     */
    public static double maxAttackMultiplier = 10.0;

    /**
     * Maximum speed multiplier cap (1.5-5.0).
     * Prevents mob speed from growing infinitely.
     * Default: 3.0
     */
    public static double maxSpeedMultiplier = 3.0;

    /**
     * Maximum armor multiplier cap (2.0-10.0).
     * Prevents mob armor from growing infinitely.
     * Default: 5.0
     */
    public static double maxArmorMultiplier = 5.0;

    // ========== Voltage Tier HP Targets ==========

    /**
     * HP multiplier target for ULV tier (Tier 0).
     * Base zombie health is 20, so 1.0 = 20 HP.
     */
    public static double ulvHpTarget = 1.0;

    /**
     * HP multiplier target for LV tier (Tier 1).
     * Default: 1.3 = 26 HP
     */
    public static double lvHpTarget = 1.3;

    /**
     * HP multiplier target for MV tier (Tier 2).
     * Default: 1.8 = 36 HP
     */
    public static double mvHpTarget = 1.8;

    /**
     * HP multiplier target for HV tier (Tier 3).
     * Default: 2.5 = 50 HP
     */
    public static double hvHpTarget = 2.5;

    /**
     * HP multiplier target for EV tier (Tier 4).
     * Default: 3.5 = 70 HP
     */
    public static double evHpTarget = 3.5;

    /**
     * HP multiplier target for IV tier (Tier 5).
     * Default: 4.8 = 96 HP
     */
    public static double ivHpTarget = 4.8;

    /**
     * HP multiplier target for LuV tier (Tier 6).
     * Default: 6.5 = 130 HP
     */
    public static double luvHpTarget = 6.5;

    /**
     * HP multiplier target for ZPM tier (Tier 7).
     * Default: 8.5 = 170 HP
     */
    public static double zpmHpTarget = 8.5;

    /**
     * HP multiplier target for UV tier (Tier 8).
     * Default: 11.0 = 220 HP
     */
    public static double uvHpTarget = 11.0;

    /**
     * HP multiplier target for UHV tier (Tier 9).
     * Default: 14.0 = 280 HP
     */
    public static double uhvHpTarget = 14.0;

    // ========== Difficulty Curve Presets ==========

    /**
     * Difficulty preset selection.
     * Valid values: NORMAL, HARD, HARDCORE, INSANE, CUSTOM
     * <p>
     * CUSTOM uses values from config file without applying preset.
     */
    public static String difficultyPreset = "NORMAL";

    // ========== GT Pollution Integration ==========

    /**
     * Weight for GT pollution contribution to temporary pollution (0.0-2.0).
     * Higher values make GT pollution more impactful.
     * Default: 0.8
     */
    public static double gtPollutionWeight = 0.8;

    /**
     * Base multiplier per 10 sources above threshold (0.1-1.0).
     * Controls how quickly multiplier increases with source count.
     * Default: 0.5
     */
    public static double gtSourceMultiplierBase = 0.5;

    /**
     * Threshold for GT pollution source multiplier activation.
     * When source count exceeds this, multipliers start applying.
     * Default: 10 sources
     */
    public static int gtSourceThreshold = 10;

    /**
     * Air Scrubber cleaning efficiency multiplier (0.5-2.0).
     * Higher values make Air Scrubbers more effective at reducing pollution.
     * Default: 1.0
     */
    public static double airScrubberEfficiency = 1.0;

    // ========== Hivemind Proximity System ==========

    /**
     * Proximity radius in chunks for Hivemind acceleration (4-16).
     * Pollution within this distance from Hiveminds accelerates difficulty.
     * Default: 8 chunks (128 blocks)
     */
    public static double hivemindProximityRadius = 8.0;

    /**
     * Maximum acceleration factor for Hivemind proximity (1.0-5.0).
     * Controls how much Hiveminds accelerate difficulty growth.
     * Default: 2.0 (up to 2x acceleration)
     */
    public static double hivemindAccelerationFactor = 2.0;

    /**
     * Enable Hivemind proximity acceleration system.
     * When enabled, pollution near Hiveminds accelerates difficulty growth.
     * Default: true
     */
    public static boolean enableHivemindAcceleration = true;

    // ========== Hordes Integration ==========

    /**
     * Enable The Hordes integration system.
     * When enabled, hordes are dynamically adjusted based on difficulty and pollution.
     * Default: true
     */
    public static boolean enableHordeIntegration = true;

    /**
     * Global horde intensity multiplier (0.5-3.0).
     * Higher values make hordes stronger overall.
     * Default: 1.0
     */
    public static double hordeIntensityMultiplier = 1.0;

    /**
     * Difficulty to horde intensity conversion factor (0.1-2.0).
     * Controls how much ImprovedMobs difficulty affects horde strength.
     * Default: 0.5
     */
    public static double difficultyToIntensityFactor = 0.5;

    /**
     * Enable pollution-triggered hordes.
     * When enabled, high pollution automatically triggers hordes.
     * Default: true
     */
    public static boolean enablePollutionTriggeredHordes = true;

    /**
     * Pollution threshold for triggering hordes (100-300).
     * When chunk pollution exceeds this, hordes may be triggered.
     * Default: 150.0
     */
    public static double pollutionHordeTriggerThreshold = 150.0;

    /**
     * Pollution horde check interval in ticks (300-1200).
     * How often to check for pollution-triggered hordes.
     * Default: 600 (30 seconds)
     */
    public static double pollutionHordeCheckInterval = 600.0;

    /**
     * Pollution horde trigger chance per check (0.01-0.2).
     * Probability of triggering a horde when pollution is high enough.
     * Default: 0.05 (5% per check)
     */
    public static double pollutionHordeTriggerChance = 0.05;

    /**
     * Enable small skirmishes (mini-hordes).
     * When enabled, small groups of mobs spawn periodically in polluted areas.
     * Default: true
     */
    public static boolean enableSkirmishes = true;

    /**
     * Pollution threshold for skirmishes (50-150).
     * When chunk pollution exceeds this, skirmishes may occur.
     * Default: 80.0
     */
    public static double skirmishPollutionThreshold = 80.0;

    /**
     * Skirmish interval in ticks (600-2400).
     * Time between skirmish checks.
     * Default: 1200 (1 minute)
     */
    public static double skirmishInterval = 1200.0;

    /**
     * Minimum skirmish mob count (1-10).
     * Default: 3
     */
    public static int skirmishMinCount = 3;

    /**
     * Maximum skirmish mob count (5-20).
     * Default: 8
     */
    public static int skirmishMaxCount = 8;

    /**
     * Major horde pollution threshold (150-300).
     * When pollution exceeds this, major hordes (2x strength) are triggered.
     * Default: 200.0
     */
    public static double majorHordePollutionThreshold = 200.0;

    /**
     * Major horde strength multiplier (1.5-3.0).
     * How much stronger major hordes are compared to normal hordes.
     * Default: 2.0
     */
    public static double majorHordeMultiplier = 2.0;

    /**
     * Enable machine targeting for horde zombies.
     * When enabled, some horde zombies will attack nearby machines.
     * Default: true
     */
    public static boolean enableMachineTargeting = true;

    /**
     * Machine targeting range in blocks (16-64).
     * How far zombies can detect machines to attack.
     * Default: 32.0
     */
    public static double machineTargetingRange = 32.0;

    /**
     * Machine targeting chance (0.1-1.0).
     * Probability that a horde zombie will target machines instead of players.
     * Default: 0.3 (30%)
     */
    public static double machineTargetingChance = 0.3;

    /**
     * Enable voltage tier scaling for hordes.
     * When enabled, horde strength scales with player's voltage tier.
     * Default: true
     */
    public static boolean enableVoltageTierScaling = true;

    /**
     * Voltage tier intensity multipliers for hordes (10 tiers: ULV to UHV).
     * Each tier gets a multiplier that affects horde strength.
     * Default: [1.0, 1.1, 1.3, 1.5, 1.8, 2.2, 2.6, 3.0, 3.5, 4.0]
     */
    public static double[] hordeTierIntensityMultipliers = {
        1.0,  // ULV (Tier 0)
        1.1,  // LV  (Tier 1)
        1.3,  // MV  (Tier 2) - 小股袭扰开始
        1.5,  // HV  (Tier 3) - 中等威胁
        1.8,  // EV  (Tier 4)
        2.2,  // IV  (Tier 5) - 大尸潮开始
        2.6,  // LuV (Tier 6)
        3.0,  // ZPM (Tier 7)
        3.5,  // UV  (Tier 8)
        4.0   // UHV (Tier 9) - 极限挑战
    };

    // ========== ImprovedMobs Integration ==========

    /**
     * Enable takeover of ImprovedMobs difficulty system.
     * When true, IIC completely controls difficulty calculation (IntegrationType.ON).
     * When false, IIC adds to ImprovedMobs base difficulty (IntegrationType.ADD).
     * Default: true (recommended)
     */
    public static boolean takeoverImprovedMobsDifficulty = true;

    /**
     * Target maximum difficulty value for ImprovedMobs integration (50-500).
     * This is the difficulty value at maximum progression (UV tier + max time + max pollution).
     * ImprovedMobs expects 0-250 range, where:
     * - 50: Mobs start breaking blocks (if difficultyBreak=50)
     * - 100: Moderate difficulty, noticeable attribute increases
     * - 150: High difficulty, significant attribute increases
     * - 250: Maximum difficulty, extreme attribute increases
     * Default: 250
     */
    public static double targetMaxDifficulty = 250.0;

    /**
     * Real-world days to reach target difficulty at maximum progression (10-90).
     * This controls how fast difficulty grows over time.
     * Example: 30 days = if you reach UV tier in 30 days, difficulty will be at target.
     * Default: 30 days
     */
    public static double realWorldDaysToMax = 30.0;

    // ========== Mob Spawn Enhancement ==========

    /**
     * Enable mob spawn enhancement system.
     * When enabled, spawn rates are modified based on difficulty.
     * Default: false (disabled by default, enable for testing)
     */
    public static boolean enableSpawnEnhancement = false;

    /**
     * Global spawn rate multiplier (0.0-10.0).
     * Base chance for mobs to spawn.
     * - 1.0 = normal spawn rate
     * - 2.0 = double spawn rate
     * - 0.5 = half spawn rate
     * Default: 1.0
     */
    public static double spawnMultiplier = 1.0;

    /**
     * Additional spawn chance per difficulty point (0.0-1.0).
     * Higher difficulty = more spawns.
     * Formula: spawnChance = spawnMultiplier + (difficulty × difficultySpawnBonus)
     * Example: difficulty=100, bonus=0.001 → +0.1 spawn chance
     * Default: 0.001 (0.1% per difficulty point)
     */
    public static double difficultySpawnBonus = 0.001;

    /**
     * Maximum spawn chance cap (0.0-10.0).
     * Prevents spawn chance from exceeding this value.
     * Default: 3.0 (up to 3x normal spawn rate)
     */
    public static double maxSpawnChance = 3.0;

    /**
     * Enable forced spawning based on difficulty.
     * When enabled, additional mobs are force-spawned in high-difficulty areas.
     * Default: false
     */
    public static boolean enableForcedSpawning = false;

    /**
     * Difficulty threshold for forced spawning (50-250).
     * When difficulty exceeds this, forced spawning may occur.
     * Default: 150.0
     */
    public static double forcedSpawnThreshold = 150.0;

    /**
     * Forced spawn chance per second (0.0-1.0).
     * Probability of triggering forced spawn each second.
     * Default: 0.01 (1% per second)
     */
    public static double forcedSpawnChance = 0.01;

    /**
     * Number of mobs to spawn per forced spawn event (1-20).
     * Default: 3
     */
    public static int forcedSpawnCount = 3;

    // ========== Debug Settings ==========

    /**
     * Enable debug line rendering (e.g., zombie attack lines).
     * Default: false (disabled)
     */
    public static boolean enableDebugLines = false;

    /**
     * Enable detailed difficulty logging.
     * Default: false (disabled)
     */
    public static boolean enableDifficultyLogging = false;

    /**
     * Enable Spore integration debug information.
     * Default: false (disabled)
     */
    public static boolean enableSporeDebug = false;

    // ========== Spore Integration Configuration ==========

    /**
     * Pollution to biomass conversion rate for Spore Hiveminds.
     * Reduced from 0.05 to prevent Biomass explosion.
     * Default: 0.01 (1 biomass per 100 pollution every 5 seconds)
     */
    public static double sporePollutionToBiomassRate = 0.01;

    /**
     * Maximum Biomass per Proto Hivemind (prevents infinite growth).
     * Default: 5000
     */
    public static int sporeMaxBiomassPerProto = 5000;

    /**
     * Pollution threshold for triggering Spore pollution feedback.
     * Raised from 100 to delay feedback until mid-game.
     * Default: 150.0
     */
    public static double sporePollutionFeedbackThreshold = 150.0;

    /**
     * Maximum health multiplier for Spore mobs (prevents stacking with ImprovedMobs).
     * Default: 3.0 (max 3x health)
     */
    public static double sporeMaxHealthMultiplier = 3.0;

    /**
     * Maximum damage multiplier for Spore mobs (prevents stacking with ImprovedMobs).
     * Default: 2.5 (max 2.5x damage)
     */
    public static double sporeMaxDamageMultiplier = 2.5;

    /**
     * Hivemind count saturation threshold for evolution calculation.
     * Raised from 10 to slow down evolution progression.
     * Default: 20
     */
    public static int sporeHivemindSaturation = 20;

    /**
     * Total Biomass saturation threshold for evolution calculation.
     * Raised from 10000 to slow down evolution progression.
     * Default: 30000
     */
    public static int sporeBiomassSaturation = 30000;

    /**
     * Host count saturation threshold for evolution calculation.
     * Raised from 500 to slow down evolution progression.
     * Default: 1000
     */
    public static int sporeHostSaturation = 1000;

    /**
     * Permanent pollution saturation threshold for evolution calculation.
     * Default: 1500
     */
    public static double sporePollutionSaturation = 1500.0;

    /**
     * Hivemind weight in evolution calculation.
     * Reduced from 0.35 to make pollution more important.
     * Default: 0.20
     */
    public static double sporeEvolutionWeightHivemind = 0.20;

    /**
     * Biomass weight in evolution calculation.
     * Reduced from 0.25 to balance with other factors.
     * Default: 0.20
     */
    public static double sporeEvolutionWeightBiomass = 0.20;

    /**
     * Host weight in evolution calculation.
     * Default: 0.15
     */
    public static double sporeEvolutionWeightHost = 0.15;

    /**
     * Pollution weight in evolution calculation.
     * Increased from 0.15 to make pollution the primary driver.
     * Default: 0.25
     */
    public static double sporeEvolutionWeightPollution = 0.25;

    /**
     * Voltage weight in evolution calculation.
     * Increased from 0.10 to make technology progression more impactful.
     * Default: 0.20
     */
    public static double sporeEvolutionWeightVoltage = 0.20;

    // ========== Spore Mob Buff Parameters ==========

    /**
     * Pollution bonus divisor for Spore mob health calculation.
     * Health bonus = pollution / divisor (max 100% at divisor value).
     * Default: 200.0 (max bonus at 200 pollution)
     */
    public static double sporePollutionBonusDivisor = 200.0;

    /**
     * Voltage bonus per tier above ULV for Spore mobs.
     * Each tier above ULV adds this percentage to health.
     * Default: 0.10 (10% per tier)
     */
    public static double sporeVoltageBonusPerTier = 0.10;

    /**
     * Evolution bonus per phase for Spore mobs.
     * Each evolution phase adds this percentage to health.
     * Default: 0.05 (5% per phase, max 50% at phase 10)
     */
    public static double sporeEvolutionBonusPerPhase = 0.05;

    /**
     * Pollution threshold for Spore mob damage bonus.
     * Damage bonus only applies when pollution exceeds this value.
     * Default: 100.0
     */
    public static double sporeDamageBonusThreshold = 100.0;

    /**
     * Pollution divisor for Spore mob damage calculation.
     * Damage bonus = pollution / divisor.
     * Default: 200.0
     */
    public static double sporeDamageBonusDivisor = 200.0;

    /**
     * Damage bonus multiplier for Spore mobs.
     * Final damage bonus = (pollution / divisor) * multiplier.
     * Default: 0.3 (30% per 200 pollution)
     */
    public static double sporeDamageBonusMultiplier = 0.3;

    /**
     * Maximum voltage tier for Spore evolution normalization.
     * Used to normalize voltage tier to 0.0-1.0 range.
     * Default: 9.0 (UHV)
     */
    public static double sporeMaxVoltageTier = 9.0;

    /**
     * Infection intensity multiplier (phase to percentage conversion).
     * Converts evolution phase (0-10) to infection intensity (0-100%).
     * Default: 10.0
     */
    public static double sporeInfectionIntensityMultiplier = 10.0;

    // ========== Pollution System Core Configuration ==========

    /**
     * Pollution update interval in ticks.
     * How often pollution values are recalculated.
     * Default: 20 ticks (1 second)
     */
    public static int pollutionUpdateInterval = 20;

    /**
     * Environment scan interval in ticks.
     * How often the system scans for trees/plants that absorb pollution.
     * Default: 6000 ticks (5 minutes)
     */
    public static int pollutionEnvironmentScanInterval = 6000;

    /**
     * Machine scan radius in chunks.
     * How far to scan for pollution-generating machines.
     * Default: 4 chunks
     */
    public static int pollutionMachineScanRadius = 4;

    // ========== Recipe-Based Pollution Configuration ==========

    /**
     * Base pollution per second for machines running recipes.
     * Design target: 1 EV multiblock running 24h = 240 pollution
     * Formula: basePollution × (tier+1)^exponent × multiblockBonus
     * EV (tier 4): 0.21 × (5)^1.3 × 2.0 = 2.77 /sec × 86400 sec = 240 pollution
     * Default: 0.21 (pollution per second)
     */
    public static double basePollutionPerSecond = 0.21;

    /**
     * Tier exponent for pollution calculation.
     * Controls how steeply pollution increases with voltage tier.
     * Higher values = more pollution from high-tier machines.
     * Default: 1.3 (moderate exponential growth)
     */
    public static double pollutionTierExponent = 1.3;

    /**
     * Pollution multiplier for multiblock structures.
     * Multiblock machines produce more pollution than single-block machines.
     * Default: 2.0 (2x more pollution)
     */
    public static double multiblockPollutionMultiplier = 2.0;

    // ========== Environment Absorption Configuration ==========

    /**
     * Grass block absorption rate (pollution per second per block).
     * Typical chunk (16×16) has ~200 grass blocks → 0.3 pollution/sec
     * Default: 0.0015
     */
    public static double grassBlockAbsorption = 0.0015;

    /**
     * Leaves absorption rate (pollution per second per block).
     * Typical chunk has ~150 leaves → 0.375 pollution/sec
     * Default: 0.0025
     */
    public static double leavesAbsorption = 0.0025;

    /**
     * Water absorption rate (pollution per second per block).
     * Typical chunk has ~50 water blocks → 0.06 pollution/sec
     * Default: 0.0012
     */
    public static double waterAbsorption = 0.0012;

    /**
     * Grass (plant) absorption rate (pollution per second per block).
     * Default: 0.001
     */
    public static double grassAbsorption = 0.001;

    /**
     * Log absorption rate (pollution per second per block).
     * Default: 0.001
     */
    public static double logAbsorption = 0.001;

    /**
     * Flower absorption rate (pollution per second per block).
     * Default: 0.0012
     */
    public static double flowerAbsorption = 0.0012;

    /**
     * Maximum environment absorption percentage.
     * Cap on how much pollution can be absorbed per second (as % of current).
     * This prevents "plant forest = invincible" while still making environment useful.
     * Default: 0.15 (15%)
     */
    public static double pollutionEnvAbsorptionMaxPercent = 0.15;

    /**
     * Pollution diffusion rate.
     * How fast pollution spreads to neighboring chunks.
     * Default: 0.15
     */
    public static double pollutionDiffusionRate = 0.15;

    /**
     * Pollution removal threshold.
     * Pollution below this value is removed from the map.
     * Default: 1.0
     */
    public static double pollutionRemovalThreshold = 1.0;

    /**
     * Spore feedback threshold.
     * Total pollution required to trigger Spore biomass feedback.
     * Default: 100.0
     */
    public static double pollutionSporeFeedbackThreshold = 100.0;

    // ========== Performance and Debug Configuration ==========

    /**
     * Maximum global threat entities allowed.
     * Limits total number of threat-spawned entities to prevent lag.
     * Default: 200
     */
    public static int threatMaxGlobalEntities = 200;

    /**
     * Debug log interval in ticks.
     * How often debug messages are logged (when debug is enabled).
     * Default: 100 ticks (5 seconds)
     */
    public static int debugLogInterval = 100;

    /**
     * Spawn rate pollution divisor for threat system.
     * Controls how pollution affects spawn rates: rate * (1.0 + pollution / divisor)
     * Default: 200.0
     */
    public static double threatSpawnRatePollutionDivisor = 200.0;

    // ========== Horde Threat Level System ==========

    /**
     * Distance threshold for threat level 5 (紧急 - Emergency).
     * When nearest Hivemind is closer than this distance (in chunks).
     * Default: 10.0 chunks
     */
    public static double hordeThreatLevel5Distance = 10.0;

    /**
     * Distance threshold for threat level 4 (严重 - Severe).
     * Default: 20.0 chunks
     */
    public static double hordeThreatLevel4Distance = 20.0;

    /**
     * Distance threshold for threat level 3 (危险 - Dangerous).
     * Default: 50.0 chunks
     */
    public static double hordeThreatLevel3Distance = 50.0;

    /**
     * Distance threshold for threat level 2 (紧张 - Tense).
     * Default: 100.0 chunks
     */
    public static double hordeThreatLevel2Distance = 100.0;

    /**
     * Distance threshold for threat level 1 (警戒 - Alert).
     * Default: 200.0 chunks
     */
    public static double hordeThreatLevel1Distance = 200.0;

    /**
     * Threat level update interval in ticks.
     * How often to recalculate player threat levels.
     * Default: 100 ticks (5 seconds)
     */
    public static int hordeThreatUpdateInterval = 100;

    /**
     * Horde cooldown duration in ticks.
     * Minimum time between horde triggers for the same player.
     * Default: 6000 ticks (5 minutes)
     */
    public static int hordePlayerCooldown = 6000;

    // ========== XaerosWorldMap Integration ==========

    /**
     * Enable pollution overlay on XaerosWorldMap.
     * When enabled, pollution levels are displayed as colored overlays on the world map.
     * Default: true
     */
    public static boolean enablePollutionMapOverlay = true;

    /**
     * Alpha transparency for pollution overlay (0.0-1.0).
     * Lower values make the overlay more transparent.
     * Default: 0.4
     */
    public static double pollutionOverlayAlpha = 0.4;

    /**
     * Show exact pollution values in tooltip when hovering over chunks.
     * Default: true
     */
    public static boolean showPollutionTooltip = true;

    /**
     * Configuration file location.
     */
    private static final File CONFIG_FILE = new File("config/triaxis-difficulty.properties");

    /**
     * Applies a difficulty preset, overriding current values.
     * <p>
     * This method sets multiple configuration values at once based on the selected preset.
     * CUSTOM preset does not modify any values.
     *
     * @param preset the preset name (NORMAL, HARD, HARDCORE, INSANE, or CUSTOM)
     */
    public static void applyPreset(String preset) {
        switch (preset.toUpperCase()) {
            case "NORMAL" -> {
                globalMultiplier = 2.0;  // 降低自 2.8
                techWeight = 25.0;
                pollutionWeight = 15.0;
                hpMultFactor = 1.8;  // 降低自 2.2
                attackMultFactor = 1.4;  // 降低自 1.6
                targetDays = 800.0;
                pollutionDenominator = 150.0;  // 提高自 100
                // 新增属性上限
                maxHpMultiplier = 15.0;
                maxAttackMultiplier = 10.0;
                maxSpeedMultiplier = 3.0;
                maxArmorMultiplier = 5.0;
                // 乘法模型参数
                baseMin = 0.5;
                baseMax = 2.0;
                scaleExponent = 1.2;
                scaleMultiplier = 0.15;
                pressureMin = 1.0;
                pressureMax = 4.0;
                sigmoidShift = 2.0;
                // 污染系统调整
                naturalDecayRate = 0.002;  // 比例衰减 0.2%/秒
                tempToPermanentRate = 0.005;  // 提高自 0.001
                tempToPermanentThreshold = 200.0;  // 阈值
                // 威胁系统调整
                hvZombieSpawnThreshold = 70.0;  // 降低自 80
                zombieSpawnChance = 0.015;  // 提高自 0.01
                creeperSpawnChance = 0.008;  // 提高自 0.005
                chargedCreeperChance = 0.002;  // 提高自 0.001
            }
            case "HARD" -> {
                globalMultiplier = 2.5;
                techWeight = 28.0;
                pollutionWeight = 17.0;
                hpMultFactor = 2.0;
                attackMultFactor = 1.5;
                targetDays = 1000.0;
                pollutionDenominator = 150.0;
                maxHpMultiplier = 18.0;
                maxAttackMultiplier = 12.0;
                maxSpeedMultiplier = 3.5;
                maxArmorMultiplier = 6.0;
                // 乘法模型参数
                baseMin = 0.5;
                baseMax = 2.2;
                scaleExponent = 1.25;
                scaleMultiplier = 0.17;
                pressureMin = 1.0;
                pressureMax = 4.5;
                sigmoidShift = 1.8;
                naturalDecayRate = 0.0018;  // 比例衰减，略快
                tempToPermanentRate = 0.006;  // 更快转化
                tempToPermanentThreshold = 180.0;  // 更低阈值
                hvZombieSpawnThreshold = 65.0;
                zombieSpawnChance = 0.018;
                creeperSpawnChance = 0.010;
                chargedCreeperChance = 0.003;
            }
            case "HARDCORE" -> {
                globalMultiplier = 2.8;
                techWeight = 30.0;
                pollutionWeight = 18.0;
                hpMultFactor = 2.2;
                attackMultFactor = 1.6;
                targetDays = 1200.0;
                pollutionDenominator = 150.0;
                maxHpMultiplier = 20.0;
                maxAttackMultiplier = 15.0;
                maxSpeedMultiplier = 4.0;
                maxArmorMultiplier = 7.0;
                // 乘法模型参数
                baseMin = 0.6;
                baseMax = 2.5;
                scaleExponent = 1.3;
                scaleMultiplier = 0.18;
                pressureMin = 1.0;
                pressureMax = 5.0;
                sigmoidShift = 1.5;
                naturalDecayRate = 0.0015;  // 比例衰减，更慢
                tempToPermanentRate = 0.007;  // 更快转化
                tempToPermanentThreshold = 150.0;  // 更低阈值
                hvZombieSpawnThreshold = 60.0;
                zombieSpawnChance = 0.020;
                creeperSpawnChance = 0.012;
                chargedCreeperChance = 0.004;
            }
            case "INSANE" -> {
                globalMultiplier = 3.5;
                techWeight = 35.0;
                pollutionWeight = 22.0;
                hpMultFactor = 3.0;
                attackMultFactor = 2.0;
                targetDays = 1500.0;
                pollutionDenominator = 150.0;
                maxHpMultiplier = 25.0;
                maxAttackMultiplier = 20.0;
                maxSpeedMultiplier = 5.0;
                maxArmorMultiplier = 10.0;
                // 乘法模型参数
                baseMin = 0.7;
                baseMax = 3.0;
                scaleExponent = 1.4;
                scaleMultiplier = 0.20;
                pressureMin = 1.0;
                pressureMax = 6.0;
                sigmoidShift = 1.2;
                naturalDecayRate = 0.001;  // 比例衰减，极慢
                tempToPermanentRate = 0.010;  // 极快转化
                tempToPermanentThreshold = 100.0;  // 极低阈值
                hvZombieSpawnThreshold = 50.0;
                zombieSpawnChance = 0.025;
                creeperSpawnChance = 0.015;
                chargedCreeperChance = 0.005;
            }
            // CUSTOM does not modify any values, uses config file values
        }
    }

    /**
     * Gets the target HP multiplier for a given voltage tier.
     * <p>
     * For tiers beyond UHV (tier 9), the multiplier continues to increase
     * by 30% per tier.
     *
     * @param tier the voltage tier (0=ULV, 1=LV, 2=MV, etc.)
     * @return the HP multiplier for that tier
     */
    public static double getHpTargetForTier(int tier) {
        return switch (tier) {
            case 0 -> ulvHpTarget;
            case 1 -> lvHpTarget;
            case 2 -> mvHpTarget;
            case 3 -> hvHpTarget;
            case 4 -> evHpTarget;
            case 5 -> ivHpTarget;
            case 6 -> luvHpTarget;
            case 7 -> zpmHpTarget;
            case 8 -> uvHpTarget;
            case 9 -> uhvHpTarget;
            default -> uhvHpTarget * (1 + (tier - 9) * 0.3); // Beyond UHV continues to grow
        };
    }

    /**
     * Gets the effective maximum voltage tier for normalization.
     * <p>
     * This is the minimum of the user-configured max tier and the GT mod's max tier,
     * ensuring compatibility.
     *
     * @return the effective maximum tier
     */
    public static int getEffectiveMaxTier() {
        return Math.min(maxIndustrialTier, maxGTTier);
    }

    /**
     * Loads configuration from the properties file.
     * <p>
     * If the file doesn't exist, applies the default preset and saves it.
     * If the file exists, loads all values and applies the selected preset
     * (unless preset is CUSTOM).
     */
    public static void load() {
        Properties props = new Properties();
        if (CONFIG_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                props.load(in);

                // 三轴权重
                weightTime = Double.parseDouble(props.getProperty("weightTime", String.valueOf(weightTime)));
                weightVoltage = Double.parseDouble(props.getProperty("weightVoltage", String.valueOf(weightVoltage)));
                weightPollution = Double.parseDouble(props.getProperty("weightPollution", String.valueOf(weightPollution)));

                // 全局系数
                globalMultiplier = Double.parseDouble(props.getProperty("globalMultiplier", String.valueOf(globalMultiplier)));
                emaAlpha = Double.parseDouble(props.getProperty("emaAlpha", String.valueOf(emaAlpha)));
                pollutionDenominator = Double.parseDouble(props.getProperty("pollutionDenominator", String.valueOf(pollutionDenominator)));

                // 乘法模型参数
                baseMin = Double.parseDouble(props.getProperty("baseMin", String.valueOf(baseMin)));
                baseMax = Double.parseDouble(props.getProperty("baseMax", String.valueOf(baseMax)));
                scaleExponent = Double.parseDouble(props.getProperty("scaleExponent", String.valueOf(scaleExponent)));
                scaleMultiplier = Double.parseDouble(props.getProperty("scaleMultiplier", String.valueOf(scaleMultiplier)));
                pressureMin = Double.parseDouble(props.getProperty("pressureMin", String.valueOf(pressureMin)));
                pressureMax = Double.parseDouble(props.getProperty("pressureMax", String.valueOf(pressureMax)));
                sigmoidShift = Double.parseDouble(props.getProperty("sigmoidShift", String.valueOf(sigmoidShift)));

                // 工业加成
                techWeight = Double.parseDouble(props.getProperty("techWeight", String.valueOf(techWeight)));
                pollutionWeight = Double.parseDouble(props.getProperty("pollutionWeight", String.valueOf(pollutionWeight)));
                maxIndustrialTier = Integer.parseInt(props.getProperty("maxIndustrialTier", String.valueOf(maxIndustrialTier)));

                // 扫描参数
                scanRadiusBlocks = Integer.parseInt(props.getProperty("scanRadiusBlocks", String.valueOf(scanRadiusBlocks)));
                maxGTTier = Integer.parseInt(props.getProperty("maxGTTier", String.valueOf(maxGTTier)));
                maxChangePerSec = Double.parseDouble(props.getProperty("maxChangePerSec", String.valueOf(maxChangePerSec)));

                // 时间曲线
                targetDays = Double.parseDouble(props.getProperty("targetDays", String.valueOf(targetDays)));
                baseDays = Double.parseDouble(props.getProperty("baseDays", String.valueOf(baseDays)));

                // 属性增幅
                hpMultFactor = Double.parseDouble(props.getProperty("hpMultFactor", String.valueOf(hpMultFactor)));
                attackMultFactor = Double.parseDouble(props.getProperty("attackMultFactor", String.valueOf(attackMultFactor)));
                speedMultFactor = Double.parseDouble(props.getProperty("speedMultFactor", String.valueOf(speedMultFactor)));
                armorMultFactor = Double.parseDouble(props.getProperty("armorMultFactor", String.valueOf(armorMultFactor)));

                // 属性上限
                maxHpMultiplier = Double.parseDouble(props.getProperty("maxHpMultiplier", String.valueOf(maxHpMultiplier)));
                maxAttackMultiplier = Double.parseDouble(props.getProperty("maxAttackMultiplier", String.valueOf(maxAttackMultiplier)));
                maxSpeedMultiplier = Double.parseDouble(props.getProperty("maxSpeedMultiplier", String.valueOf(maxSpeedMultiplier)));
                maxArmorMultiplier = Double.parseDouble(props.getProperty("maxArmorMultiplier", String.valueOf(maxArmorMultiplier)));

                // 电压阶段血量目标
                ulvHpTarget = Double.parseDouble(props.getProperty("ulvHpTarget", String.valueOf(ulvHpTarget)));
                lvHpTarget = Double.parseDouble(props.getProperty("lvHpTarget", String.valueOf(lvHpTarget)));
                mvHpTarget = Double.parseDouble(props.getProperty("mvHpTarget", String.valueOf(mvHpTarget)));
                hvHpTarget = Double.parseDouble(props.getProperty("hvHpTarget", String.valueOf(hvHpTarget)));
                evHpTarget = Double.parseDouble(props.getProperty("evHpTarget", String.valueOf(evHpTarget)));
                ivHpTarget = Double.parseDouble(props.getProperty("ivHpTarget", String.valueOf(ivHpTarget)));
                luvHpTarget = Double.parseDouble(props.getProperty("luvHpTarget", String.valueOf(luvHpTarget)));
                zpmHpTarget = Double.parseDouble(props.getProperty("zpmHpTarget", String.valueOf(zpmHpTarget)));
                uvHpTarget = Double.parseDouble(props.getProperty("uvHpTarget", String.valueOf(uvHpTarget)));
                uhvHpTarget = Double.parseDouble(props.getProperty("uhvHpTarget", String.valueOf(uhvHpTarget)));

                // GT污染集成
                gtPollutionWeight = Double.parseDouble(props.getProperty("gtPollutionWeight", String.valueOf(gtPollutionWeight)));
                gtSourceMultiplierBase = Double.parseDouble(props.getProperty("gtSourceMultiplierBase", String.valueOf(gtSourceMultiplierBase)));
                gtSourceThreshold = Integer.parseInt(props.getProperty("gtSourceThreshold", String.valueOf(gtSourceThreshold)));
                airScrubberEfficiency = Double.parseDouble(props.getProperty("airScrubberEfficiency", String.valueOf(airScrubberEfficiency)));

                // 污染生成配置
                basePollutionRate = Double.parseDouble(props.getProperty("basePollutionRate", String.valueOf(basePollutionRate)));
                tierGrowthFactor = Double.parseDouble(props.getProperty("tierGrowthFactor", String.valueOf(tierGrowthFactor)));
                multiblockPollutionMultiplier = Double.parseDouble(props.getProperty("multiblockPollutionMultiplier", String.valueOf(multiblockPollutionMultiplier)));

                // 污染衰减和吸收
                naturalDecayRate = Double.parseDouble(props.getProperty("naturalDecayRate", String.valueOf(naturalDecayRate)));

                // 污染转化
                tempToPermanentThreshold = Double.parseDouble(props.getProperty("tempToPermanentThreshold", String.valueOf(tempToPermanentThreshold)));
                tempToPermanentRate = Double.parseDouble(props.getProperty("tempToPermanentRate", String.valueOf(tempToPermanentRate)));
                permanentToDifficultyRate = Double.parseDouble(props.getProperty("permanentToDifficultyRate", String.valueOf(permanentToDifficultyRate)));

                // 炮塔系统
                flamethrowerDirectDamage = Double.parseDouble(props.getProperty("flamethrowerDirectDamage", String.valueOf(flamethrowerDirectDamage)));
                flamethrowerGroundDamage = Double.parseDouble(props.getProperty("flamethrowerGroundDamage", String.valueOf(flamethrowerGroundDamage)));

                // 威胁触发等级配置
                zombieAttackMinTier = Integer.parseInt(props.getProperty("zombieAttackMinTier", String.valueOf(zombieAttackMinTier)));
                zombieSpawnMinTier = Integer.parseInt(props.getProperty("zombieSpawnMinTier", String.valueOf(zombieSpawnMinTier)));
                creeperSpawnMinTier = Integer.parseInt(props.getProperty("creeperSpawnMinTier", String.valueOf(creeperSpawnMinTier)));

                // 威胁系统详细配置
                hvZombieSpawnThreshold = Double.parseDouble(props.getProperty("hvZombieSpawnThreshold", String.valueOf(hvZombieSpawnThreshold)));
                hvCreeperSpawnThreshold = Double.parseDouble(props.getProperty("hvCreeperSpawnThreshold", String.valueOf(hvCreeperSpawnThreshold)));
                chargedCreeperThreshold = Double.parseDouble(props.getProperty("chargedCreeperThreshold", String.valueOf(chargedCreeperThreshold)));
                zombieSpawnChance = Double.parseDouble(props.getProperty("zombieSpawnChance", String.valueOf(zombieSpawnChance)));
                creeperSpawnChance = Double.parseDouble(props.getProperty("creeperSpawnChance", String.valueOf(creeperSpawnChance)));
                chargedCreeperChance = Double.parseDouble(props.getProperty("chargedCreeperChance", String.valueOf(chargedCreeperChance)));

                // 波次系统配置
                waveBaseSize = Integer.parseInt(props.getProperty("waveBaseSize", String.valueOf(waveBaseSize)));
                waveCreeperBaseSize = Integer.parseInt(props.getProperty("waveCreeperBaseSize", String.valueOf(waveCreeperBaseSize)));
                wavePollutionDivisor = Integer.parseInt(props.getProperty("wavePollutionDivisor", String.valueOf(wavePollutionDivisor)));
                waveTierDivisor = Integer.parseInt(props.getProperty("waveTierDivisor", String.valueOf(waveTierDivisor)));
                waveMaxSize = Integer.parseInt(props.getProperty("waveMaxSize", String.valueOf(waveMaxSize)));
                waveCreeperMaxSize = Integer.parseInt(props.getProperty("waveCreeperMaxSize", String.valueOf(waveCreeperMaxSize)));
                waveCreeperChance = Double.parseDouble(props.getProperty("waveCreeperChance", String.valueOf(waveCreeperChance)));

                // Hivemind接近系统
                hivemindProximityRadius = Double.parseDouble(props.getProperty("hivemindProximityRadius", String.valueOf(hivemindProximityRadius)));
                hivemindAccelerationFactor = Double.parseDouble(props.getProperty("hivemindAccelerationFactor", String.valueOf(hivemindAccelerationFactor)));
                enableHivemindAcceleration = Boolean.parseBoolean(props.getProperty("enableHivemindAcceleration", String.valueOf(enableHivemindAcceleration)));

                // Hordes集成
                enableHordeIntegration = Boolean.parseBoolean(props.getProperty("enableHordeIntegration", String.valueOf(enableHordeIntegration)));
                hordeIntensityMultiplier = Double.parseDouble(props.getProperty("hordeIntensityMultiplier", String.valueOf(hordeIntensityMultiplier)));
                difficultyToIntensityFactor = Double.parseDouble(props.getProperty("difficultyToIntensityFactor", String.valueOf(difficultyToIntensityFactor)));
                enablePollutionTriggeredHordes = Boolean.parseBoolean(props.getProperty("enablePollutionTriggeredHordes", String.valueOf(enablePollutionTriggeredHordes)));
                pollutionHordeTriggerThreshold = Double.parseDouble(props.getProperty("pollutionHordeTriggerThreshold", String.valueOf(pollutionHordeTriggerThreshold)));
                pollutionHordeCheckInterval = Double.parseDouble(props.getProperty("pollutionHordeCheckInterval", String.valueOf(pollutionHordeCheckInterval)));
                pollutionHordeTriggerChance = Double.parseDouble(props.getProperty("pollutionHordeTriggerChance", String.valueOf(pollutionHordeTriggerChance)));
                enableSkirmishes = Boolean.parseBoolean(props.getProperty("enableSkirmishes", String.valueOf(enableSkirmishes)));
                skirmishPollutionThreshold = Double.parseDouble(props.getProperty("skirmishPollutionThreshold", String.valueOf(skirmishPollutionThreshold)));
                skirmishInterval = Double.parseDouble(props.getProperty("skirmishInterval", String.valueOf(skirmishInterval)));
                skirmishMinCount = Integer.parseInt(props.getProperty("skirmishMinCount", String.valueOf(skirmishMinCount)));
                skirmishMaxCount = Integer.parseInt(props.getProperty("skirmishMaxCount", String.valueOf(skirmishMaxCount)));
                majorHordePollutionThreshold = Double.parseDouble(props.getProperty("majorHordePollutionThreshold", String.valueOf(majorHordePollutionThreshold)));
                majorHordeMultiplier = Double.parseDouble(props.getProperty("majorHordeMultiplier", String.valueOf(majorHordeMultiplier)));
                enableMachineTargeting = Boolean.parseBoolean(props.getProperty("enableMachineTargeting", String.valueOf(enableMachineTargeting)));
                machineTargetingRange = Double.parseDouble(props.getProperty("machineTargetingRange", String.valueOf(machineTargetingRange)));
                machineTargetingChance = Double.parseDouble(props.getProperty("machineTargetingChance", String.valueOf(machineTargetingChance)));
                enableVoltageTierScaling = Boolean.parseBoolean(props.getProperty("enableVoltageTierScaling", String.valueOf(enableVoltageTierScaling)));

                // Load horde tier intensity multipliers array
                String tierMultipliersStr = props.getProperty("hordeTierIntensityMultipliers", "1.0,1.1,1.3,1.5,1.8,2.2,2.6,3.0,3.5,4.0");
                String[] tierMultipliersParts = tierMultipliersStr.split(",");
                if (tierMultipliersParts.length == 10) {
                    for (int i = 0; i < 10; i++) {
                        hordeTierIntensityMultipliers[i] = Double.parseDouble(tierMultipliersParts[i].trim());
                    }
                }

                // 预设和调试
                difficultyPreset = props.getProperty("difficultyPreset", difficultyPreset);
                // ImprovedMobs Integration
                takeoverImprovedMobsDifficulty = Boolean.parseBoolean(props.getProperty("takeoverImprovedMobsDifficulty", String.valueOf(takeoverImprovedMobsDifficulty)));
                targetMaxDifficulty = Double.parseDouble(props.getProperty("targetMaxDifficulty", String.valueOf(targetMaxDifficulty)));
                realWorldDaysToMax = Double.parseDouble(props.getProperty("realWorldDaysToMax", String.valueOf(realWorldDaysToMax)));

                // Mob Spawn Enhancement
                enableSpawnEnhancement = Boolean.parseBoolean(props.getProperty("enableSpawnEnhancement", String.valueOf(enableSpawnEnhancement)));
                spawnMultiplier = Double.parseDouble(props.getProperty("spawnMultiplier", String.valueOf(spawnMultiplier)));
                difficultySpawnBonus = Double.parseDouble(props.getProperty("difficultySpawnBonus", String.valueOf(difficultySpawnBonus)));
                maxSpawnChance = Double.parseDouble(props.getProperty("maxSpawnChance", String.valueOf(maxSpawnChance)));
                enableForcedSpawning = Boolean.parseBoolean(props.getProperty("enableForcedSpawning", String.valueOf(enableForcedSpawning)));
                forcedSpawnThreshold = Double.parseDouble(props.getProperty("forcedSpawnThreshold", String.valueOf(forcedSpawnThreshold)));
                forcedSpawnChance = Double.parseDouble(props.getProperty("forcedSpawnChance", String.valueOf(forcedSpawnChance)));
                forcedSpawnCount = Integer.parseInt(props.getProperty("forcedSpawnCount", String.valueOf(forcedSpawnCount)));

                // Debug Options
                enableDebugLines = Boolean.parseBoolean(props.getProperty("enableDebugLines", String.valueOf(enableDebugLines)));
                enableDifficultyLogging = Boolean.parseBoolean(props.getProperty("enableDifficultyLogging", String.valueOf(enableDifficultyLogging)));
                enableSporeDebug = Boolean.parseBoolean(props.getProperty("enableSporeDebug", String.valueOf(enableSporeDebug)));

                // Spore 集成配置
                sporePollutionToBiomassRate = Double.parseDouble(props.getProperty("sporePollutionToBiomassRate", String.valueOf(sporePollutionToBiomassRate)));
                sporeMaxBiomassPerProto = Integer.parseInt(props.getProperty("sporeMaxBiomassPerProto", String.valueOf(sporeMaxBiomassPerProto)));
                sporePollutionFeedbackThreshold = Double.parseDouble(props.getProperty("sporePollutionFeedbackThreshold", String.valueOf(sporePollutionFeedbackThreshold)));
                sporeMaxHealthMultiplier = Double.parseDouble(props.getProperty("sporeMaxHealthMultiplier", String.valueOf(sporeMaxHealthMultiplier)));
                sporeMaxDamageMultiplier = Double.parseDouble(props.getProperty("sporeMaxDamageMultiplier", String.valueOf(sporeMaxDamageMultiplier)));
                sporeHivemindSaturation = Integer.parseInt(props.getProperty("sporeHivemindSaturation", String.valueOf(sporeHivemindSaturation)));
                sporeBiomassSaturation = Integer.parseInt(props.getProperty("sporeBiomassSaturation", String.valueOf(sporeBiomassSaturation)));
                sporeHostSaturation = Integer.parseInt(props.getProperty("sporeHostSaturation", String.valueOf(sporeHostSaturation)));
                sporePollutionSaturation = Double.parseDouble(props.getProperty("sporePollutionSaturation", String.valueOf(sporePollutionSaturation)));
                sporeEvolutionWeightHivemind = Double.parseDouble(props.getProperty("sporeEvolutionWeightHivemind", String.valueOf(sporeEvolutionWeightHivemind)));
                sporeEvolutionWeightBiomass = Double.parseDouble(props.getProperty("sporeEvolutionWeightBiomass", String.valueOf(sporeEvolutionWeightBiomass)));
                sporeEvolutionWeightHost = Double.parseDouble(props.getProperty("sporeEvolutionWeightHost", String.valueOf(sporeEvolutionWeightHost)));
                sporeEvolutionWeightPollution = Double.parseDouble(props.getProperty("sporeEvolutionWeightPollution", String.valueOf(sporeEvolutionWeightPollution)));
                sporeEvolutionWeightVoltage = Double.parseDouble(props.getProperty("sporeEvolutionWeightVoltage", String.valueOf(sporeEvolutionWeightVoltage)));

                // Spore 怪物增强参数
                sporePollutionBonusDivisor = Double.parseDouble(props.getProperty("sporePollutionBonusDivisor", String.valueOf(sporePollutionBonusDivisor)));
                sporeVoltageBonusPerTier = Double.parseDouble(props.getProperty("sporeVoltageBonusPerTier", String.valueOf(sporeVoltageBonusPerTier)));
                sporeEvolutionBonusPerPhase = Double.parseDouble(props.getProperty("sporeEvolutionBonusPerPhase", String.valueOf(sporeEvolutionBonusPerPhase)));
                sporeDamageBonusThreshold = Double.parseDouble(props.getProperty("sporeDamageBonusThreshold", String.valueOf(sporeDamageBonusThreshold)));
                sporeDamageBonusDivisor = Double.parseDouble(props.getProperty("sporeDamageBonusDivisor", String.valueOf(sporeDamageBonusDivisor)));
                sporeDamageBonusMultiplier = Double.parseDouble(props.getProperty("sporeDamageBonusMultiplier", String.valueOf(sporeDamageBonusMultiplier)));
                sporeMaxVoltageTier = Double.parseDouble(props.getProperty("sporeMaxVoltageTier", String.valueOf(sporeMaxVoltageTier)));
                sporeInfectionIntensityMultiplier = Double.parseDouble(props.getProperty("sporeInfectionIntensityMultiplier", String.valueOf(sporeInfectionIntensityMultiplier)));

                // 污染系统核心配置
                pollutionUpdateInterval = Integer.parseInt(props.getProperty("pollutionUpdateInterval", String.valueOf(pollutionUpdateInterval)));
                pollutionEnvironmentScanInterval = Integer.parseInt(props.getProperty("pollutionEnvironmentScanInterval", String.valueOf(pollutionEnvironmentScanInterval)));
                pollutionMachineScanRadius = Integer.parseInt(props.getProperty("pollutionMachineScanRadius", String.valueOf(pollutionMachineScanRadius)));
                basePollutionPerSecond = Double.parseDouble(props.getProperty("basePollutionPerSecond", String.valueOf(basePollutionPerSecond)));
                pollutionTierExponent = Double.parseDouble(props.getProperty("pollutionTierExponent", String.valueOf(pollutionTierExponent)));
                multiblockPollutionMultiplier = Double.parseDouble(props.getProperty("multiblockPollutionMultiplier", String.valueOf(multiblockPollutionMultiplier)));

                // Environment absorption
                grassBlockAbsorption = Double.parseDouble(props.getProperty("grassBlockAbsorption", String.valueOf(grassBlockAbsorption)));
                leavesAbsorption = Double.parseDouble(props.getProperty("leavesAbsorption", String.valueOf(leavesAbsorption)));
                waterAbsorption = Double.parseDouble(props.getProperty("waterAbsorption", String.valueOf(waterAbsorption)));
                grassAbsorption = Double.parseDouble(props.getProperty("grassAbsorption", String.valueOf(grassAbsorption)));
                logAbsorption = Double.parseDouble(props.getProperty("logAbsorption", String.valueOf(logAbsorption)));
                flowerAbsorption = Double.parseDouble(props.getProperty("flowerAbsorption", String.valueOf(flowerAbsorption)));
                pollutionEnvAbsorptionMaxPercent = Double.parseDouble(props.getProperty("pollutionEnvAbsorptionMaxPercent", String.valueOf(pollutionEnvAbsorptionMaxPercent)));
                pollutionDiffusionRate = Double.parseDouble(props.getProperty("pollutionDiffusionRate", String.valueOf(pollutionDiffusionRate)));
                pollutionRemovalThreshold = Double.parseDouble(props.getProperty("pollutionRemovalThreshold", String.valueOf(pollutionRemovalThreshold)));
                pollutionSporeFeedbackThreshold = Double.parseDouble(props.getProperty("pollutionSporeFeedbackThreshold", String.valueOf(pollutionSporeFeedbackThreshold)));

                // 性能和调试配置
                threatMaxGlobalEntities = Integer.parseInt(props.getProperty("threatMaxGlobalEntities", String.valueOf(threatMaxGlobalEntities)));
                debugLogInterval = Integer.parseInt(props.getProperty("debugLogInterval", String.valueOf(debugLogInterval)));
                threatSpawnRatePollutionDivisor = Double.parseDouble(props.getProperty("threatSpawnRatePollutionDivisor", String.valueOf(threatSpawnRatePollutionDivisor)));

                // XaerosWorldMap集成配置
                enablePollutionMapOverlay = Boolean.parseBoolean(props.getProperty("enablePollutionMapOverlay", String.valueOf(enablePollutionMapOverlay)));
                pollutionOverlayAlpha = Double.parseDouble(props.getProperty("pollutionOverlayAlpha", String.valueOf(pollutionOverlayAlpha)));
                showPollutionTooltip = Boolean.parseBoolean(props.getProperty("showPollutionTooltip", String.valueOf(showPollutionTooltip)));

                // 应用预设（如果不是CUSTOM）
                if (!difficultyPreset.equalsIgnoreCase("CUSTOM")) {
                    applyPreset(difficultyPreset);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // First run, apply default preset
            applyPreset(difficultyPreset);
            save();
        }
    }

    /**
     * Saves the current configuration to the properties file.
     * <p>
     * The saved file includes extensive comments explaining each parameter.
     * This method is called automatically after loading if the file doesn't exist,
     * or can be called manually to persist configuration changes.
     */
    public static void save() {
        Properties props = new Properties();

        // 三轴权重
        props.setProperty("weightTime", String.valueOf(weightTime));
        props.setProperty("weightVoltage", String.valueOf(weightVoltage));
        props.setProperty("weightPollution", String.valueOf(weightPollution));

        // 全局系数
        props.setProperty("globalMultiplier", String.valueOf(globalMultiplier));
        props.setProperty("emaAlpha", String.valueOf(emaAlpha));
        props.setProperty("pollutionDenominator", String.valueOf(pollutionDenominator));

        // 乘法模型参数
        props.setProperty("baseMin", String.valueOf(baseMin));
        props.setProperty("baseMax", String.valueOf(baseMax));
        props.setProperty("scaleExponent", String.valueOf(scaleExponent));
        props.setProperty("scaleMultiplier", String.valueOf(scaleMultiplier));
        props.setProperty("pressureMin", String.valueOf(pressureMin));
        props.setProperty("pressureMax", String.valueOf(pressureMax));
        props.setProperty("sigmoidShift", String.valueOf(sigmoidShift));

        // 工业加成
        props.setProperty("techWeight", String.valueOf(techWeight));
        props.setProperty("pollutionWeight", String.valueOf(pollutionWeight));
        props.setProperty("maxIndustrialTier", String.valueOf(maxIndustrialTier));

        // 扫描参数
        props.setProperty("scanRadiusBlocks", String.valueOf(scanRadiusBlocks));
        props.setProperty("maxGTTier", String.valueOf(maxGTTier));
        props.setProperty("maxChangePerSec", String.valueOf(maxChangePerSec));

        // 时间曲线
        props.setProperty("targetDays", String.valueOf(targetDays));
        props.setProperty("baseDays", String.valueOf(baseDays));

        // 属性增幅
        props.setProperty("hpMultFactor", String.valueOf(hpMultFactor));
        props.setProperty("attackMultFactor", String.valueOf(attackMultFactor));
        props.setProperty("speedMultFactor", String.valueOf(speedMultFactor));
        props.setProperty("armorMultFactor", String.valueOf(armorMultFactor));

        // 属性上限
        props.setProperty("maxHpMultiplier", String.valueOf(maxHpMultiplier));
        props.setProperty("maxAttackMultiplier", String.valueOf(maxAttackMultiplier));
        props.setProperty("maxSpeedMultiplier", String.valueOf(maxSpeedMultiplier));
        props.setProperty("maxArmorMultiplier", String.valueOf(maxArmorMultiplier));

        // 电压阶段血量目标
        props.setProperty("ulvHpTarget", String.valueOf(ulvHpTarget));
        props.setProperty("lvHpTarget", String.valueOf(lvHpTarget));
        props.setProperty("mvHpTarget", String.valueOf(mvHpTarget));
        props.setProperty("hvHpTarget", String.valueOf(hvHpTarget));
        props.setProperty("evHpTarget", String.valueOf(evHpTarget));
        props.setProperty("ivHpTarget", String.valueOf(ivHpTarget));
        props.setProperty("luvHpTarget", String.valueOf(luvHpTarget));
        props.setProperty("zpmHpTarget", String.valueOf(zpmHpTarget));
        props.setProperty("uvHpTarget", String.valueOf(uvHpTarget));
        props.setProperty("uhvHpTarget", String.valueOf(uhvHpTarget));

        // GT污染集成
        props.setProperty("gtPollutionWeight", String.valueOf(gtPollutionWeight));
        props.setProperty("gtSourceMultiplierBase", String.valueOf(gtSourceMultiplierBase));
        props.setProperty("gtSourceThreshold", String.valueOf(gtSourceThreshold));
        props.setProperty("airScrubberEfficiency", String.valueOf(airScrubberEfficiency));

        // 污染生成配置
        props.setProperty("basePollutionRate", String.valueOf(basePollutionRate));
        props.setProperty("tierGrowthFactor", String.valueOf(tierGrowthFactor));
        props.setProperty("multiblockPollutionMultiplier", String.valueOf(multiblockPollutionMultiplier));

        // 污染衰减和吸收
        props.setProperty("naturalDecayRate", String.valueOf(naturalDecayRate));

        // 污染转化
        props.setProperty("tempToPermanentThreshold", String.valueOf(tempToPermanentThreshold));
        props.setProperty("tempToPermanentRate", String.valueOf(tempToPermanentRate));
        props.setProperty("permanentToDifficultyRate", String.valueOf(permanentToDifficultyRate));

        // 炮塔系统
        props.setProperty("flamethrowerDirectDamage", String.valueOf(flamethrowerDirectDamage));
        props.setProperty("flamethrowerGroundDamage", String.valueOf(flamethrowerGroundDamage));

        // 威胁触发等级配置
        props.setProperty("zombieAttackMinTier", String.valueOf(zombieAttackMinTier));
        props.setProperty("zombieSpawnMinTier", String.valueOf(zombieSpawnMinTier));
        props.setProperty("creeperSpawnMinTier", String.valueOf(creeperSpawnMinTier));

        // 威胁系统详细配置
        props.setProperty("hvZombieSpawnThreshold", String.valueOf(hvZombieSpawnThreshold));
        props.setProperty("hvCreeperSpawnThreshold", String.valueOf(hvCreeperSpawnThreshold));
        props.setProperty("chargedCreeperThreshold", String.valueOf(chargedCreeperThreshold));
        props.setProperty("zombieSpawnChance", String.valueOf(zombieSpawnChance));
        props.setProperty("creeperSpawnChance", String.valueOf(creeperSpawnChance));
        props.setProperty("chargedCreeperChance", String.valueOf(chargedCreeperChance));

        // 波次系统配置
        props.setProperty("waveBaseSize", String.valueOf(waveBaseSize));
        props.setProperty("waveCreeperBaseSize", String.valueOf(waveCreeperBaseSize));
        props.setProperty("wavePollutionDivisor", String.valueOf(wavePollutionDivisor));
        props.setProperty("waveTierDivisor", String.valueOf(waveTierDivisor));
        props.setProperty("waveMaxSize", String.valueOf(waveMaxSize));
        props.setProperty("waveCreeperMaxSize", String.valueOf(waveCreeperMaxSize));
        props.setProperty("waveCreeperChance", String.valueOf(waveCreeperChance));

        // Hivemind接近系统
        props.setProperty("hivemindProximityRadius", String.valueOf(hivemindProximityRadius));
        props.setProperty("hivemindAccelerationFactor", String.valueOf(hivemindAccelerationFactor));
        props.setProperty("enableHivemindAcceleration", String.valueOf(enableHivemindAcceleration));

        // Hordes集成
        props.setProperty("enableHordeIntegration", String.valueOf(enableHordeIntegration));
        props.setProperty("hordeIntensityMultiplier", String.valueOf(hordeIntensityMultiplier));
        props.setProperty("difficultyToIntensityFactor", String.valueOf(difficultyToIntensityFactor));
        props.setProperty("enablePollutionTriggeredHordes", String.valueOf(enablePollutionTriggeredHordes));
        props.setProperty("pollutionHordeTriggerThreshold", String.valueOf(pollutionHordeTriggerThreshold));
        props.setProperty("pollutionHordeCheckInterval", String.valueOf(pollutionHordeCheckInterval));
        props.setProperty("pollutionHordeTriggerChance", String.valueOf(pollutionHordeTriggerChance));
        props.setProperty("enableSkirmishes", String.valueOf(enableSkirmishes));
        props.setProperty("skirmishPollutionThreshold", String.valueOf(skirmishPollutionThreshold));
        props.setProperty("skirmishInterval", String.valueOf(skirmishInterval));
        props.setProperty("skirmishMinCount", String.valueOf(skirmishMinCount));
        props.setProperty("skirmishMaxCount", String.valueOf(skirmishMaxCount));
        props.setProperty("majorHordePollutionThreshold", String.valueOf(majorHordePollutionThreshold));
        props.setProperty("majorHordeMultiplier", String.valueOf(majorHordeMultiplier));
        props.setProperty("enableMachineTargeting", String.valueOf(enableMachineTargeting));
        props.setProperty("machineTargetingRange", String.valueOf(machineTargetingRange));
        props.setProperty("machineTargetingChance", String.valueOf(machineTargetingChance));
        props.setProperty("enableVoltageTierScaling", String.valueOf(enableVoltageTierScaling));

        // Save horde tier intensity multipliers array
        StringBuilder tierMultipliersStr = new StringBuilder();
        for (int i = 0; i < hordeTierIntensityMultipliers.length; i++) {
            if (i > 0) tierMultipliersStr.append(",");
            tierMultipliersStr.append(hordeTierIntensityMultipliers[i]);
        }
        props.setProperty("hordeTierIntensityMultipliers", tierMultipliersStr.toString());

        // 预设和调试
        props.setProperty("difficultyPreset", difficultyPreset);
        // ImprovedMobs Integration
        props.setProperty("takeoverImprovedMobsDifficulty", String.valueOf(takeoverImprovedMobsDifficulty));
        props.setProperty("targetMaxDifficulty", String.valueOf(targetMaxDifficulty));
        props.setProperty("realWorldDaysToMax", String.valueOf(realWorldDaysToMax));

        // Mob Spawn Enhancement
        props.setProperty("enableSpawnEnhancement", String.valueOf(enableSpawnEnhancement));
        props.setProperty("spawnMultiplier", String.valueOf(spawnMultiplier));
        props.setProperty("difficultySpawnBonus", String.valueOf(difficultySpawnBonus));
        props.setProperty("maxSpawnChance", String.valueOf(maxSpawnChance));
        props.setProperty("enableForcedSpawning", String.valueOf(enableForcedSpawning));
        props.setProperty("forcedSpawnThreshold", String.valueOf(forcedSpawnThreshold));
        props.setProperty("forcedSpawnChance", String.valueOf(forcedSpawnChance));
        props.setProperty("forcedSpawnCount", String.valueOf(forcedSpawnCount));

        // Debug Options
        props.setProperty("enableDebugLines", String.valueOf(enableDebugLines));
        props.setProperty("enableDifficultyLogging", String.valueOf(enableDifficultyLogging));
        props.setProperty("enableSporeDebug", String.valueOf(enableSporeDebug));

        // Spore 集成配置
        props.setProperty("sporePollutionToBiomassRate", String.valueOf(sporePollutionToBiomassRate));
        props.setProperty("sporeMaxBiomassPerProto", String.valueOf(sporeMaxBiomassPerProto));
        props.setProperty("sporePollutionFeedbackThreshold", String.valueOf(sporePollutionFeedbackThreshold));
        props.setProperty("sporeMaxHealthMultiplier", String.valueOf(sporeMaxHealthMultiplier));
        props.setProperty("sporeMaxDamageMultiplier", String.valueOf(sporeMaxDamageMultiplier));
        props.setProperty("sporeHivemindSaturation", String.valueOf(sporeHivemindSaturation));
        props.setProperty("sporeBiomassSaturation", String.valueOf(sporeBiomassSaturation));
        props.setProperty("sporeHostSaturation", String.valueOf(sporeHostSaturation));
        props.setProperty("sporePollutionSaturation", String.valueOf(sporePollutionSaturation));
        props.setProperty("sporeEvolutionWeightHivemind", String.valueOf(sporeEvolutionWeightHivemind));
        props.setProperty("sporeEvolutionWeightBiomass", String.valueOf(sporeEvolutionWeightBiomass));
        props.setProperty("sporeEvolutionWeightHost", String.valueOf(sporeEvolutionWeightHost));
        props.setProperty("sporeEvolutionWeightPollution", String.valueOf(sporeEvolutionWeightPollution));
        props.setProperty("sporeEvolutionWeightVoltage", String.valueOf(sporeEvolutionWeightVoltage));

        // Spore 怪物增强参数
        props.setProperty("sporePollutionBonusDivisor", String.valueOf(sporePollutionBonusDivisor));
        props.setProperty("sporeVoltageBonusPerTier", String.valueOf(sporeVoltageBonusPerTier));
        props.setProperty("sporeEvolutionBonusPerPhase", String.valueOf(sporeEvolutionBonusPerPhase));
        props.setProperty("sporeDamageBonusThreshold", String.valueOf(sporeDamageBonusThreshold));
        props.setProperty("sporeDamageBonusDivisor", String.valueOf(sporeDamageBonusDivisor));
        props.setProperty("sporeDamageBonusMultiplier", String.valueOf(sporeDamageBonusMultiplier));
        props.setProperty("sporeMaxVoltageTier", String.valueOf(sporeMaxVoltageTier));
        props.setProperty("sporeInfectionIntensityMultiplier", String.valueOf(sporeInfectionIntensityMultiplier));

        // 污染系统核心配置
        props.setProperty("pollutionUpdateInterval", String.valueOf(pollutionUpdateInterval));
        props.setProperty("pollutionEnvironmentScanInterval", String.valueOf(pollutionEnvironmentScanInterval));
        props.setProperty("pollutionMachineScanRadius", String.valueOf(pollutionMachineScanRadius));
        props.setProperty("basePollutionPerSecond", String.valueOf(basePollutionPerSecond));
        props.setProperty("pollutionTierExponent", String.valueOf(pollutionTierExponent));
        props.setProperty("multiblockPollutionMultiplier", String.valueOf(multiblockPollutionMultiplier));

        // Environment absorption
        props.setProperty("grassBlockAbsorption", String.valueOf(grassBlockAbsorption));
        props.setProperty("leavesAbsorption", String.valueOf(leavesAbsorption));
        props.setProperty("waterAbsorption", String.valueOf(waterAbsorption));
        props.setProperty("grassAbsorption", String.valueOf(grassAbsorption));
        props.setProperty("logAbsorption", String.valueOf(logAbsorption));
        props.setProperty("flowerAbsorption", String.valueOf(flowerAbsorption));
        props.setProperty("pollutionEnvAbsorptionMaxPercent", String.valueOf(pollutionEnvAbsorptionMaxPercent));
        props.setProperty("pollutionDiffusionRate", String.valueOf(pollutionDiffusionRate));
        props.setProperty("pollutionRemovalThreshold", String.valueOf(pollutionRemovalThreshold));
        props.setProperty("pollutionSporeFeedbackThreshold", String.valueOf(pollutionSporeFeedbackThreshold));

        // 性能和调试配置
        props.setProperty("threatMaxGlobalEntities", String.valueOf(threatMaxGlobalEntities));
        props.setProperty("debugLogInterval", String.valueOf(debugLogInterval));
        props.setProperty("threatSpawnRatePollutionDivisor", String.valueOf(threatSpawnRatePollutionDivisor));

        // XaerosWorldMap集成配置
        props.setProperty("enablePollutionMapOverlay", String.valueOf(enablePollutionMapOverlay));
        props.setProperty("pollutionOverlayAlpha", String.valueOf(pollutionOverlayAlpha));
        props.setProperty("showPollutionTooltip", String.valueOf(showPollutionTooltip));

        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                String comments = """
                        ================================================================================
                        Tri-Axis Difficulty System Configuration / 三轴难度系统配置
                        ================================================================================

                        [难度预设 Difficulty Presets] - 推荐使用预设，简单快捷
                        difficultyPreset: NORMAL, HARD, HARDCORE, INSANE, CUSTOM
                          - NORMAL (默认): 适合休闲玩家，难度适中，推荐新手使用
                          - HARD: 适合有经验的玩家，挑战性较高
                          - HARDCORE: 适合硬核玩家，高难度高挑战
                          - INSANE: 适合极限挑战，极高难度
                          - CUSTOM: 使用下方自定义配置，高级用户使用

                        [三轴权重 Tri-Axis Weights] - 控制三个难度轴的影响比例
                        weightTime: 时间因子权重 (0.0-1.0) - 游戏时间越长难度越高
                        weightVoltage: 机器电压权重 (0.0-1.0) - 科技等级越高难度越高
                        weightPollution: 污染权重 (0.0-1.0) - 污染越严重难度越高
                        注意：三个权重之和建议为1.0

                        [全局难度系数 Global Multipliers]
                        globalMultiplier: 全局难度倍率 (1.0-5.0推荐)
                          - 影响最终难度值，越高怪物越强
                        emaAlpha: 平滑系数 (0.01-0.2)
                          - 越小难度变化越平滑，越大响应越快
                        pollutionDenominator: 污染饱和阈值 (500-1500推荐)
                          - 越大需要更多污染才能达到高难度
                          - NORMAL: 1000, HARD: 900, HARDCORE: 800, INSANE: 600

                        [工业加成 Industrial Bonus] - 影响怪物属性的加成系数
                        techWeight: 科技加成权重 (10.0-50.0)
                          - 控制电压等级对怪物强度的影响
                        pollutionWeight: 污染加成权重 (5.0-30.0)
                          - 控制污染对怪物强度的影响
                        maxIndustrialTier: 游戏最大电压等级 (0=ULV, 9=UHV, 14=MAX)
                          - 推荐设置: 9 (UHV) - 适合大多数整合包
                          - 设置为你整合包的最高电压等级

                        [GT污染集成 GT Pollution Integration] - GregTech Modern污染系统集成
                        gtPollutionWeight: GT污染贡献权重 (0.0-2.0)
                          - 控制GT污染对临时污染的影响，默认0.8
                        gtSourceMultiplierBase: 污染源倍率基数 (0.1-1.0)
                          - 控制污染源数量对倍率的影响速度，默认0.5
                        gtSourceThreshold: 污染源倍率触发阈值 (5-20)
                          - 超过此数量的污染源开始应用倍率，默认10
                          - 倍率规则: 11-20源=1.5x, 21-30源=2.0x, 31-50源=3.0x, 51+源=4.0x
                        airScrubberEfficiency: 空气净化器效率 (0.5-2.0)
                          - 控制Air Scrubber清理污染的效率，默认1.0

                        [Hivemind接近系统 Hivemind Proximity System] - Spore虫巢加速系统
                        hivemindProximityRadius: 接近半径(区块) (4-16)
                          - 污染在此距离内接近Hivemind时触发加速，默认8区块(128方块)
                        hivemindAccelerationFactor: 最大加速倍率 (1.0-5.0)
                          - 控制Hivemind对难度增长的加速程度，默认2.0(最高2倍加速)
                          - 加速基于: 距离(越近越强) × 生物质(越多越强) × 污染(越高越强)
                        enableHivemindAcceleration: 启用Hivemind加速 (true/false)
                          - 是否启用污染接近虫巢时的难度加速，默认true

                        [Hordes集成 Hordes Integration] - The Hordes尸潮系统集成
                        enableHordeIntegration: 启用Hordes集成 (true/false)
                          - 是否启用尸潮系统与工业难度的联动，默认true
                        hordeIntensityMultiplier: 全局尸潮强度倍率 (0.5-3.0)
                          - 控制尸潮整体强度，默认1.0
                        difficultyToIntensityFactor: Difficulty转换系数 (0.1-2.0)
                          - 控制ImprovedMobs难度对尸潮强度的影响，默认0.5

                        enablePollutionTriggeredHordes: 启用污染触发尸潮 (true/false)
                          - 高污染区域自动触发尸潮，默认true
                        pollutionHordeTriggerThreshold: 污染触发阈值 (100-300)
                          - 区块污染超过此值可能触发尸潮，默认150
                        pollutionHordeCheckInterval: 检查间隔(tick) (300-1200)
                          - 多久检查一次污染触发，默认600(30秒)
                        pollutionHordeTriggerChance: 触发概率 (0.01-0.2)
                          - 每次检查的触发概率，默认0.05(5%)

                        enableSkirmishes: 启用小股袭扰 (true/false)
                          - 污染区域定期生成小股怪物，默认true
                        skirmishPollutionThreshold: 袭扰污染阈值 (50-150)
                          - 区块污染超过此值可能触发袭扰，默认80
                        skirmishInterval: 袭扰间隔(tick) (600-2400)
                          - 袭扰检查间隔，默认1200(1分钟)
                        skirmishMinCount: 袭扰最小数量 (1-10)
                          - 默认3
                        skirmishMaxCount: 袭扰最大数量 (5-20)
                          - 默认8

                        majorHordePollutionThreshold: 大尸潮污染阈值 (150-300)
                          - 污染超过此值触发大尸潮(2倍强度)，默认200
                        majorHordeMultiplier: 大尸潮强度倍率 (1.5-3.0)
                          - 大尸潮相对普通尸潮的强度，默认2.0

                        enableMachineTargeting: 启用机器攻击 (true/false)
                          - 尸潮僵尸攻击附近机器，默认true
                        machineTargetingRange: 机器检测范围(方块) (16-64)
                          - 僵尸检测机器的距离，默认32
                        machineTargetingChance: 机器攻击概率 (0.1-1.0)
                          - 僵尸攻击机器而非玩家的概率，默认0.3(30%)

                        enableVoltageTierScaling: 启用电压等级缩放 (true/false)
                          - 尸潮强度随玩家电压等级提升，默认true
                        hordeTierIntensityMultipliers: 电压等级强度倍率数组 (10个值，逗号分隔)
                          - 每个电压等级的尸潮强度倍率 (ULV到UHV)
                          - 默认: 1.0,1.1,1.3,1.5,1.8,2.2,2.6,3.0,3.5,4.0
                          - 对应: ULV=1.0x, LV=1.1x, MV=1.3x, HV=1.5x, EV=1.8x
                                 IV=2.2x, LuV=2.6x, ZPM=3.0x, UV=3.5x, UHV=4.0x

                        [时间曲线 Time Scaling] - 控制时间对难度的影响
                        targetDays: 达到最高时间难度的MC天数 (300-2000)
                          - NORMAL: 800天, HARD: 1000天, HARDCORE: 1200天, INSANE: 1500天
                        baseDays: 对数缩放基数 (10-50)
                          - 越小前期爬升越快，建议30

                        [属性增幅系数 Attribute Multipliers] - 怪物属性增强倍率
                        hpMultFactor: 血量增幅倍率 (0.5-5.0)
                        attackMultFactor: 伤害增幅倍率 (0.5-3.0)
                        speedMultFactor: 速度增幅倍率 (0.3-2.0)
                        armorMultFactor: 护甲增幅倍率 (0.5-2.0)

                        [属性上限 Attribute Caps] - 防止怪物属性无限增长
                        maxHpMultiplier: 最大血量倍率 (5.0-25.0)
                          - 默认15.0，僵尸最高300HP
                          - 防止后期怪物血量过高
                        maxAttackMultiplier: 最大伤害倍率 (3.0-20.0)
                          - 默认10.0，僵尸最高50伤害
                          - 防止后期怪物伤害过高
                        maxSpeedMultiplier: 最大速度倍率 (1.5-5.0)
                          - 默认3.0
                        maxArmorMultiplier: 最大护甲倍率 (2.0-10.0)
                          - 默认5.0

                        [污染衰减和吸收 Pollution Decay & Absorption]
                        naturalDecayRate: 自然衰减速率 (/秒/区块) (0.01-0.1)
                          - 默认0.03 (降低自0.05，让污染更容易积累)
                        grassBlockAbsorption: 草方块吸收速率 (/方块/秒) (0.0005-0.005)
                          - 默认0.0015，典型区块~200块 = 0.3/秒
                        leavesAbsorption: 树叶吸收速率 (/方块/秒) (0.001-0.01)
                          - 默认0.0025，典型区块~150块 = 0.375/秒
                        waterAbsorption: 水方块吸收速率 (/方块/秒) (0.0005-0.005)
                          - 默认0.0012，典型区块~50块 = 0.06/秒
                        grassAbsorption: 草/花吸收速率 (/方块/秒) (0.0005-0.005)
                          - 默认0.001
                        logAbsorption: 原木吸收速率 (/方块/秒) (0.0005-0.005)
                          - 默认0.001
                        flowerAbsorption: 花吸收速率 (/方块/秒) (0.0005-0.005)
                          - 默认0.0012

                        [污染转化 Pollution Conversion]
                        tempToPermanentThreshold: 临时转永久阈值 (100-300)
                          - 默认200，超过此值开始转化
                        tempToPermanentRate: 转化速率 (0.0001-0.01)
                          - 默认0.001 (0.1%/秒)
                        permanentToDifficultyRate: 永久污染转难度系数 (0.05-0.5)
                          - 默认0.1

                        [炮塔系统 Turret System]
                        flamethrowerDirectDamage: 火焰炮塔直击伤害 (2.0-10.0)
                          - 默认4.0 (降低自5.0，平衡防御强度)
                        flamethrowerGroundDamage: 火焰炮塔地面伤害 (1.0-5.0)
                          - 默认1.5 (降低自2.0)

                        [电压阶段血量目标 Voltage Tier HP Targets]
                        僵尸基础血量20，这些是目标倍率
                        ulvHpTarget: ULV阶段血量倍率 (推荐1.0 = 20血)
                        lvHpTarget: LV阶段血量倍率 (推荐1.3 = 26血)
                        mvHpTarget: MV阶段血量倍率 (推荐1.8 = 36血)
                        hvHpTarget: HV阶段血量倍率 (推荐2.5 = 50血)
                        evHpTarget: EV阶段血量倍率 (推荐3.5 = 70血)
                        ivHpTarget: IV阶段血量倍率 (推荐4.8 = 96血)
                        luvHpTarget: LuV阶段血量倍率 (推荐6.5 = 130血)
                        zpmHpTarget: ZPM阶段血量倍率 (推荐8.5 = 170血)
                        uvHpTarget: UV阶段血量倍率 (推荐11.0 = 220血)
                        uhvHpTarget: UHV阶段血量倍率 (推荐14.0 = 280血)

                        [扫描参数 Scanning Parameters]
                        scanRadiusBlocks: 扫描机器的半径范围 (32-128)
                          - 影响性能，建议64
                        maxGTTier: GT模组支持的最大等级 (通常14)
                        maxChangePerSec: 难度变化速率限制 (0.005-0.05)
                          - 防止难度突变

                        [调试选项 Debug Options] - 仅用于开发和调试
                        enableDebugLines: 启用调试线渲染 (默认false)
                          - 显示僵尸攻击机器的连线
                        enableDifficultyLogging: 启用难度日志输出 (默认false)
                          - 输出详细的难度计算日志
                        enableSporeDebug: 启用Spore集成调试信息 (默认false)
                          - 输出Spore mod集成的调试信息

                        注意：调试选项可以通过游戏内指令 /im industrial debug on 开启

                        [污染系统说明]
                        - 机器运行会产生污染，污染会吸引怪物攻击机器
                        - 树叶、草、花、水可以净化污染
                        - 污染 >= 50: 僵尸会攻击机器
                        - 污染 >= 80: Creeper会攻击机器
                        - 污染 >= 100: 开始生成污染Creeper
                        - GT污染源(Muffler)会增加临时污染，源越多倍率越高
                        - Air Scrubber可以清理污染
                        - 污染接近Spore Hivemind时会加速难度增长，提升怪物血量

                        [机器数量参考]
                        MV阶段: 20-30台机器
                        HV阶段: 40-60台机器
                        IV-LuV: 100+台机器
                        ZPM-UHV: 200+台机器
                        ================================================================================
                        """;
                props.store(out, comments);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if GregTech CEu is loaded.
     * <p>
     * This method attempts to load the GTCEu main class to determine if the mod is present.
     *
     * @return true if GregTech CEu is loaded
     */
    public static boolean hasGTCEu() {
        try {
            Class.forName("com.gregtechceu.gtceu.GTCEu");
            IndustrialLogger.info("[TriAxisConfig] hasGTCEu() -> TRUE");
            return true;
        } catch (ClassNotFoundException e) {
            IndustrialLogger.warn("[TriAxisConfig] hasGTCEu() -> FALSE: " + e.getMessage());
            return false;
        }
    }

    /**
     * Synchronize values from the new IICConfig system to this legacy config.
     * This method is called when the config is loaded or reloaded.
     *
     * @since 1.0.0
     */
    public static void syncFromConfig() {
        cn.minerealms.iic.core.config.IICConfig config = cn.minerealms.iic.core.config.IICConfig.INSTANCE;
        if (config == null) return;

        // Tri-Axis Weights
        weightTime = config.DIFFICULTY.weightTime.get();
        weightVoltage = config.DIFFICULTY.weightVoltage.get();
        weightPollution = config.DIFFICULTY.weightPollution.get();

        // Global Difficulty Multipliers
        globalMultiplier = config.DIFFICULTY.globalMultiplier.get();
        emaAlpha = config.DIFFICULTY.emaAlpha.get();
        pollutionDenominator = config.DIFFICULTY.pollutionDenominator.get();

        // Industrial Bonus Configuration
        techWeight = config.DIFFICULTY.techWeight.get();
        pollutionWeight = config.DIFFICULTY.pollutionWeight.get();
        maxIndustrialTier = config.DIFFICULTY.maxIndustrialTier.get();

        // Difficulty Contribution Weights
        playerBonusWeight = config.DIFFICULTY.playerBonusWeight.get();
        localPollutionWeight = config.DIFFICULTY.localPollutionWeight.get();
        globalPollutionWeight = config.DIFFICULTY.globalPollutionWeight.get();
        timeFactorWeight = config.DIFFICULTY.timeFactorWeight.get();

        // Time Scaling
        targetDays = config.DIFFICULTY.targetDays.get();
        baseDays = config.DIFFICULTY.baseDays.get();

        // Attribute Multipliers
        hpMultFactor = config.DIFFICULTY.hpMultFactor.get();
        attackMultFactor = config.DIFFICULTY.attackMultFactor.get();
        speedMultFactor = config.DIFFICULTY.speedMultFactor.get();
        armorMultFactor = config.DIFFICULTY.armorMultFactor.get();

        // Voltage Tier HP Targets
        ulvHpTarget = config.DIFFICULTY.ulvHpTarget.get();
        lvHpTarget = config.DIFFICULTY.lvHpTarget.get();
        mvHpTarget = config.DIFFICULTY.mvHpTarget.get();
        hvHpTarget = config.DIFFICULTY.hvHpTarget.get();
        evHpTarget = config.DIFFICULTY.evHpTarget.get();
        ivHpTarget = config.DIFFICULTY.ivHpTarget.get();
        luvHpTarget = config.DIFFICULTY.luvHpTarget.get();
        zpmHpTarget = config.DIFFICULTY.zpmHpTarget.get();
        uvHpTarget = config.DIFFICULTY.uvHpTarget.get();
        uhvHpTarget = config.DIFFICULTY.uhvHpTarget.get();

        // Scanning Parameters
        scanRadiusBlocks = config.DIFFICULTY.scanRadiusBlocks.get();
        maxGTTier = config.DIFFICULTY.maxGTTier.get();
        maxChangePerSec = config.DIFFICULTY.maxChangePerSec.get();

        // Pollution System - map to correct field names
        tempToPermanentThreshold = config.POLLUTION.conversionThreshold.get();
        tempToPermanentRate = config.POLLUTION.conversionRate.get();
        permanentToDifficultyRate = config.POLLUTION.difficultyMultiplier.get();

        // Threat System - map to correct field names
        mvZombieAttackThreshold = config.THREAT.mvZombieAttackThreshold.get();
        hvZombieSpawnThreshold = config.THREAT.hvZombieSpawnThreshold.get();
        hvCreeperSpawnThreshold = config.THREAT.hvCreeperSpawnThreshold.get();
        chargedCreeperThreshold = config.THREAT.chargedCreeperThreshold.get();
        zombieSpawnChance = config.THREAT.zombieSpawnChance.get();
        creeperSpawnChance = config.THREAT.creeperSpawnChance.get();
        chargedCreeperChance = config.THREAT.chargedCreeperChance.get();

        // Horde Integration
        enableHordeIntegration = config.HORDE.enableHordeIntegration.get();
        hordeIntensityMultiplier = config.HORDE.hordeIntensityMultiplier.get();
        difficultyToIntensityFactor = config.HORDE.difficultyToIntensityFactor.get();
        enablePollutionTriggeredHordes = config.HORDE.enablePollutionTriggeredHordes.get();
        pollutionHordeTriggerThreshold = config.HORDE.pollutionHordeTriggerThreshold.get();
        pollutionHordeCheckInterval = config.HORDE.pollutionHordeCheckInterval.get();
        pollutionHordeTriggerChance = config.HORDE.pollutionHordeTriggerChance.get();
        enableSkirmishes = config.HORDE.enableSkirmishes.get();
        skirmishPollutionThreshold = config.HORDE.skirmishPollutionThreshold.get();
        skirmishInterval = config.HORDE.skirmishInterval.get();
        skirmishMinCount = config.HORDE.skirmishMinCount.get();
        skirmishMaxCount = config.HORDE.skirmishMaxCount.get();
        majorHordePollutionThreshold = config.HORDE.majorHordePollutionThreshold.get();
        majorHordeMultiplier = config.HORDE.majorHordeMultiplier.get();
        enableMachineTargeting = config.HORDE.enableMachineTargeting.get();
        machineTargetingRange = config.HORDE.machineTargetingRange.get();
        machineTargetingChance = config.HORDE.machineTargetingChance.get();
        enableVoltageTierScaling = config.HORDE.enableVoltageTierScaling.get();

        // Integration
        gtPollutionWeight = config.INTEGRATION.gtPollutionWeight.get();
        gtSourceMultiplierBase = config.INTEGRATION.gtSourceMultiplierBase.get();
        gtSourceThreshold = config.INTEGRATION.gtSourceThreshold.get();
        airScrubberEfficiency = config.INTEGRATION.airScrubberEfficiency.get();
        enableHivemindAcceleration = config.INTEGRATION.enableHivemindAcceleration.get();
        hivemindProximityRadius = config.INTEGRATION.hivemindProximityRadius.get();
        hivemindAccelerationFactor = config.INTEGRATION.hivemindAccelerationFactor.get();

        // Debug Options
        enableDebugLines = config.DEBUG.enableDebugLines.get();
        enableDifficultyLogging = config.DEBUG.enableDifficultyLogging.get();
        enableSporeDebug = config.DEBUG.enableSporeDebug.get();
    }

    /**
     * Synchronize values from this legacy config to the new IICConfig system.
     * This method is called when values are changed via commands.
     */
    public static void syncToConfig() {
        cn.minerealms.iic.core.config.IICConfig config = cn.minerealms.iic.core.config.IICConfig.INSTANCE;
        if (config == null) return;

        // Tri-Axis Weights
        config.DIFFICULTY.weightTime.set(weightTime);
        config.DIFFICULTY.weightVoltage.set(weightVoltage);
        config.DIFFICULTY.weightPollution.set(weightPollution);

        // Global Difficulty Multipliers
        config.DIFFICULTY.globalMultiplier.set(globalMultiplier);
        config.DIFFICULTY.emaAlpha.set(emaAlpha);
        config.DIFFICULTY.pollutionDenominator.set(pollutionDenominator);

        // Industrial Bonus Configuration
        config.DIFFICULTY.techWeight.set(techWeight);
        config.DIFFICULTY.pollutionWeight.set(pollutionWeight);
        config.DIFFICULTY.maxIndustrialTier.set(maxIndustrialTier);

        // Difficulty Contribution Weights
        config.DIFFICULTY.playerBonusWeight.set(playerBonusWeight);
        config.DIFFICULTY.localPollutionWeight.set(localPollutionWeight);
        config.DIFFICULTY.globalPollutionWeight.set(globalPollutionWeight);
        config.DIFFICULTY.timeFactorWeight.set(timeFactorWeight);

        // Time Scaling
        config.DIFFICULTY.targetDays.set(targetDays);
        config.DIFFICULTY.baseDays.set(baseDays);

        // Attribute Multipliers
        config.DIFFICULTY.hpMultFactor.set(hpMultFactor);
        config.DIFFICULTY.attackMultFactor.set(attackMultFactor);
        config.DIFFICULTY.speedMultFactor.set(speedMultFactor);
        config.DIFFICULTY.armorMultFactor.set(armorMultFactor);

        // Voltage Tier HP Targets
        config.DIFFICULTY.ulvHpTarget.set(ulvHpTarget);
        config.DIFFICULTY.lvHpTarget.set(lvHpTarget);
        config.DIFFICULTY.mvHpTarget.set(mvHpTarget);
        config.DIFFICULTY.hvHpTarget.set(hvHpTarget);
        config.DIFFICULTY.evHpTarget.set(evHpTarget);
        config.DIFFICULTY.ivHpTarget.set(ivHpTarget);
        config.DIFFICULTY.luvHpTarget.set(luvHpTarget);
        config.DIFFICULTY.zpmHpTarget.set(zpmHpTarget);
        config.DIFFICULTY.uvHpTarget.set(uvHpTarget);
        config.DIFFICULTY.uhvHpTarget.set(uhvHpTarget);

        // Scanning Parameters
        config.DIFFICULTY.scanRadiusBlocks.set(scanRadiusBlocks);
        config.DIFFICULTY.maxGTTier.set(maxGTTier);
        config.DIFFICULTY.maxChangePerSec.set(maxChangePerSec);

        // Pollution System - map to correct field names
        config.POLLUTION.conversionThreshold.set(tempToPermanentThreshold);
        config.POLLUTION.conversionRate.set(tempToPermanentRate);
        config.POLLUTION.difficultyMultiplier.set(permanentToDifficultyRate);

        // Threat System - map to correct field names
        config.THREAT.mvZombieAttackThreshold.set(mvZombieAttackThreshold);
        config.THREAT.hvZombieSpawnThreshold.set(hvZombieSpawnThreshold);
        config.THREAT.hvCreeperSpawnThreshold.set(hvCreeperSpawnThreshold);
        config.THREAT.chargedCreeperThreshold.set(chargedCreeperThreshold);
        config.THREAT.zombieSpawnChance.set(zombieSpawnChance);
        config.THREAT.creeperSpawnChance.set(creeperSpawnChance);
        config.THREAT.chargedCreeperChance.set(chargedCreeperChance);

        // Horde Integration
        config.HORDE.enableHordeIntegration.set(enableHordeIntegration);
        config.HORDE.hordeIntensityMultiplier.set(hordeIntensityMultiplier);
        config.HORDE.difficultyToIntensityFactor.set(difficultyToIntensityFactor);
        config.HORDE.enablePollutionTriggeredHordes.set(enablePollutionTriggeredHordes);
        config.HORDE.pollutionHordeTriggerThreshold.set(pollutionHordeTriggerThreshold);
        config.HORDE.pollutionHordeCheckInterval.set(pollutionHordeCheckInterval);
        config.HORDE.pollutionHordeTriggerChance.set(pollutionHordeTriggerChance);
        config.HORDE.enableSkirmishes.set(enableSkirmishes);
        config.HORDE.skirmishPollutionThreshold.set(skirmishPollutionThreshold);
        config.HORDE.skirmishInterval.set(skirmishInterval);
        config.HORDE.skirmishMinCount.set(skirmishMinCount);
        config.HORDE.skirmishMaxCount.set(skirmishMaxCount);
        config.HORDE.majorHordePollutionThreshold.set(majorHordePollutionThreshold);
        config.HORDE.majorHordeMultiplier.set(majorHordeMultiplier);
        config.HORDE.enableMachineTargeting.set(enableMachineTargeting);
        config.HORDE.machineTargetingRange.set(machineTargetingRange);
        config.HORDE.machineTargetingChance.set(machineTargetingChance);
        config.HORDE.enableVoltageTierScaling.set(enableVoltageTierScaling);

        // Integration
        config.INTEGRATION.gtPollutionWeight.set(gtPollutionWeight);
        config.INTEGRATION.gtSourceMultiplierBase.set(gtSourceMultiplierBase);
        config.INTEGRATION.gtSourceThreshold.set(gtSourceThreshold);
        config.INTEGRATION.airScrubberEfficiency.set(airScrubberEfficiency);
        config.INTEGRATION.enableHivemindAcceleration.set(enableHivemindAcceleration);
        config.INTEGRATION.hivemindProximityRadius.set(hivemindProximityRadius);
        config.INTEGRATION.hivemindAccelerationFactor.set(hivemindAccelerationFactor);

        // Debug Options
        config.DEBUG.enableDebugLines.set(enableDebugLines);
        config.DEBUG.enableDifficultyLogging.set(enableDifficultyLogging);
        config.DEBUG.enableSporeDebug.set(enableSporeDebug);

        // Save the config
        cn.minerealms.iic.core.config.IICConfig.SPEC.save();
    }

    // ========================================
    // Helper Methods for Dynamic Configuration
    // ========================================

    private static Properties configProps = new Properties();

    /**
     * Gets a boolean value from configuration.
     *
     * @param key the configuration key
     * @param defaultValue the default value if key not found
     * @return the boolean value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        if (CONFIG_FILE.exists() && configProps.isEmpty()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                configProps.load(in);
            } catch (IOException e) {
                return defaultValue;
            }
        }
        return Boolean.parseBoolean(configProps.getProperty(key, String.valueOf(defaultValue)));
    }

    /**
     * Gets an integer value from configuration.
     *
     * @param key the configuration key
     * @param defaultValue the default value if key not found
     * @return the integer value
     */
    public static int getInt(String key, int defaultValue) {
        if (CONFIG_FILE.exists() && configProps.isEmpty()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                configProps.load(in);
            } catch (IOException e) {
                return defaultValue;
            }
        }
        try {
            return Integer.parseInt(configProps.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a double value from configuration.
     *
     * @param key the configuration key
     * @param defaultValue the default value if key not found
     * @return the double value
     */
    public static double getDouble(String key, double defaultValue) {
        if (CONFIG_FILE.exists() && configProps.isEmpty()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                configProps.load(in);
            } catch (IOException e) {
                return defaultValue;
            }
        }
        try {
            return Double.parseDouble(configProps.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a boolean value in configuration.
     *
     * @param key the configuration key
     * @param value the boolean value
     */
    public static void setBoolean(String key, boolean value) {
        configProps.setProperty(key, String.valueOf(value));
    }

    /**
     * Sets an integer value in configuration.
     *
     * @param key the configuration key
     * @param value the integer value
     */
    public static void setInt(String key, int value) {
        configProps.setProperty(key, String.valueOf(value));
    }

    /**
     * Sets a double value in configuration.
     *
     * @param key the configuration key
     * @param value the double value
     */
    public static void setDouble(String key, double value) {
        configProps.setProperty(key, String.valueOf(value));
    }
}
