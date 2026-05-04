package cn.minerealms.iic.integration.gunmod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dummy class to allow compilation when Gun Mod is not present.
 * The actual mixin will target com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity at runtime.
 */
public class DummyWorkbenchBlockEntity extends BlockEntity {
    public DummyWorkbenchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
