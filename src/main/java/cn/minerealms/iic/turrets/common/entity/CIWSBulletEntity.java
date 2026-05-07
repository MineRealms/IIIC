package cn.minerealms.iic.turrets.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * CIWS炮塔子弹实体
 *
 * 简化的子弹实体，用于CIWS炮塔发射
 * 特性：
 * - 高速飞行
 * - 命中即消失
 * - 造成伤害
 * - 不受重力影响
 */
public class CIWSBulletEntity extends Projectile {

    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(CIWSBulletEntity.class, EntityDataSerializers.FLOAT);

    private int life = 100; // 5秒生命周期（100 ticks）
    private Vec3 initialVelocity;

    public CIWSBulletEntity(EntityType<? extends CIWSBulletEntity> type, Level level) {
        super(type, level);
    }

    public CIWSBulletEntity(EntityType<? extends CIWSBulletEntity> type, Level level, Vec3 pos, Vec3 direction, float damage) {
        this(type, level);
        this.setPos(pos.x, pos.y, pos.z);
        this.setDamage(damage);

        // 设置高速飞行（3.0 blocks/tick = 60 blocks/second）
        double speed = 3.0;
        this.initialVelocity = direction.normalize().scale(speed);
        this.setDeltaMovement(initialVelocity);

        // 设置旋转以匹配方向
        this.setYRot((float) (Math.atan2(direction.x, direction.z) * 180.0 / Math.PI));
        this.setXRot((float) (Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * 180.0 / Math.PI));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE, 4.0F);
    }

    public void setDamage(float damage) {
        this.entityData.set(DAMAGE, damage);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // 客户端：添加粒子效果
            return;
        }

        // 生命周期检查
        if (--life <= 0) {
            this.discard();
            return;
        }

        // 保持速度不变（不受重力影响）
        if (initialVelocity != null) {
            this.setDeltaMovement(initialVelocity);
        }

        // 检测碰撞
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        // 更新位置
        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);

        Entity entity = result.getEntity();
        if (entity instanceof LivingEntity target) {
            // 造成伤害
            DamageSource damageSource = this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity living ? living : null);
            target.hurt(damageSource, getDamage());
        }

        // 命中后消失
        this.discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);

        // 命中方块后消失
        this.discard();
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        // 不能击中自己的所有者
        if (entity == this.getOwner()) {
            return false;
        }

        // 只能击中生物实体
        return entity instanceof LivingEntity && super.canHitEntity(entity);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.setDamage(tag.getFloat("Damage"));
        if (tag.contains("VelocityX")) {
            this.initialVelocity = new Vec3(
                tag.getDouble("VelocityX"),
                tag.getDouble("VelocityY"),
                tag.getDouble("VelocityZ")
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putFloat("Damage", getDamage());
        if (initialVelocity != null) {
            tag.putDouble("VelocityX", initialVelocity.x);
            tag.putDouble("VelocityY", initialVelocity.y);
            tag.putDouble("VelocityZ", initialVelocity.z);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
