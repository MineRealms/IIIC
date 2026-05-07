package cn.minerealms.iic.turrets.client.model;

import cn.minerealms.iic.IntegratedIndustrialCraft;
import cn.minerealms.iic.turrets.common.block_entity.CIWSTurretBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * CIWS近防炮炮塔模型
 * 复用火焰炮塔的模型和动画
 */
public class CIWSTurretModel extends GeoModel<CIWSTurretBlockEntity> {

    @Override
    public ResourceLocation getModelResource(CIWSTurretBlockEntity animatable) {
        // 复用火焰炮塔的模型
        return new ResourceLocation(IntegratedIndustrialCraft.MODID, "geo/block/flamethrower.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CIWSTurretBlockEntity animatable) {
        // 使用CIWS专用的纹理（如果没有则复用火焰炮塔的）
        return new ResourceLocation(IntegratedIndustrialCraft.MODID, "textures/block/flamethrower.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CIWSTurretBlockEntity animatable) {
        // 复用火焰炮塔的动画
        return new ResourceLocation(IntegratedIndustrialCraft.MODID, "animations/block/flamethrower_anim.json");
    }
}
