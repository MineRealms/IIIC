package cn.minerealms.iic.mixin.gunmod;

import cn.minerealms.iic.integration.gunmod.DummyWorkbenchBlock;
import cn.minerealms.iic.integration.gunmod.IEnergyWorkbench;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/**
 * Mixin to add ticking capability to Gun Mod's WorkbenchBlock.
 * This allows the workbench to consume energy and track progress over time.
 */
@Mixin(value = DummyWorkbenchBlock.class, remap = false)
public abstract class WorkbenchBlockMixin implements EntityBlock {

    /**
     * Add ticker to the workbench block entity.
     * This is called by Minecraft to get the ticker for server-side ticking.
     */
    @Unique
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Only tick on server side
        if (level.isClientSide) {
            return null;
        }

        // Return our custom ticker
        return iic$createWorkbenchTicker();
    }

    /**
     * Create the ticker for the workbench.
     * This ticker calls the iic$tick() method on the block entity.
     */
    @Unique
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> iic$createWorkbenchTicker() {
        return (level, pos, state, blockEntity) -> {
            if (blockEntity instanceof IEnergyWorkbench energyWorkbench) {
                // Call the tick method from our mixin
                if (blockEntity instanceof WorkbenchBlockEntityMixin mixin) {
                    mixin.iic$tick();
                }
            }
        };
    }
}
