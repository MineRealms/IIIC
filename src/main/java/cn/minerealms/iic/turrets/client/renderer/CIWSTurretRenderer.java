package cn.minerealms.iic.turrets.client.renderer;

import cn.minerealms.iic.turrets.client.model.CIWSTurretModel;
import cn.minerealms.iic.turrets.common.block_entity.CIWSTurretBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * CIWS近防炮炮塔渲染器
 * 复用火焰炮塔的模型和动画
 */
public class CIWSTurretRenderer extends GeoBlockRenderer<CIWSTurretBlockEntity> {

    public CIWSTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(new CIWSTurretModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case UP -> {
                poseStack.translate(0, 1, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case DOWN -> {
                // 默认朝向，不需要旋转
            }
            default -> {
                // 只支持上下放置
            }
        }
    }

    @Override
    public void render(@NotNull CIWSTurretBlockEntity animatable, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // TODO: Implement target tracking rotation when SerializableDataTicket is fixed
        // For now, just render the model without dynamic rotation
        /*
        if (animatable.hasTarget()) {
            Vec3 turretPos = animatable.getBlockPos().getCenter();
            double targetX = animatable.getAnimData(CIWSTurretBlockEntity.TARGET_POS_X);
            double targetY = animatable.getAnimData(CIWSTurretBlockEntity.TARGET_POS_Y);
            double targetZ = animatable.getAnimData(CIWSTurretBlockEntity.TARGET_POS_Z);

            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
            Vec3 direction = targetPos.subtract(turretPos).normalize();

            // 计算yaw和pitch
            float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
            float pitch = (float) Math.toDegrees(Math.asin(-direction.y));

            // 平滑插值
            animatable.yRot0 = yaw * 0.1f + animatable.yRot0 * 0.9f;
            animatable.xRot0 = pitch * 0.1f + animatable.xRot0 * 0.9f;
        }
        */

        // 调用父类渲染（GeoBlockRenderer会处理所有渲染逻辑）
        this.defaultRender(poseStack, animatable, bufferSource, null, null, 0, partialTick, packedLight);
    }
}
