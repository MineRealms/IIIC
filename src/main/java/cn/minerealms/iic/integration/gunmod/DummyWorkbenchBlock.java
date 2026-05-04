package cn.minerealms.iic.integration.gunmod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Dummy class to allow compilation when Gun Mod is not present.
 * The actual mixin will target com.mrcrayfish.guns.block.WorkbenchBlock at runtime.
 */
public class DummyWorkbenchBlock extends Block implements EntityBlock {
    public DummyWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}
