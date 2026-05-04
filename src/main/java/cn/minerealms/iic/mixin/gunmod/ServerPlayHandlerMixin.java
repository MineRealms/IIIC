package cn.minerealms.iic.mixin.gunmod;

import cn.minerealms.iic.integration.gunmod.DummyServerPlayHandler;
import cn.minerealms.iic.integration.gunmod.IEnergyWorkbench;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to modify Gun Mod's ServerPlayHandler to use progress-based crafting.
 * Intercepts the handleCraft method to start energy-consuming crafting instead of instant crafting.
 */
@Mixin(value = DummyServerPlayHandler.class, remap = false)
public class ServerPlayHandlerMixin {

    /**
     * Intercept the handleCraft method to implement progress-based crafting.
     * This replaces the instant crafting with a system that:
     * 1. Checks if workbench has enough energy
     * 2. Starts crafting progress
     * 3. Consumes energy over time via tick()
     * 4. Drops item when progress completes
     */
    @Inject(
        method = "handleCraft(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/core/BlockPos;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void iic$onHandleCraft(ServerPlayer player, ResourceLocation id, BlockPos pos, CallbackInfo ci) {
        try {
            Level world = player.level();

            // Get the workbench container
            Object containerMenu = player.containerMenu;
            if (containerMenu == null) {
                return;
            }

            // Use reflection to check if it's a WorkbenchContainer
            Class<?> workbenchContainerClass = Class.forName("com.mrcrayfish.guns.common.container.WorkbenchContainer");
            if (!workbenchContainerClass.isInstance(containerMenu)) {
                return;
            }

            // Get the workbench position
            BlockPos workbenchPos = (BlockPos) workbenchContainerClass.getMethod("getPos").invoke(containerMenu);
            if (!workbenchPos.equals(pos)) {
                return;
            }

            // Get the recipe
            Class<?> workbenchRecipesClass = Class.forName("com.mrcrayfish.guns.crafting.WorkbenchRecipes");
            Object recipe = workbenchRecipesClass.getMethod("getRecipeById", Level.class, ResourceLocation.class)
                .invoke(null, world, id);

            if (recipe == null) {
                return;
            }

            // Check if player has materials
            boolean hasMaterials = (boolean) recipe.getClass().getMethod("hasMaterials", ServerPlayer.class)
                .invoke(recipe, player);
            if (!hasMaterials) {
                return;
            }

            // Get the workbench block entity
            Object workbenchBlockEntity = workbenchContainerClass.getMethod("getWorkbench").invoke(containerMenu);
            if (!(workbenchBlockEntity instanceof IEnergyWorkbench energyWorkbench)) {
                // Fallback to original behavior if mixin not applied
                return;
            }

            // Check if already crafting
            if (energyWorkbench.iic$isCrafting()) {
                ci.cancel();
                return;
            }

            // Determine recipe type and costs
            ItemStack resultStack = (ItemStack) recipe.getClass().getMethod("getItem").invoke(recipe);
            String itemId = resultStack.getItem().toString().toLowerCase();

            // Check if it's ammo or weapon
            boolean isAmmo = itemId.contains("ammo") || itemId.contains("shell") ||
                           itemId.contains("round") || itemId.contains("bullet");

            // Get costs from config
            int maxProgress = cn.minerealms.iic.integration.gunmod.GunModRecipeConfig.getCraftingTime(id, isAmmo);
            int recipeCost = cn.minerealms.iic.integration.gunmod.GunModRecipeConfig.getEnergyCost(id, isAmmo);

            // Try to start crafting
            if (!energyWorkbench.iic$startCrafting(maxProgress, recipeCost)) {
                // Not enough energy - send message to player
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cNot enough energy! Need " + recipeCost + " EU"),
                    true
                );
                ci.cancel();
                return;
            }

            // Consume materials
            recipe.getClass().getMethod("consumeMaterials", ServerPlayer.class).invoke(recipe, player);

            // Store recipe data in block entity for completion
            // We'll use a custom NBT tag that gets saved automatically
            if (workbenchBlockEntity instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                try {
                    // Use reflection to access and modify the NBT
                    java.lang.reflect.Method saveMethod = be.getClass().getMethod("saveWithoutMetadata");
                    net.minecraft.nbt.CompoundTag beTag = (net.minecraft.nbt.CompoundTag) saveMethod.invoke(be);

                    beTag.putString("IICPendingRecipeId", id.toString());
                    net.minecraft.nbt.CompoundTag resultTag = new net.minecraft.nbt.CompoundTag();
                    resultStack.save(resultTag);
                    beTag.put("IICPendingResult", resultTag);

                    // Mark as changed to trigger save
                    be.setChanged();
                } catch (Exception e) {
                    System.err.println("[IIC] Failed to store recipe data: " + e.getMessage());
                }
            }

            // Cancel original method
            ci.cancel();

        } catch (Exception e) {
            // If anything fails, let original method run
            System.err.println("[IIC] Error in workbench craft handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
