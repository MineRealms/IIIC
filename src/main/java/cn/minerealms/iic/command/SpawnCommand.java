package cn.minerealms.iic.command;

import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.spawn.MobSpawnManager;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Map;

/**
 * Commands for controlling and debugging mob spawning.
 * <p>
 * Commands:
 * - /im spawn status - Show spawn statistics
 * - /im spawn debug <on|off> - Toggle spawn debug mode
 * - /im spawn reset - Reset spawn statistics
 * - /im spawn multiplier <value> - Set spawn multiplier
 * - /im spawn bonus <value> - Set difficulty spawn bonus
 * - /im spawn test <count> <type> [pos] - Force spawn mobs for testing
 * - /im spawn enable <true|false> - Enable/disable spawn enhancement
 */
public class SpawnCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("spawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(SpawnCommand::showStatus))
                .then(Commands.literal("debug")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(SpawnCommand::setDebugMode)))
                .then(Commands.literal("reset")
                        .executes(SpawnCommand::resetStatistics))
                .then(Commands.literal("multiplier")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10.0))
                                .executes(SpawnCommand::setSpawnMultiplier)))
                .then(Commands.literal("bonus")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(SpawnCommand::setDifficultyBonus)))
                .then(Commands.literal("test")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                .then(Commands.argument("type", EntityArgument.entity())
                                        .executes(SpawnCommand::testSpawnAtPlayer)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(SpawnCommand::testSpawnAtPos)))))
                .then(Commands.literal("enable")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(SpawnCommand::setSpawnEnhancement)));
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Get statistics
        int total = MobSpawnManager.getTotalSpawns();
        int allowed = MobSpawnManager.getAllowedSpawns();
        int denied = MobSpawnManager.getDeniedSpawns();
        long timeSinceReset = MobSpawnManager.getTimeSinceReset();
        boolean debugMode = MobSpawnManager.isDebugMode();

        // Calculate rates
        double allowRate = total > 0 ? (allowed * 100.0 / total) : 0.0;
        double denyRate = total > 0 ? (denied * 100.0 / total) : 0.0;
        double spawnsPerSecond = timeSinceReset > 0 ? (total / (double) timeSinceReset) : 0.0;

        // Send header
        source.sendSuccess(() -> Component.literal("=== Mob Spawn Status ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Configuration
        source.sendSuccess(() -> Component.literal(String.format("Enhancement: %s",
                TriAxisConfig.enableSpawnEnhancement ? "ENABLED" : "DISABLED"))
                .withStyle(TriAxisConfig.enableSpawnEnhancement ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        source.sendSuccess(() -> Component.literal(String.format("Spawn Multiplier: %.2f",
                TriAxisConfig.spawnMultiplier))
                .withStyle(ChatFormatting.YELLOW), false);

        source.sendSuccess(() -> Component.literal(String.format("Difficulty Bonus: %.4f per difficulty point",
                TriAxisConfig.difficultySpawnBonus))
                .withStyle(ChatFormatting.YELLOW), false);

        source.sendSuccess(() -> Component.literal(String.format("Max Spawn Chance: %.2f",
                TriAxisConfig.maxSpawnChance))
                .withStyle(ChatFormatting.YELLOW), false);

        source.sendSuccess(() -> Component.literal(String.format("Debug Mode: %s",
                debugMode ? "ON" : "OFF"))
                .withStyle(debugMode ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);

        // Statistics
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("Statistics (last " + timeSinceReset + "s):")
                .withStyle(ChatFormatting.AQUA), false);

        source.sendSuccess(() -> Component.literal(String.format("  Total Attempts: %d (%.2f/s)",
                total, spawnsPerSecond))
                .withStyle(ChatFormatting.WHITE), false);

        source.sendSuccess(() -> Component.literal(String.format("  Allowed: %d (%.1f%%)",
                allowed, allowRate))
                .withStyle(ChatFormatting.GREEN), false);

        source.sendSuccess(() -> Component.literal(String.format("  Denied: %d (%.1f%%)",
                denied, denyRate))
                .withStyle(ChatFormatting.RED), false);

        // Top spawned types
        Map<EntityType<?>, Integer> spawnCounts = MobSpawnManager.getSpawnCountsByType();
        if (!spawnCounts.isEmpty()) {
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("Top Spawned Types:")
                    .withStyle(ChatFormatting.AQUA), false);

            spawnCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        String name = entry.getKey().getDescription().getString();
                        int count = entry.getValue();
                        source.sendSuccess(() -> Component.literal(String.format("  %s: %d", name, count))
                                .withStyle(ChatFormatting.WHITE), false);
                    });
        }

        return 1;
    }

    private static int setDebugMode(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        MobSpawnManager.setDebugMode(enabled);

        context.getSource().sendSuccess(() -> Component.literal(
                "Spawn debug mode: " + (enabled ? "ON" : "OFF"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);

        return 1;
    }

    private static int resetStatistics(CommandContext<CommandSourceStack> context) {
        MobSpawnManager.resetStatistics();

        context.getSource().sendSuccess(() -> Component.literal("Spawn statistics reset")
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setSpawnMultiplier(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        TriAxisConfig.spawnMultiplier = value;
        TriAxisConfig.save();

        context.getSource().sendSuccess(() -> Component.literal(
                String.format("Spawn multiplier set to: %.2f", value))
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setDifficultyBonus(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        TriAxisConfig.difficultySpawnBonus = value;
        TriAxisConfig.save();

        context.getSource().sendSuccess(() -> Component.literal(
                String.format("Difficulty spawn bonus set to: %.4f", value))
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int testSpawnAtPlayer(CommandContext<CommandSourceStack> context) {
        try {
            int count = IntegerArgumentType.getInteger(context, "count");
            var entityArg = EntityArgument.getEntity(context, "type");

            // Get entity type from argument
            EntityType<?> entityType = entityArg.getType();

            if (!(entityType.create(context.getSource().getLevel()) instanceof Mob)) {
                context.getSource().sendFailure(Component.literal("Entity type must be a mob")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            @SuppressWarnings("unchecked")
            EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) entityType;

            ServerLevel level = context.getSource().getLevel();
            BlockPos pos = BlockPos.containing(context.getSource().getPosition());

            int spawned = MobSpawnManager.forceSpawnMobs(level, pos, count, mobType);

            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Spawned %d/%d %s at player position",
                            spawned, count, mobType.getDescription().getString()))
                    .withStyle(ChatFormatting.GREEN), true);

            return spawned;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int testSpawnAtPos(CommandContext<CommandSourceStack> context) {
        try {
            int count = IntegerArgumentType.getInteger(context, "count");
            var entityArg = EntityArgument.getEntity(context, "type");
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            EntityType<?> entityType = entityArg.getType();

            if (!(entityType.create(context.getSource().getLevel()) instanceof Mob)) {
                context.getSource().sendFailure(Component.literal("Entity type must be a mob")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            @SuppressWarnings("unchecked")
            EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) entityType;

            ServerLevel level = context.getSource().getLevel();
            int spawned = MobSpawnManager.forceSpawnMobs(level, pos, count, mobType);

            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Spawned %d/%d %s at %s",
                            spawned, count, mobType.getDescription().getString(), pos))
                    .withStyle(ChatFormatting.GREEN), true);

            return spawned;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int setSpawnEnhancement(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        TriAxisConfig.enableSpawnEnhancement = enabled;
        TriAxisConfig.save();

        context.getSource().sendSuccess(() -> Component.literal(
                "Spawn enhancement: " + (enabled ? "ENABLED" : "DISABLED"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);

        return 1;
    }
}
