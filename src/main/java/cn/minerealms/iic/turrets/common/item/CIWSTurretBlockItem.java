package cn.minerealms.iic.turrets.common.item;

import cn.minerealms.iic.turrets.client.model.DefaultedBlockItemGeoModel;
import cn.minerealms.iic.turrets.common.block.CIWSTurretBlock;
import cn.minerealms.iic.turrets.common.block_entity.CIWSTurretTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.item.block.machine.ItemBlockMachine;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * CIWS近防炮炮塔物品
 */
public class CIWSTurretBlockItem extends ItemBlockMachine implements GeoItem {

    private BlockEntityWithoutLevelRenderer renderer;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CIWSTurretBlockItem(CIWSTurretBlock block) {
        super(block);
    }

    @Override
    public CIWSTurretTier getTier() {
        return Attribute.getTier(getBlock(), CIWSTurretTier.class);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<GeckoBlockItem>(new DefaultedBlockItemGeoModel<>(ForgeRegistries.ITEMS.getKey(CIWSTurretBlockItem.this)));
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
