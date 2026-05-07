package cn.minerealms.iic.turrets.common.block_entity;

import mekanism.api.tier.BaseTier;
import mekanism.api.tier.ITier;

import java.util.function.Supplier;

/**
 * CIWS (Close-In Weapon System) 近防炮炮塔等级
 *
 * 特性：
 * - 只有精英（ELITE）等级
 * - 极高射速（2 tick冷却 = 0.1秒 = 10发/秒）
 * - 使用Gun Mod的子弹系统
 * - 中等伤害，依靠射速压制
 * - 中等射程（48格）
 * - 高能量消耗
 */
public enum CIWSTurretTier implements ITier {
    ELITE(BaseTier.ELITE, 2, 4.0, 50000, 48.0, 100); // 2 tick冷却，4伤害，50k能量，48格射程，100 FE/发

    private final BaseTier baseTier;
    private final int cooldown;
    private final double damage;
    private final int energyCapacity;
    private final double range;
    private final int energyPerShot;

    private Integer cooldownReference;
    private Double damageReference;
    private Integer energyCapacityReference;
    private Double rangeReference;
    private Integer energyPerShotReference;

    CIWSTurretTier(BaseTier baseTier, int cooldown, double damage, int energyCapacity, double range, int energyPerShot) {
        this.baseTier = baseTier;
        this.cooldown = cooldown;
        this.damage = damage;
        this.energyCapacity = energyCapacity;
        this.range = range;
        this.energyPerShot = energyPerShot;
    }

    @Override
    public BaseTier getBaseTier() {
        return baseTier;
    }

    public int getCooldown() {
        return cooldownReference == null ? getBaseCooldown() : cooldownReference;
    }

    private int getBaseCooldown() {
        return cooldown;
    }

    public double getDamage() {
        return damageReference == null ? getBaseDamage() : damageReference;
    }

    private double getBaseDamage() {
        return damage;
    }

    public int getEnergyCapacity() {
        return energyCapacityReference == null ? getBaseEnergyCapacity() : energyCapacityReference;
    }

    private int getBaseEnergyCapacity() {
        return energyCapacity;
    }

    public double getRange() {
        return rangeReference == null ? getBaseRange() : rangeReference;
    }

    private double getBaseRange() {
        return range;
    }

    public int getEnergyPerShot() {
        return energyPerShotReference == null ? getBaseEnergyPerShot() : energyPerShotReference;
    }

    private int getBaseEnergyPerShot() {
        return energyPerShot;
    }

    public void setConfigReference(Supplier<Integer> cooldownReference, Supplier<Double> damageReference,
                                   Supplier<Integer> energyCapacityReference, Supplier<Double> rangeReference,
                                   Supplier<Integer> energyPerShotReference) {
        this.cooldownReference = cooldownReference.get();
        this.damageReference = damageReference.get();
        this.energyCapacityReference = energyCapacityReference.get();
        this.rangeReference = rangeReference.get();
        this.energyPerShotReference = energyPerShotReference.get();
    }
}
