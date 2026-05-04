package cn.minerealms.iic.integration.gunmod;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Dummy class to allow compilation when Gun Mod is not present.
 * The actual mixin will target com.mrcrayfish.guns.common.network.ServerPlayHandler at runtime.
 */
public class DummyServerPlayHandler {
    public static void handleCraft(ServerPlayer player, ResourceLocation id, BlockPos pos) {
        // Dummy implementation
    }
}
