package cn.minerealms.iic.client;

import cn.minerealms.iic.api.PollutionOverlayAPI;
import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;
import cn.minerealms.iic.mixin.xaeromap.GuiMapAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * Client-side renderer for pollution overlay on XaerosWorldMap.
 * <p>
 * This renderer draws colored rectangles over polluted chunks on the world map,
 * with colors ranging from green (low pollution) to red (extreme pollution).
 *
 * @author ImprovedMobs Industrial Integration
 */
public class PollutionOverlayRenderer {

    /**
     * Renders the pollution overlay on the world map.
     * <p>
     * This method is called from {@link cn.minerealms.iic.mixin.xaeromap.GuiMapMixin}
     * during the map rendering phase.
     * <p>
     * Performance optimization: Uses LOD (Level of Detail) system to skip rendering
     * low-pollution chunks when zoomed out.
     *
     * @param guiGraphics The GUI graphics context
     * @param guiMap The GuiMap instance (passed as Object to avoid early class loading)
     */
    public static void render(GuiGraphics guiGraphics, Object guiMap) {
        Map<ChunkPos, Double> pollutedChunks = PollutionOverlayAPI.getAllPollutedChunks();
        if (pollutedChunks.isEmpty()) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // Get camera position and scale from GuiMap using accessor
        GuiMapAccessor accessor = (GuiMapAccessor) guiMap;
        double cameraX = accessor.iic_getCameraX();
        double cameraZ = accessor.iic_getCameraZ();
        double scale = accessor.iic_getScale();

        // Get screen dimensions via reflection to avoid importing GuiMap
        int screenWidth;
        int screenHeight;
        try {
            java.lang.reflect.Field widthField = guiMap.getClass().getField("width");
            java.lang.reflect.Field heightField = guiMap.getClass().getField("height");
            screenWidth = widthField.getInt(guiMap);
            screenHeight = heightField.getInt(guiMap);
        } catch (Exception e) {
            IndustrialLogger.error("Failed to get screen dimensions from GuiMap", e);
            return;
        }

        // Set up rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        int renderedChunks = 0;
        int culledByLOD = 0;
        int culledByFrustum = 0;

        // Render each polluted chunk
        for (Map.Entry<ChunkPos, Double> entry : pollutedChunks.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            double pollution = entry.getValue();

            // LOD culling: Skip low-pollution chunks when zoomed out
            if (!shouldRenderChunk(scale, pollution)) {
                culledByLOD++;
                continue;
            }

            // Convert chunk world coordinates to screen coordinates
            // 1 chunk = 16 blocks
            int chunkWorldX = chunkPos.x * 16;
            int chunkWorldZ = chunkPos.z * 16;

            double screenX = (chunkWorldX - cameraX) * scale + screenWidth / 2.0;
            double screenZ = (chunkWorldZ - cameraZ) * scale + screenHeight / 2.0;
            double chunkSize = 16 * scale; // Chunk size on screen

            // Frustum culling: Skip chunks outside screen bounds
            if (screenX + chunkSize < 0 || screenX > screenWidth ||
                screenZ + chunkSize < 0 || screenZ > screenHeight) {
                culledByFrustum++;
                continue;
            }

            // Calculate color based on pollution level
            float[] color = getPollutionColor(pollution);
            float r = color[0];
            float g = color[1];
            float b = color[2];
            float a = color[3] * (float) TriAxisConfig.pollutionOverlayAlpha;

            // Draw chunk rectangle
            bufferBuilder.vertex(matrix, (float)screenX, (float)(screenZ + chunkSize), 0)
                    .color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, (float)(screenX + chunkSize), (float)(screenZ + chunkSize), 0)
                    .color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, (float)(screenX + chunkSize), (float)screenZ, 0)
                    .color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, (float)screenX, (float)screenZ, 0)
                    .color(r, g, b, a).endVertex();

            renderedChunks++;
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();

        poseStack.popPose();

        // Debug logging (only if debug enabled and many chunks were culled)
        if (IndustrialLogger.isDebugEnabled() && (culledByLOD > 100 || culledByFrustum > 100)) {
            IndustrialLogger.debug(String.format(
                "[PollutionOverlay] Rendered: %d, LOD culled: %d, Frustum culled: %d, Scale: %.2f",
                renderedChunks, culledByLOD, culledByFrustum, scale
            ));
        }
    }

    /**
     * LOD (Level of Detail) system - determines if a chunk should be rendered based on zoom level.
     * <p>
     * Performance optimization:
     * <ul>
     *   <li>Scale < 0.2 (very zoomed out): Only show pollution >= 100 (high pollution)</li>
     *   <li>Scale < 0.5 (zoomed out): Only show pollution >= 50 (medium+ pollution)</li>
     *   <li>Scale >= 0.5 (normal/zoomed in): Show all pollution</li>
     * </ul>
     *
     * @param scale Current map scale/zoom level
     * @param pollution Pollution value
     * @return true if chunk should be rendered, false to skip
     */
    private static boolean shouldRenderChunk(double scale, double pollution) {
        if (scale < 0.2) {
            // Very zoomed out - only show high pollution
            return pollution >= 100.0;
        } else if (scale < 0.5) {
            // Zoomed out - only show medium+ pollution
            return pollution >= 50.0;
        }
        // Normal/zoomed in - show all pollution
        return true;
    }

    /**
     * Converts pollution value to RGBA color.
     * <p>
     * Color gradient:
     * <ul>
     *   <li>0-50: Green → Yellow (low pollution)</li>
     *   <li>50-100: Yellow → Orange (medium pollution)</li>
     *   <li>100-200: Orange → Red (high pollution)</li>
     *   <li>200+: Deep Red (extreme pollution)</li>
     * </ul>
     *
     * @param pollution Pollution value
     * @return RGBA color array [r, g, b, a] with values 0.0-1.0
     */
    private static float[] getPollutionColor(double pollution) {
        float alpha = 1.0f; // Base alpha (will be multiplied by config value)

        if (pollution < 50) {
            // Green → Yellow
            float t = (float)(pollution / 50.0);
            return new float[]{t, 1.0f, 0.0f, alpha};
        } else if (pollution < 100) {
            // Yellow → Orange
            float t = (float)((pollution - 50) / 50.0);
            return new float[]{1.0f, 1.0f - t * 0.5f, 0.0f, alpha};
        } else if (pollution < 200) {
            // Orange → Red
            float t = (float)((pollution - 100) / 100.0);
            return new float[]{1.0f, 0.5f - t * 0.5f, 0.0f, alpha};
        } else {
            // Deep Red
            return new float[]{0.8f, 0.0f, 0.0f, alpha};
        }
    }

    /**
     * Gets the pollution value for the chunk at the given mouse position.
     * <p>
     * This is used for hover tooltips.
     *
     * @param guiMap The GuiMap instance (passed as Object to avoid early class loading)
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @return Pollution value, or 0.0 if no pollution
     */
    public static double getPollutionAtMouse(Object guiMap, int mouseX, int mouseY) {
        GuiMapAccessor accessor = (GuiMapAccessor) guiMap;
        double cameraX = accessor.iic_getCameraX();
        double cameraZ = accessor.iic_getCameraZ();
        double scale = accessor.iic_getScale();

        // Get screen dimensions via reflection
        int screenWidth;
        int screenHeight;
        try {
            java.lang.reflect.Field widthField = guiMap.getClass().getField("width");
            java.lang.reflect.Field heightField = guiMap.getClass().getField("height");
            screenWidth = widthField.getInt(guiMap);
            screenHeight = heightField.getInt(guiMap);
        } catch (Exception e) {
            return 0.0;
        }

        // Convert mouse position to world coordinates
        double worldX = (mouseX - screenWidth / 2.0) / scale + cameraX;
        double worldZ = (mouseY - screenHeight / 2.0) / scale + cameraZ;

        // Convert to chunk position
        ChunkPos chunkPos = new ChunkPos((int)Math.floor(worldX / 16), (int)Math.floor(worldZ / 16));

        return PollutionOverlayAPI.getPollutionForChunk(chunkPos);
    }
}
