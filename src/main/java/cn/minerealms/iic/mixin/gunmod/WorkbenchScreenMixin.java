package cn.minerealms.iic.mixin.gunmod;

import cn.minerealms.iic.integration.gunmod.DummyWorkbenchScreen;
import cn.minerealms.iic.integration.gunmod.IEnergyWorkbench;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add progress bar and energy display to Gun Mod's WorkbenchScreen.
 * Renders progress bar and energy status in the GUI.
 */
@Mixin(value = DummyWorkbenchScreen.class, remap = false)
public abstract class WorkbenchScreenMixin extends AbstractContainerScreen {

    @Shadow
    private Object workbench; // WorkbenchBlockEntity

    @Unique
    private static final ResourceLocation GUI_BASE = new ResourceLocation("cgm:textures/gui/workbench.png");

    public WorkbenchScreenMixin(Object menu, Inventory playerInventory, Component title) {
        super((net.minecraft.world.inventory.AbstractContainerMenu) menu, playerInventory, title);
    }

    /**
     * Inject after the background is rendered to add progress bar and energy display.
     */
    @Inject(method = "renderBg", at = @At("TAIL"), remap = false)
    private void iic$renderProgressAndEnergy(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (!(this.workbench instanceof IEnergyWorkbench energyWorkbench)) {
            return;
        }

        int startX = this.leftPos;
        int startY = this.topPos;

        // ==================== Energy Bar ====================
        int energyBarX = startX + 8;
        int energyBarY = startY + 90;
        int energyBarWidth = 160;
        int energyBarHeight = 6;

        // Draw energy bar background (dark gray)
        graphics.fill(energyBarX, energyBarY, energyBarX + energyBarWidth, energyBarY + energyBarHeight, 0xFF3F3F3F);

        // Draw energy bar fill (cyan)
        int storedEnergy = energyWorkbench.iic$getStoredEnergy();
        int maxEnergy = energyWorkbench.iic$getMaxEnergy();
        if (maxEnergy > 0) {
            int fillWidth = (int) ((float) storedEnergy / maxEnergy * energyBarWidth);
            graphics.fill(energyBarX, energyBarY, energyBarX + fillWidth, energyBarY + energyBarHeight, 0xFF00FFFF);
        }

        // Draw energy bar border (white)
        graphics.fill(energyBarX, energyBarY, energyBarX + energyBarWidth, energyBarY + 1, 0xFFFFFFFF); // Top
        graphics.fill(energyBarX, energyBarY + energyBarHeight - 1, energyBarX + energyBarWidth, energyBarY + energyBarHeight, 0xFFFFFFFF); // Bottom
        graphics.fill(energyBarX, energyBarY, energyBarX + 1, energyBarY + energyBarHeight, 0xFFFFFFFF); // Left
        graphics.fill(energyBarX + energyBarWidth - 1, energyBarY, energyBarX + energyBarWidth, energyBarY + energyBarHeight, 0xFFFFFFFF); // Right

        // Draw energy text
        String energyText = String.format("Energy: %d / %d EU", storedEnergy, maxEnergy);
        graphics.drawString(this.font, energyText, energyBarX + 2, energyBarY - 10, 0xFFFFFF, false);

        // ==================== Progress Bar ====================
        if (energyWorkbench.iic$isCrafting()) {
            int progressBarX = startX + 8;
            int progressBarY = startY + 102;
            int progressBarWidth = 160;
            int progressBarHeight = 8;

            // Draw progress bar background (dark gray)
            graphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, 0xFF3F3F3F);

            // Draw progress bar fill (green)
            int progress = energyWorkbench.iic$getProgress();
            int maxProgress = energyWorkbench.iic$getMaxProgress();
            if (maxProgress > 0) {
                int fillWidth = (int) ((float) progress / maxProgress * progressBarWidth);
                graphics.fill(progressBarX, progressBarY, progressBarX + fillWidth, progressBarY + progressBarHeight, 0xFF00FF00);
            }

            // Draw progress bar border (white)
            graphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + 1, 0xFFFFFFFF); // Top
            graphics.fill(progressBarX, progressBarY + progressBarHeight - 1, progressBarX + progressBarWidth, progressBarY + progressBarHeight, 0xFFFFFFFF); // Bottom
            graphics.fill(progressBarX, progressBarY, progressBarX + 1, progressBarY + progressBarHeight, 0xFFFFFFFF); // Left
            graphics.fill(progressBarX + progressBarWidth - 1, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, 0xFFFFFFFF); // Right

            // Draw progress text
            int percentage = maxProgress > 0 ? (progress * 100 / maxProgress) : 0;
            String progressText = String.format("Crafting: %d%%", percentage);
            graphics.drawString(this.font, progressText, progressBarX + 2, progressBarY - 10, 0xFFFFFF, false);

            // Draw time remaining
            int ticksRemaining = maxProgress - progress;
            float secondsRemaining = ticksRemaining / 20.0f;
            String timeText = String.format("Time: %.1fs", secondsRemaining);
            graphics.drawString(this.font, timeText, progressBarX + progressBarWidth - this.font.width(timeText) - 2, progressBarY - 10, 0xFFFFFF, false);
        }
    }
}
