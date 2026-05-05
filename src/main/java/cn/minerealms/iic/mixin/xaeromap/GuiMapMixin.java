package cn.minerealms.iic.mixin.xaeromap;

import cn.minerealms.iic.api.PollutionOverlayAPI;
import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin for XaerosWorldMap's GuiMap to add pollution overlay functionality.
 * <p>
 * This mixin renders pollution overlay directly without using buttons,
 * to avoid importing XaerosWorldMap classes that would cause early loading.
 *
 * @author ImprovedMobs Industrial Integration
 */
@OnlyIn(Dist.CLIENT)
@Mixin(value = xaero.map.gui.GuiMap.class, remap = false, priority = 2000)
public abstract class GuiMapMixin {

    @Shadow
    public double cameraX;

    @Shadow
    public double cameraZ;

    @Shadow
    public double scale;

    @Shadow(remap = true)
    public int width;

    @Shadow(remap = true)
    public int height;

    @Shadow
    public abstract Minecraft getMinecraft();

    @Unique
    private ResourceKey<Level> iic$lastDimension = null;

    /**
     * Inject into render method to check dimension changes.
     */
    @Inject(method = "m_88315_", at = @At("HEAD"), remap = false)
    private void iic$checkDimensionChange(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!TriAxisConfig.enablePollutionMapOverlay) {
            return;
        }

        Minecraft mc = getMinecraft();
        if (mc.level != null) {
            ResourceKey<Level> currentDim = mc.level.dimension();

            if (iic$lastDimension != null && !iic$lastDimension.equals(currentDim)) {
                PollutionOverlayAPI.clearCache();
            }

            iic$lastDimension = currentDim;
        }
    }

    /**
     * Inject into render method to draw pollution overlay.
     */
    @Inject(method = "m_88315_", at = @At("TAIL"), remap = false)
    private void iic$renderPollutionOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!TriAxisConfig.enablePollutionMapOverlay) {
            return;
        }

        // Render pollution overlay directly
        iic$renderPollution(guiGraphics);

        // Render hover tooltip
        if (TriAxisConfig.showPollutionTooltip) {
            double pollution = iic$getPollutionAtMouse(mouseX, mouseY);
            if (pollution > 0.1) {
                String tooltipText = String.format("§6Pollution: §c%.1f", pollution);
                guiGraphics.drawString(
                        getMinecraft().font,
                        tooltipText,
                        mouseX + 10,
                        mouseY - 10,
                        0xFFFFFF
                );
            }
        }
    }

    @Unique
    private void iic$renderPollution(GuiGraphics guiGraphics) {
        Map<ChunkPos, Double> pollutedChunks = PollutionOverlayAPI.getAllPollutedChunks();
        if (pollutedChunks.isEmpty()) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<ChunkPos, Double> entry : pollutedChunks.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            double pollution = entry.getValue();

            int chunkWorldX = chunkPos.x * 16;
            int chunkWorldZ = chunkPos.z * 16;

            double screenX = (chunkWorldX - cameraX) * scale + width / 2.0;
            double screenZ = (chunkWorldZ - cameraZ) * scale + height / 2.0;
            double chunkSize = 16 * scale;

            if (screenX + chunkSize < 0 || screenX > width ||
                screenZ + chunkSize < 0 || screenZ > height) {
                continue;
            }

            float[] color = iic$getPollutionColor(pollution);
            float r = color[0];
            float g = color[1];
            float b = color[2];
            float a = color[3] * (float) TriAxisConfig.pollutionOverlayAlpha;

            bufferBuilder.vertex(matrix, (float)screenX, (float)(screenZ + chunkSize), 0)
                    .color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, (float)(screenX + chunkSize), (float)(screenZ + chunkSize), 0)
                    .color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, (float)(screenX + chunkSize), (float)screenZ, 0)
                    .color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, (float)screenX, (float)screenZ, 0)
                    .color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    @Unique
    private float[] iic$getPollutionColor(double pollution) {
        float alpha = 1.0f;

        if (pollution < 50) {
            float t = (float)(pollution / 50.0);
            return new float[]{t, 1.0f, 0.0f, alpha};
        } else if (pollution < 100) {
            float t = (float)((pollution - 50) / 50.0);
            return new float[]{1.0f, 1.0f - t * 0.5f, 0.0f, alpha};
        } else if (pollution < 200) {
            float t = (float)((pollution - 100) / 100.0);
            return new float[]{1.0f, 0.5f - t * 0.5f, 0.0f, alpha};
        } else {
            return new float[]{0.8f, 0.0f, 0.0f, alpha};
        }
    }

    @Unique
    private double iic$getPollutionAtMouse(int mouseX, int mouseY) {
        double worldX = (mouseX - width / 2.0) / scale + cameraX;
        double worldZ = (mouseY - height / 2.0) / scale + cameraZ;

        ChunkPos chunkPos = new ChunkPos((int)Math.floor(worldX / 16), (int)Math.floor(worldZ / 16));

        return PollutionOverlayAPI.getPollutionForChunk(chunkPos);
    }
}
