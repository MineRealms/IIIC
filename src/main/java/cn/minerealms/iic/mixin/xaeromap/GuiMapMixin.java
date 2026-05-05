package cn.minerealms.iic.mixin.xaeromap;

import cn.minerealms.iic.api.PollutionOverlayAPI;
import cn.minerealms.iic.industrial.TriAxisConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mixin(value = xaero.map.gui.GuiMap.class, remap = false, priority = 2000)
public abstract class GuiMapMixin {

    @Unique
    private static Minecraft iic$getMinecraft() {
        return Minecraft.getInstance();
    }

    @Unique
    private static int iic$getWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    @Unique
    private static int iic$getHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    @Unique
    private static int iic$getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    @Unique
    private static int iic$getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    @Unique
    private ResourceKey<Level> iic$lastDimension = null;

    @Inject(method = "m_88315_", at = @At("HEAD"), remap = false)
    private void iic$onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!TriAxisConfig.enablePollutionMapOverlay) {
            return;
        }

        Minecraft mc = iic$getMinecraft();
        if (mc.level != null) {
            ResourceKey<Level> currentDim = mc.level.dimension();
            if (iic$lastDimension != null && !iic$lastDimension.equals(currentDim)) {
                PollutionOverlayAPI.clearCache();
            }
            iic$lastDimension = currentDim;
        }

        iic$renderPollution(guiGraphics, mouseX, mouseY);
    }

    @Unique
    private void iic$renderPollution(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Map<ChunkPos, Double> pollutedChunks = PollutionOverlayAPI.getAllPollutedChunks();
        if (pollutedChunks.isEmpty()) {
            return;
        }

        int screenWidth = iic$getScreenWidth();
        int screenHeight = iic$getScreenHeight();

        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();

        double cameraX = 0;
        double cameraZ = 0;
        double scale = 1.0;

        try {
            Class<?> guiMapClass = Class.forName("xaero.map.gui.GuiMap");
            java.lang.reflect.Field cameraXField = guiMapClass.getDeclaredField("cameraX");
            cameraXField.setAccessible(true);
            cameraX = cameraXField.getDouble(this);

            java.lang.reflect.Field cameraZField = guiMapClass.getDeclaredField("cameraZ");
            cameraZField.setAccessible(true);
            cameraZ = cameraZField.getDouble(this);

            java.lang.reflect.Field scaleField = guiMapClass.getDeclaredField("scale");
            scaleField.setAccessible(true);
            scale = scaleField.getDouble(this);
        } catch (Exception e) {
            return;
        }

        for (Map.Entry<ChunkPos, Double> entry : pollutedChunks.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            double pollution = entry.getValue();

            int chunkWorldX = chunkPos.x * 16;
            int chunkWorldZ = chunkPos.z * 16;

            double screenX = (chunkWorldX - cameraX) * scale + screenWidth / 2.0;
            double screenZ = (chunkWorldZ - cameraZ) * scale + screenHeight / 2.0;
            double chunkSize = 16 * scale;

            if (screenX + chunkSize < 0 || screenX > screenWidth || screenZ + chunkSize < 0 || screenZ > screenHeight) {
                continue;
            }

            float[] color = iic$getPollutionColor(pollution);
            float alpha = color[3] * (float) TriAxisConfig.pollutionOverlayAlpha;

            bufferBuilder.vertex(matrix, (float) screenX, (float) (screenZ + chunkSize), 0).color(color[0], color[1], color[2], alpha).endVertex();
            bufferBuilder.vertex(matrix, (float) (screenX + chunkSize), (float) (screenZ + chunkSize), 0).color(color[0], color[1], color[2], alpha).endVertex();
            bufferBuilder.vertex(matrix, (float) (screenX + chunkSize), (float) screenZ, 0).color(color[0], color[1], color[2], alpha).endVertex();
            bufferBuilder.vertex(matrix, (float) screenX, (float) screenZ, 0).color(color[0], color[1], color[2], alpha).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();

        if (TriAxisConfig.showPollutionTooltip) {
            double worldX = (mouseX - screenWidth / 2.0) / scale + cameraX;
            double worldZ = (mouseY - screenHeight / 2.0) / scale + cameraZ;
            ChunkPos chunkPos = new ChunkPos((int) Math.floor(worldX / 16), (int) Math.floor(worldZ / 16));
            double pollution = PollutionOverlayAPI.getPollutionForChunk(chunkPos);
            if (pollution > 0.1) {
                String tooltipText = String.format("Pollution: %.1f", pollution);
                guiGraphics.drawString(iic$getMinecraft().font, tooltipText, mouseX + 10, mouseY - 10, 0xFFFFFF);
            }
        }
    }

    @Unique
    private float[] iic$getPollutionColor(double pollution) {
        if (pollution < 50) {
            float t = (float) (pollution / 50.0);
            return new float[]{t, 1.0f, 0.0f, 1.0f};
        } else if (pollution < 100) {
            float t = (float) ((pollution - 50) / 50.0);
            return new float[]{1.0f, 1.0f - t * 0.5f, 0.0f, 1.0f};
        } else if (pollution < 200) {
            float t = (float) ((pollution - 100) / 100.0);
            return new float[]{1.0f, 0.5f - t * 0.5f, 0.0f, 1.0f};
        } else {
            return new float[]{0.8f, 0.0f, 0.0f, 1.0f};
        }
    }
}
