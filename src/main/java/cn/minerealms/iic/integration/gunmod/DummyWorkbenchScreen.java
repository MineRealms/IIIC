package cn.minerealms.iic.integration.gunmod;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Dummy class to allow compilation when Gun Mod is not present.
 * The actual mixin will target com.mrcrayfish.guns.client.screen.WorkbenchScreen at runtime.
 */
public class DummyWorkbenchScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private Object workbench;

    public DummyWorkbenchScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Dummy implementation
    }
}
