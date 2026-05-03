package cn.minerealms.iic.forge;

import cn.minerealms.iic.command.IICCommand;
import cn.minerealms.iic.difficulty.DifficultyManager;
import cn.minerealms.iic.integration.alexscaves.AlexsCavesIntegration;
import cn.minerealms.iic.threat.horde.HordeIntegrationManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import cn.minerealms.iic.IntegratedIndustrialCraft;

@Mod.EventBusSubscriber(modid = IntegratedIndustrialCraft.MODID)
public class EventHandler {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Initialize systems
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Register unified command system
        IICCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Tick for each player
            event.getServer().getPlayerList().getPlayers().forEach(player -> {
                DifficultyManager.tick(player);
            });

            // Tick Pollution system only for dimensions with players
            // Avoid scanning empty dimensions (performance optimization)
            event.getServer().getAllLevels().forEach(level -> {
                // Only tick pollution for dimensions that have players
                boolean hasPlayers = !level.players().isEmpty();
                if (hasPlayers) {
                    cn.minerealms.iic.pollution.PollutionManager.tick(level);
                }
                HordeIntegrationManager.tick(level);
                AlexsCavesIntegration.tick(level);
            });
        }
    }
}
