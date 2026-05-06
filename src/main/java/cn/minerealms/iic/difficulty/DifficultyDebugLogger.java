package cn.minerealms.iic.difficulty;

import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.pollution.PollutionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class DifficultyDebugLogger {

    private static final String LOG_FILE = "logs/difficulty-debug.csv";
    private static final long LOG_INTERVAL_TICKS = 120 * 20;
    private static long lastLogTick = -1;
    private static boolean headerWritten = false;
    private static long logCounter = 0;

    public static void tick(ServerLevel level, ChunkPos chunkPos) {
        long currentTick = level.getGameTime();

        if (currentTick - lastLogTick < LOG_INTERVAL_TICKS) {
            return;
        }
        lastLogTick = currentTick;

        try {
            // Convert ChunkPos to BlockPos (center of chunk)
            BlockPos center = new BlockPos(chunkPos.getMinBlockX(), 64, chunkPos.getMinBlockZ());
            TriAxisDifficultyManager.DifficultyState state = TriAxisDifficultyManager.calculateLocalDifficulty(level, center);
            writeLog(level, chunkPos, state);
        } catch (Exception e) {
            IndustrialLogger.error("[DifficultyDebugLogger] Failed to write log", e);
        }
    }

    private static void writeLog(ServerLevel level, ChunkPos chunkPos, TriAxisDifficultyManager.DifficultyState state) throws IOException {
        long mcDays = level.getDayTime() / 24000L;
        long gameTime = level.getGameTime();
        double realTimeMinutes = gameTime / 1200.0;

        if (!headerWritten) {
            writeHeader();
            headerWritten = true;
        }

        double mcDaysPerRealDay = 24.0 * 3.0;
        double targetMCDays = TriAxisConfig.realWorldDaysToMax * mcDaysPerRealDay;
        double tRaw = Math.log1p(mcDays / TriAxisConfig.baseDays) / Math.log1p(targetMCDays / TriAxisConfig.baseDays);
        double T = Mth.clamp(tRaw, 0.0, 1.0);
        double Base = TriAxisConfig.baseMin + (TriAxisConfig.baseMax - TriAxisConfig.baseMin) * T;

        double V = state.voltageFactor;
        double tierValue = V * TriAxisConfig.getEffectiveMaxTier();
        double Scale = 1.0 + Math.pow(tierValue, TriAxisConfig.scaleExponent) * TriAxisConfig.scaleMultiplier;

        double P = state.pollutionFactor;
        double Pressure;
        double emaPollution = P * TriAxisConfig.pollutionDenominator;
        if (emaPollution <= 0.0) {
            Pressure = 1.0;
        } else {
            double sigmoid = 1.0 / (1.0 + Math.exp(-P + TriAxisConfig.sigmoidShift));
            Pressure = TriAxisConfig.pressureMin + (TriAxisConfig.pressureMax - TriAxisConfig.pressureMin) * sigmoid;
        }

        double rawDifficulty = Base * Scale * Pressure * TriAxisConfig.globalMultiplier;

        // FIXED: Use V=1.0 (max theoretical voltage) to match TriAxisDifficultyManager's max calculation
        double maxTierValue = 1.0 * TriAxisConfig.getEffectiveMaxTier();
        double maxScale = 1.0 + Math.pow(maxTierValue, TriAxisConfig.scaleExponent) * TriAxisConfig.scaleMultiplier;
        double theoreticalMax = TriAxisConfig.baseMax * maxScale * TriAxisConfig.pressureMax * TriAxisConfig.globalMultiplier;
        double scaledDifficulty = rawDifficulty / theoreticalMax * TriAxisConfig.targetMaxDifficulty;

        // MCDay tracking
        long currentMcDayTick = (level.getDayTime() / 24000L) * 24000L;

        double localPollution = PollutionManager.getTemporaryPollution(chunkPos);
        double globalPollution = PollutionManager.getPermanentPollution();

        double cachedDifficulty = TriAxisDifficultyManager.difficultyCache.getOrDefault(chunkPos, 0.0);

        logCounter++;

        StringBuilder sb = new StringBuilder();
        sb.append(logCounter).append(",");
        sb.append(String.format("%.2f,", realTimeMinutes));
        sb.append(gameTime).append(",");
        sb.append(mcDays).append(",");
        sb.append(chunkPos.x).append(",");
        sb.append(chunkPos.z).append(",");
        sb.append(String.format("%.6f,%.6f,", T, Base));
        sb.append(String.format("%.6f,%.6f,", V, Scale));
        sb.append(String.format("%.6f,%.6f,%.6f,", P, emaPollution, Pressure));
        sb.append(String.format("%.6f,%.6f,%.6f,%.6f,%.6f,", 
            TriAxisConfig.globalMultiplier, rawDifficulty, theoreticalMax, cachedDifficulty, scaledDifficulty));
        sb.append(String.format("%.4f,%.4f,", localPollution, globalPollution));
        sb.append(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.4f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s,", 
            TriAxisConfig.realWorldDaysToMax, TriAxisConfig.baseDays, TriAxisConfig.baseMin, TriAxisConfig.baseMax,
            TriAxisConfig.scaleExponent, TriAxisConfig.scaleMultiplier, TriAxisConfig.pressureMin, TriAxisConfig.pressureMax,
            TriAxisConfig.sigmoidShift, TriAxisConfig.pollutionDenominator, TriAxisConfig.targetMaxDifficulty,
            TriAxisConfig.maxChangePerSec, TriAxisConfig.hasGTCEu()));
        sb.append(String.format("%.4f,%.4f,%.4f,", state.timeFactor, state.voltageFactor, state.pollutionFactor));
        sb.append(String.format("%d,%d", 
            TriAxisDifficultyManager.getLastDifficultyUpdateTick(chunkPos), currentMcDayTick));
        sb.append("\n");

        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.print(sb.toString());
        }

        IndustrialLogger.info(String.format("[DifficultyDebugLogger] Logged: raw=%.4f, scaled=%.4f, T=%.4f, V=%.4f, P=%.4f",
            rawDifficulty, scaledDifficulty, T, V, P));
    }

    private static void writeHeader() throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("LogIndex,RealTimeMin,GameTick,MCDays,ChunkX,ChunkZ,TimeFactorT,Base,VoltageFactorV,Scale,");
        header.append("PollutionFactorP,EMAPollution,PressureMultiplier,GlobalMultiplier,RawDifficulty,TheoreticalMax,CachedDifficulty,ScaledDifficulty,");
        header.append("LocalPollution,GlobalPollution,Config_realWorldDaysToMax,Config_baseDays,Config_baseMin,Config_baseMax,");
        header.append("Config_scaleExponent,Config_scaleMultiplier,Config_pressureMin,Config_pressureMax,Config_sigmoidShift,");
        header.append("Config_pollutionDenominator,Config_targetMaxDifficulty,Config_maxChangePerSec,Config_hasGTCEu,");
        header.append("State_timeFactor,State_voltageFactor,State_pollutionFactor,LastMCDayUpdateTick,MCDayTick\n");

        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(LOG_FILE, false);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.print(header.toString());
        }

        IndustrialLogger.info("[DifficultyDebugLogger] Started debug log: " + LOG_FILE);
    }
}