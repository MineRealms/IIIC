package cn.minerealms.iic.mixin.xaeromap;

import cn.minerealms.iic.client.PollutionOverlayRenderer;
import cn.minerealms.iic.industrial.TriAxisConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaero.map.gui.CursorBox;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;

/**
 * Mixin for XaerosWorldMap's GuiMap to add pollution overlay functionality.
 * <p>
 * This mixin adds:
 * <ul>
 *   <li>A toggle button for the pollution overlay</li>
 *   <li>Rendering of the pollution overlay when enabled</li>
 *   <li>Hover tooltip showing exact pollution values</li>
 * </ul>
 *
 * <p><b>Mixin规范遵循：</b>
 * <ul>
 *   <li>所有注入方法都是private</li>
 *   <li>所有@Unique方法都是private</li>
 *   <li>使用iic$前缀避免命名冲突</li>
 *   <li>remap=false因为XaerosWorldMap是混淆的</li>
 *   <li>不使用@Shadow访问继承字段，直接通过实例访问</li>
 * </ul>
 *
 * @author ImprovedMobs Industrial Integration
 */
@OnlyIn(Dist.CLIENT)
@Mixin(value = GuiMap.class, remap = false, priority = 1000)
public abstract class GuiMapMixin {

    /**
     * Pollution toggle button instance.
     * Instance field, not static - safe for Mixin.
     */
    @Unique
    private Button iic$pollutionButton;

    /**
     * Pollution overlay state.
     * Static field to persist across GUI reinitializations.
     * This is allowed in Mixin as it's a field, not a method.
     */
    @Unique
    private static boolean iic$showPollution = false;

    /**
     * Inject into init method to add pollution toggle button.
     * <p>
     * The button is placed on the right side of the screen, above the zoom buttons.
     *
     * <p><b>Mixin规范：</b>
     * <ul>
     *   <li>方法必须是private</li>
     *   <li>使用@Inject注入</li>
     *   <li>@At("TAIL")在方法末尾注入</li>
     *   <li>remap=false匹配类级别设置</li>
     * </ul>
     */
    @Inject(method = "m_7856_", at = @At("TAIL"), remap = false)
    private void iic$addPollutionButton(CallbackInfo ci) {
        if (!TriAxisConfig.enablePollutionMapOverlay) {
            return; // Skip if disabled in config
        }

        GuiMap self = (GuiMap) (Object) this;

        // Create tooltip based on current state
        CursorBox tooltip = new CursorBox(
                Component.translatable(
                        iic$showPollution ?
                                "gui.iic.pollution_overlay_on" :
                                "gui.iic.pollution_overlay_off"
                )
        );

        // Create button (positioned above zoom buttons at x=width-20, y=height-180)
        // Texture coordinates: 245 (on) or 229 (off) for x, 80 for y
        this.iic$pollutionButton = new GuiTexturedButton(
                self.width - 20,           // x: right side
                self.height - 180,         // y: above zoom buttons
                20, 20,                    // width, height
                iic$showPollution ? 245 : 229,  // textureX (changes based on state)
                80,                        // textureY
                16, 16,                    // texWidth, texHeight
                WorldMap.guiTextures,      // Use XaerosWorldMap's texture atlas
                this::iic$onPollutionButton,  // Click handler
                () -> tooltip              // Tooltip supplier
        );

        // Add button to GUI
        self.addButton(this.iic$pollutionButton);
    }

    /**
     * Button click handler - toggles pollution overlay.
     *
     * <p><b>Mixin规范：</b>
     * <ul>
     *   <li>方法必须是private</li>
     *   <li>使用@Unique标记</li>
     *   <li>不能是public或protected</li>
     * </ul>
     */
    @Unique
    private void iic$onPollutionButton(Button button) {
        iic$showPollution = !iic$showPollution;

        // Reinitialize GUI to update button texture
        GuiMap self = (GuiMap) (Object) this;
        self.init(self.getMinecraft(), self.width, self.height);
    }

    /**
     * Inject into render method to draw pollution overlay.
     * <p>
     * We inject at TAIL to ensure the overlay is drawn on top of the map.
     *
     * <p><b>Mixin规范：</b>
     * <ul>
     *   <li>方法必须是private</li>
     *   <li>使用@Inject注入</li>
     *   <li>@At("TAIL")在方法末尾注入</li>
     *   <li>remap=false匹配类级别设置</li>
     * </ul>
     */
    @Inject(method = "m_88315_", at = @At("TAIL"), remap = false)
    private void iic$renderPollutionOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!iic$showPollution || !TriAxisConfig.enablePollutionMapOverlay) {
            return;
        }

        GuiMap self = (GuiMap) (Object) this;

        // Render pollution overlay
        PollutionOverlayRenderer.render(guiGraphics, self);

        // Render hover tooltip with exact pollution value
        if (TriAxisConfig.showPollutionTooltip) {
            double pollution = PollutionOverlayRenderer.getPollutionAtMouse(self, mouseX, mouseY);
            if (pollution > 0.1) {
                String tooltipText = String.format("§6Pollution: §c%.1f", pollution);
                guiGraphics.drawString(
                        self.getMinecraft().font,
                        tooltipText,
                        mouseX + 10,
                        mouseY - 10,
                        0xFFFFFF
                );
            }
        }
    }
}
