package cn.minerealms.iic.turrets.common.block_entity;

import cn.minerealms.iic.turrets.MekanismTurretsConfig;
import cn.minerealms.iic.turrets.common.entity.TurretProjectileEntity;
import cn.minerealms.iic.turrets.common.registry.EntityRegistry;
import cn.minerealms.iic.turrets.common.registry.SoundRegistry;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.network.message.S2CMessageBulletTrail;
import mekanism.api.*;
import mekanism.api.math.FloatingLong;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.security.SecurityFrequency;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.SecurityUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.UUID;

/**
 * CIWS (Close-In Weapon System) 近防炮炮塔方块实体
 *
 * 特性：
 * - 48格射程
 * - 极高射速（10发/秒）
 * - 使用Gun Mod的子弹系统发射弹幕
 * - 消耗能量和弹药
 * - 弹药槽位：只能放入，不能取出
 * - 支持目标过滤（敌对/被动/玩家）
 * - 只有精英等级
 */
public class CIWSTurretBlockEntity extends TileEntityMekanism implements GeoBlockEntity {

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;

    // CIWS不需要弹药槽，只消耗能量（像激光炮塔一样）
    // 能量底座不支持物品传输，只支持能量和流体

    // TODO: SerializableDataTicket is abstract in GeckoLib 4.8.3, need to find proper initialization method
    // For now, we'll track target state internally without network sync
    // public static SerializableDataTicket<Boolean> HAS_TARGET;
    // public static SerializableDataTicket<Double> TARGET_POS_X;
    // public static SerializableDataTicket<Double> TARGET_POS_Y;
    // public static SerializableDataTicket<Double> TARGET_POS_Z;

    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);

    private AABB targetBox;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private CIWSTurretTier tier;
    private MachineEnergyContainer<CIWSTurretBlockEntity> energyContainer;

    // 目标过滤设置
    private boolean targetsHostile = true;
    private boolean targetsPassive = false;
    private boolean targetsPlayers = false;
    private boolean targetsTrusted = true;

    // 攻击模式：false = 精准模式（单目标），true = 范围模式（密集区域）
    private boolean areaMode = false;

    private @Nullable LivingEntity target;
    public float xRot0 = 0;
    public float yRot0 = 0;
    private int coolDown = 0;
    private int idleTicks = 0;
    private int lastUpgradeCount = -1;
    private int targetValidationCounter = 0;

    public CIWSTurretBlockEntity(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    public void blockRemoved() {
        Vec3 pos = getBlockPos().getCenter();
        for (Upgrade upgrade : upgradeComponent.getInstalledTypes()) {
            level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z,
                    UpgradeUtils.getStack(upgrade, upgradeComponent.getUpgrades(upgrade))));
        }
        super.blockRemoved();
    }

    @Override
    protected @Nullable IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(this::getDirection);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    public MachineEnergyContainer<CIWSTurretBlockEntity> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    protected @Nullable IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 143, 35), RelativeSide.BACK);
        return builder.build();
    }

    public CIWSTurretTier getTier() {
        return tier;
    }

    public boolean targetsHostile() {
        return targetsHostile;
    }

    public void setTargetsHostile(boolean targetsHostile) {
        this.targetsHostile = targetsHostile;
        setChanged();
    }

    public boolean targetsPassive() {
        return targetsPassive;
    }

    public void setTargetsPassive(boolean targetsPassive) {
        this.targetsPassive = targetsPassive;
        setChanged();
    }

    public boolean targetsPlayers() {
        return targetsPlayers;
    }

    public void setTargetsPlayers(boolean targetsPlayers) {
        this.targetsPlayers = targetsPlayers;
        setChanged();
    }

    public boolean targetsTrusted() {
        return targetsTrusted;
    }

    public void setTargetsTrusted(boolean targetsTrusted) {
        this.targetsTrusted = targetsTrusted;
        setChanged();
    }

    public boolean isAreaMode() {
        return areaMode;
    }

    public void setAreaMode(boolean areaMode) {
        this.areaMode = areaMode;
        setChanged();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();

        if (tier == null) {
            tier = Attribute.getTier(getBlockType(), CIWSTurretTier.class);
        }

        if (coolDown > 0) {
            coolDown--;
        }

        // 每5 tick验证一次目标有效性
        if (++targetValidationCounter >= 5) {
            targetValidationCounter = 0;
            tryInvalidateTarget();
        }

        tryFindTarget();

        if (target != null) {
            // 发射前再次验证目标有效性
            if (!target.isAlive() || target.isRemoved()) {
                target = null;
                // setAnimData(HAS_TARGET, false);  // TODO: Re-enable when SerializableDataTicket is fixed
                return;
            }

            Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            // setAnimData(TARGET_POS_X, targetPos.x);  // TODO: Re-enable when SerializableDataTicket is fixed
            // setAnimData(TARGET_POS_Y, targetPos.y);
            // setAnimData(TARGET_POS_Z, targetPos.z);
            // setAnimData(HAS_TARGET, true);

            // 冷却完成且有足够能量时发射（不需要弹药）
            if (coolDown <= 0 && hasEnoughEnergy()) {
                shootBullet();
                consumeEnergy();
                coolDown = tier.getCooldown() / (upgradeComponent.getUpgrades(Upgrade.SPEED) + 1);
            }
        } else {
            // setAnimData(HAS_TARGET, false);  // TODO: Re-enable when SerializableDataTicket is fixed
        }
    }

    /**
     * 检查是否有足够的能量
     */
    private boolean hasEnoughEnergy() {
        int energyRequired = tier.getEnergyPerShot();
        return energyContainer.getEnergy().compareTo(FloatingLong.create(energyRequired)) >= 0;
    }

    /**
     * 消耗能量
     */
    private void consumeEnergy() {
        int energyRequired = tier.getEnergyPerShot();
        energyContainer.extract(FloatingLong.create(energyRequired), Action.EXECUTE, AutomationType.INTERNAL);
    }

    /**
     * 发射子弹（使用Gun Mod的TurretProjectileEntity）
     */
    private void shootBullet() {
        if (target == null || level == null) return;

        int mufflerCount = getComponent().getUpgrades(Upgrade.MUFFLING);
        float volume = 1.0F - (mufflerCount / (float) Upgrade.MUFFLING.getMax());
        level.playSound(null, getBlockPos(), SoundRegistry.TURRET_SHOOT.get(), SoundSource.BLOCKS, volume, 1.2F);

        triggerAnim("controller", "shoot");

        // 计算枪口位置（复用火焰炮塔的枪口位置）
        Vec3 center = getBlockPos().getCenter();
        double muzzleOffsetX = -(-1.0) / 16.0;
        double muzzleOffsetY = 27.0 / 16.0 - 0.5;
        double muzzleOffsetZ = 21.5 / 16.0;

        double cosYaw = Math.cos(yRot0);
        double sinYaw = Math.sin(yRot0);
        double rotatedX = muzzleOffsetX * cosYaw - muzzleOffsetZ * sinYaw;
        double rotatedZ = muzzleOffsetX * sinYaw + muzzleOffsetZ * cosYaw;

        Vec3 muzzlePos = center.add(rotatedX, muzzleOffsetY, rotatedZ);

        // 计算目标位置
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 direction = targetPos.subtract(muzzlePos).normalize();

        // 创建虚拟弹药物品（不需要真实弹药）
        ItemStack virtualAmmo = new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET);

        // 创建Gun Mod的炮塔子弹实体
        TurretProjectileEntity projectile = new TurretProjectileEntity(
            EntityRegistry.TURRET_PROJECTILE.get(),
            level,
            getBlockPos(),
            muzzlePos,
            direction,
            virtualAmmo
        );

        level.addFreshEntity(projectile);

        // 发送弹道轨迹包（Gun Mod的子弹轨迹渲染）
        try {
            com.mrcrayfish.guns.entity.ProjectileEntity[] projectiles = new com.mrcrayfish.guns.entity.ProjectileEntity[]{projectile};
            com.mrcrayfish.guns.common.Gun.Projectile projectileProps = projectile.getProjectile();

            if (projectileProps != null && !projectileProps.isVisible()) {
                S2CMessageBulletTrail trailMessage = new S2CMessageBulletTrail(
                    projectiles,
                    projectileProps,
                    -1, // 没有射手ID
                    null // 没有粒子效果
                );

                // 发送给附近的玩家（使用Gun Mod的网络系统）
                double radius = 128.0;
                com.mrcrayfish.framework.api.network.LevelLocation location =
                    com.mrcrayfish.framework.api.network.LevelLocation.create(
                        level,
                        muzzlePos.x,
                        muzzlePos.y,
                        muzzlePos.z,
                        radius
                    );
                PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> location, trailMessage);
            }
        } catch (Exception e) {
            // 如果Gun Mod的网络包发送失败，静默失败（子弹仍然会发射）
        }
    }

    /**
     * 创建炮塔专用的子弹实体（已废弃，使用TurretProjectileEntity）
     */
    @Deprecated
    private com.mrcrayfish.guns.entity.ProjectileEntity createTurretProjectile(Vec3 startPos, Vec3 direction, double damage) {
        return null;
    }

    private void tryFindTarget() {
        if (target != null) {
            return;
        }

        if (targetBox == null) {
            double range = tier.getRange();
            BlockPos pos = getBlockPos();
            targetBox = new AABB(pos).inflate(range);
        }

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, targetBox, this::isValidTarget);

        if (entities.isEmpty()) {
            idleTicks++;
            return;
        }

        idleTicks = 0;

        if (areaMode) {
            // 范围模式：找到最密集的区域
            target = findDensestTarget(entities);
        } else {
            // 精准模式：找到最近的目标
            target = findNearestTarget(entities);
        }
    }

    private LivingEntity findNearestTarget(List<LivingEntity> entities) {
        Vec3 turretPos = getBlockPos().getCenter();
        return entities.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(turretPos)))
                .orElse(null);
    }

    private LivingEntity findDensestTarget(List<LivingEntity> entities) {
        // 找到周围6x6范围内实体最多的目标
        LivingEntity densest = null;
        int maxCount = 0;

        for (LivingEntity entity : entities) {
            AABB area = entity.getBoundingBox().inflate(3);
            int count = (int) entities.stream()
                    .filter(e -> area.intersects(e.getBoundingBox()))
                    .count();

            if (count > maxCount) {
                maxCount = count;
                densest = entity;
            }
        }

        return densest;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        // 检查目标类型过滤
        MobCategory category = entity.getType().getCategory();
        if (category == MobCategory.MONSTER && !targetsHostile) {
            return false;
        }
        if ((category == MobCategory.CREATURE || category == MobCategory.AMBIENT) && !targetsPassive) {
            return false;
        }

        if (entity instanceof Player player) {
            if (!targetsPlayers) {
                return false;
            }
            // 检查安全频率
            UUID owner = SecurityUtils.get().getOwnerUUID(this);
            if (!player.getUUID().equals(owner)) {
                if (!targetsTrusted) {
                    SecurityFrequency frequency = getFrequency(FrequencyType.SECURITY);
                    if (frequency != null && frequency.getTrustedUUIDs().contains(player.getUUID())) {
                        return false;
                    }
                }
            } else {
                return false; // 不攻击所有者
            }
        }

        return true;
    }

    public void tryInvalidateTarget() {
        if (target == null) {
            return;
        }

        if (!isValidTarget(target)) {
            target = null;
            return;
        }

        Vec3 turretPos = getBlockPos().getCenter();
        double distSq = target.distanceToSqr(turretPos);
        double range = tier.getRange();

        if (distSq > range * range) {
            target = null;
        }
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        if(!this.level.isClientSide()) sendUpdatePacket();
    }

    public boolean hasTarget() {
        return target != null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (hasTarget()) {
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        NBTUtils.setFloatIfPresent(nbt, "xRot0", value -> xRot0 = value);
        NBTUtils.setFloatIfPresent(nbt, "yRot0", value -> yRot0 = value);
        NBTUtils.setIntIfPresent(nbt, "coolDown", value -> coolDown = value);
        NBTUtils.setBooleanIfPresent(nbt, "targetsHostile", value -> targetsHostile = value);
        NBTUtils.setBooleanIfPresent(nbt, "targetsPassive", value -> targetsPassive = value);
        NBTUtils.setBooleanIfPresent(nbt, "targetsPlayers", value -> targetsPlayers = value);
        NBTUtils.setBooleanIfPresent(nbt, "targetsTrusted", value -> targetsTrusted = value);
        NBTUtils.setBooleanIfPresent(nbt, "areaMode", value -> areaMode = value);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putFloat("xRot0", xRot0);
        nbt.putFloat("yRot0", yRot0);
        nbt.putInt("coolDown", coolDown);
        nbt.putBoolean("targetsHostile", targetsHostile);
        nbt.putBoolean("targetsPassive", targetsPassive);
        nbt.putBoolean("targetsPlayers", targetsPlayers);
        nbt.putBoolean("targetsTrusted", targetsTrusted);
        nbt.putBoolean("areaMode", areaMode);
    }

    @Override
    public void parseUpgradeData(@NotNull IUpgradeData data) {
        if (data instanceof CIWSTurretUpgradeData upgradeData) {
            getEnergyContainer().setEnergy(upgradeData.energyContainer().getEnergy());
            energySlot.setStack(upgradeData.energySlot().getStack());
            targetsHostile = upgradeData.targetsHostile();
            targetsPassive = upgradeData.targetsPassive();
            targetsPlayers = upgradeData.targetsPlayers();
            targetsTrusted = upgradeData.targetsTrusted();
            areaMode = upgradeData.areaMode();
            for (ITileComponent component : getComponents()) {
                component.read(upgradeData.components());
            }
        }
    }

    @NotNull
    @Override
    public CIWSTurretUpgradeData getUpgradeData() {
        return new CIWSTurretUpgradeData(getEnergyContainer(), energySlot,
                targetsHostile, targetsPassive, targetsPlayers, targetsTrusted, areaMode, getComponents());
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        return net.minecraft.network.chat.Component.translatable("container.integratedindustrialcraft.ciws_turret");
    }
}
