package cn.minerealms.iic.mixin.xaeromap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;

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
