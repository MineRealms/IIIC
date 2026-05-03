package cn.minerealms.iic.mixin.xaeromap;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;

/**
 * Accessor mixin for GuiMap to access private fields without reflection.
 * <p>
 * This provides access to camera position and scale for coordinate conversion
 * in the pollution overlay renderer.
 *
 * @author ImprovedMobs Industrial Integration
 */
@Mixin(value = GuiMap.class, remap = false)
public interface GuiMapAccessor {

    /**
     * Gets the camera X position in world coordinates.
     *
     * @return Camera X position
     */
    @Accessor("cameraX")
    double iic_getCameraX();

    /**
     * Gets the camera Z position in world coordinates.
     *
     * @return Camera Z position
     */
    @Accessor("cameraZ")
    double iic_getCameraZ();

    /**
     * Gets the current map scale/zoom level.
     *
     * @return Map scale
     */
    @Accessor("scale")
    double iic_getScale();
}

/**
 * Accessor for GuiTexturedButton to update texture coordinates without reflection.
 * <p>
 * This allows us to update the button appearance without reinitializing the entire GUI.
 *
 * @author ImprovedMobs Industrial Integration
 */
@Mixin(value = GuiTexturedButton.class, remap = false)
interface GuiTexturedButtonAccessor {

    /**
     * Sets the texture X coordinate.
     *
     * @param textureX New texture X coordinate
     */
    @Accessor("textureX")
    void iic_setTextureX(int textureX);

    /**
     * Sets the texture Y coordinate.
     *
     * @param textureY New texture Y coordinate
     */
    @Accessor("textureY")
    void iic_setTextureY(int textureY);

    /**
     * Sets the texture resource location.
     *
     * @param texture New texture resource location
     */
    @Accessor("texture")
    void iic_setTexture(ResourceLocation texture);
}
