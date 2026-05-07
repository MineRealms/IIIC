package cn.minerealms.iic.turrets.common.entity;

import com.mrcrayfish.guns.common.Gun;
import com.mrcrayfish.guns.common.GripType;
import com.mrcrayfish.guns.entity.ProjectileEntity;
import com.mrcrayfish.guns.item.GunItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 炮塔专用的子弹实体
 *
 * 继承自Gun Mod的ProjectileEntity，但不需要LivingEntity射手
 * 用于CIWS炮塔发射子弹
 */
public class TurretProjectileEntity extends ProjectileEntity {

    private BlockPos turretPos;

    /**
     * 标准构造函数（用于反序列化）
     */
    public TurretProjectileEntity(EntityType<? extends ProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 炮塔专用构造函数
     *
     * @param entityType 实体类型
     * @param level 世界
     * @param turretPos 炮塔位置（用于伤害来源）
     * @param startPos 子弹起始位置
     * @param direction 发射方向（已归一化）
     * @param ammoItem 弹药物品
     */
    public TurretProjectileEntity(EntityType<? extends ProjectileEntity> entityType, Level level,
                                  BlockPos turretPos, Vec3 startPos, Vec3 direction, ItemStack ammoItem) {
        super(entityType, level);

        this.turretPos = turretPos;
        this.shooterId = -1; // 没有射手实体
        this.shooter = null;

        // 创建虚拟的Gun配置（基于弹药类型）
        this.modifiedGun = createTurretGun(ammoItem);
        this.general = modifiedGun.getGeneral();
        this.projectile = modifiedGun.getProjectile();

        // 设置实体大小
        this.entitySize = new net.minecraft.world.entity.EntityDimensions(
            this.projectile.getSize(),
            this.projectile.getSize(),
            false
        );

        // 设置重力（炮塔子弹通常不受重力影响）
        this.modifiedGravity = 0.0;

        // 设置生命周期
        this.life = this.projectile.getLife();

        // 设置速度和方向
        double speed = this.projectile.getSpeed();
        this.setDeltaMovement(direction.x * speed, direction.y * speed, direction.z * speed);
        this.updateHeading();

        // 设置起始位置
        this.setPos(startPos.x, startPos.y, startPos.z);

        // 设置弹药物品（用于渲染）
        this.setItem(ammoItem);

        // 设置武器为空（炮塔没有武器物品）
        this.setWeapon(ItemStack.EMPTY);
    }

    /**
     * 创建炮塔专用的Gun配置
     */
    private Gun createTurretGun(ItemStack ammoItem) {
        Gun gun = new Gun();

        // 通用配置 - 直接设置字段
        Gun.General general = new Gun.General();
        try {
            java.lang.reflect.Field autoField = Gun.General.class.getDeclaredField("auto");
            autoField.setAccessible(true);
            autoField.set(general, true);

            java.lang.reflect.Field rateField = Gun.General.class.getDeclaredField("rate");
            rateField.setAccessible(true);
            rateField.set(general, 2);

            java.lang.reflect.Field gripTypeField = Gun.General.class.getDeclaredField("gripType");
            gripTypeField.setAccessible(true);
            gripTypeField.set(general, GripType.TWO_HANDED);

            java.lang.reflect.Field maxAmmoField = Gun.General.class.getDeclaredField("maxAmmo");
            maxAmmoField.setAccessible(true);
            maxAmmoField.set(general, 1000);

            java.lang.reflect.Field reloadAmountField = Gun.General.class.getDeclaredField("reloadAmount");
            reloadAmountField.setAccessible(true);
            reloadAmountField.set(general, 1);

            java.lang.reflect.Field alwaysSpreadField = Gun.General.class.getDeclaredField("alwaysSpread");
            alwaysSpreadField.setAccessible(true);
            alwaysSpreadField.set(general, false);

            java.lang.reflect.Field spreadField = Gun.General.class.getDeclaredField("spread");
            spreadField.setAccessible(true);
            spreadField.set(general, 0.0F);

            java.lang.reflect.Field projectileAmountField = Gun.General.class.getDeclaredField("projectileAmount");
            projectileAmountField.setAccessible(true);
            projectileAmountField.set(general, 1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Gun.General", e);
        }

        // 子弹配置 - 直接设置字段
        Gun.Projectile projectile = new Gun.Projectile();
        try {
            java.lang.reflect.Field itemField = Gun.Projectile.class.getDeclaredField("item");
            itemField.setAccessible(true);
            itemField.set(projectile, ForgeRegistries.ITEMS.getKey(ammoItem.getItem()));

            java.lang.reflect.Field visibleField = Gun.Projectile.class.getDeclaredField("visible");
            visibleField.setAccessible(true);
            visibleField.set(projectile, false);

            java.lang.reflect.Field damageField = Gun.Projectile.class.getDeclaredField("damage");
            damageField.setAccessible(true);
            damageField.set(projectile, 4.0F);

            java.lang.reflect.Field sizeField = Gun.Projectile.class.getDeclaredField("size");
            sizeField.setAccessible(true);
            sizeField.set(projectile, 0.25F);

            java.lang.reflect.Field speedField = Gun.Projectile.class.getDeclaredField("speed");
            speedField.setAccessible(true);
            speedField.set(projectile, 3.0);

            java.lang.reflect.Field lifeField = Gun.Projectile.class.getDeclaredField("life");
            lifeField.setAccessible(true);
            lifeField.set(projectile, 100);

            java.lang.reflect.Field gravityField = Gun.Projectile.class.getDeclaredField("gravity");
            gravityField.setAccessible(true);
            gravityField.set(projectile, false);

            java.lang.reflect.Field damageReduceField = Gun.Projectile.class.getDeclaredField("damageReduceOverLife");
            damageReduceField.setAccessible(true);
            damageReduceField.set(projectile, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Gun.Projectile", e);
        }

        // 音效配置
        Gun.Sounds sounds = new Gun.Sounds();

        // 组装Gun对象
        try {
            java.lang.reflect.Field generalField = Gun.class.getDeclaredField("general");
            generalField.setAccessible(true);
            generalField.set(gun, general);

            java.lang.reflect.Field projectileField = Gun.class.getDeclaredField("projectile");
            projectileField.setAccessible(true);
            projectileField.set(gun, projectile);

            java.lang.reflect.Field soundsField = Gun.class.getDeclaredField("sounds");
            soundsField.setAccessible(true);
            soundsField.set(gun, sounds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create turret gun configuration", e);
        }

        return gun;
    }

    /**
     * 获取子弹配置（用于弹道轨迹渲染）
     */
    public Gun.Projectile getProjectile() {
        return this.projectile;
    }

    /**
     * 获取炮塔位置（用于伤害来源）
     */
    public BlockPos getTurretPos() {
        return turretPos;
    }

    /**
     * 更新heading（方向向量）
     * 注意：ProjectileEntity的updateHeading是public的
     */
    public void updateHeading() {
        Vec3 motion = this.getDeltaMovement();
        double horizontalDistance = motion.horizontalDistance();
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Math.atan2(motion.y, horizontalDistance) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
}
